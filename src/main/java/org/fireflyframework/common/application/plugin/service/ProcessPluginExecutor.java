/*
 * Copyright 2024-2026 Firefly Software Solutions Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fireflyframework.application.plugin.service;

import org.fireflyframework.application.context.ApplicationExecutionContext;
import org.fireflyframework.application.plugin.ProcessExecutionContext;
import org.fireflyframework.application.plugin.ProcessMapping;
import org.fireflyframework.application.plugin.ProcessMetadata;
import org.fireflyframework.application.plugin.ProcessPlugin;
import org.fireflyframework.application.plugin.ProcessPluginRegistry;
import org.fireflyframework.application.plugin.ProcessResult;
import org.fireflyframework.application.plugin.ValidationResult;
import org.fireflyframework.application.plugin.config.PluginProperties;
import org.fireflyframework.application.plugin.event.PluginEventPublisher;
import org.fireflyframework.application.plugin.exception.ProcessNotFoundException;
import org.fireflyframework.application.plugin.metrics.PluginMetricsService;
import org.fireflyframework.application.security.SecurityAuthorizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Core orchestration service for executing process plugins.
 * 
 * <p>This service is the main entry point for executing business processes
 * through the plugin architecture. It handles:</p>
 * <ul>
 *   <li>Process resolution via {@code ProcessMappingService}</li>
 *   <li>Plugin lookup from {@code ProcessPluginRegistry}</li>
 *   <li>Permission validation against the process metadata</li>
 *   <li>Input validation before execution</li>
 *   <li>Process execution with proper context</li>
 *   <li>Timeout handling</li>
 *   <li>Error handling and result wrapping</li>
 * </ul>
 * 
 * <h3>Usage Example</h3>
 * <pre>
 * &#64;Service
 * public class AccountApplicationService extends AbstractApplicationService {
 *     
 *     private final ProcessPluginExecutor processExecutor;
 *     
 *     public Mono&lt;AccountResponse&gt; createAccount(
 *             ApplicationExecutionContext context,
 *             AccountCreationRequest request) {
 *         
 *         return processExecutor.executeProcess(context, "createAccount", toMap(request))
 *             .map(result -> result.getOutput(AccountResponse.class));
 *     }
 * }
 * </pre>
 * 
 * <p><b>Note:</b> This service is called from application services (NOT controllers).
 * The controller layer remains unchanged and continues to use {@code AbstractApplicationController}
 * with the existing {@code @Secure} annotation for authorization.</p>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 * @see ProcessPlugin
 * @see ProcessMappingService
 * @see ProcessPluginRegistry
 */
@Slf4j
@Service
public class ProcessPluginExecutor {
    
    private final ProcessPluginRegistry registry;
    private final ProcessMappingService mappingService;
    private final SecurityAuthorizationService authorizationService;
    private final PluginProperties properties;
    private final PluginEventPublisher eventPublisher;
    private final PluginMetricsService metricsService;
    
    public ProcessPluginExecutor(
            ProcessPluginRegistry registry,
            ProcessMappingService mappingService,
            SecurityAuthorizationService authorizationService,
            PluginProperties properties,
            PluginEventPublisher eventPublisher,
            PluginMetricsService metricsService) {
        this.registry = registry;
        this.mappingService = mappingService;
        this.authorizationService = authorizationService;
        this.properties = properties;
        this.eventPublisher = eventPublisher;
        this.metricsService = metricsService;
    }
    
    /**
     * Executes a process for the given operation.
     * 
     * <p>This method:</p>
     * <ol>
     *   <li>Resolves the process mapping based on tenant/product configuration</li>
     *   <li>Looks up the process plugin from the registry</li>
     *   <li>Validates process-level permissions</li>
     *   <li>Validates inputs (if plugin provides validation)</li>
     *   <li>Executes the process with the full context</li>
     * </ol>
     * 
     * @param context the ApplicationExecutionContext (already resolved by controller)
     * @param operationId the API operation being served (e.g., "createAccount")
     * @param input the request payload as a map
     * @return Mono emitting the process result
     */
    public Mono<ProcessResult> executeProcess(
            ApplicationExecutionContext context,
            String operationId,
            Map<String, Object> input) {
        
        return executeProcess(context, operationId, input, null);
    }
    
    /**
     * Executes a process with channel type specification.
     * 
     * @param context the ApplicationExecutionContext
     * @param operationId the API operation being served
     * @param input the request payload
     * @param channelType the channel type (e.g., "MOBILE", "WEB")
     * @return Mono emitting the process result
     */
    public Mono<ProcessResult> executeProcess(
            ApplicationExecutionContext context,
            String operationId,
            Map<String, Object> input,
            String channelType) {
        
        long startTime = System.currentTimeMillis();
        String executionId = UUID.randomUUID().toString();
        
        log.debug("Executing process for operation: {} (tenant: {}, product: {}, channel: {})",
                operationId, context.getTenantId(), context.getProductId(), channelType);
        
        return mappingService.resolveMapping(
                    context.getTenantId(),
                    operationId,
                    context.getProductId(),
                    channelType
                )
                .switchIfEmpty(Mono.error(new ProcessNotFoundException(operationId,
                        "No process mapping found for operation: " + operationId, true)))
                .flatMap(mapping -> executeWithMapping(context, mapping, input, executionId, startTime))
                .timeout(getTimeout())
                .onErrorResume(error -> handleError(error, operationId, executionId, startTime));
    }
    
    /**
     * Executes a specific process by ID, bypassing the mapping service.
     * 
     * <p>Use this method when you know the exact process ID to execute,
     * such as during testing or for administrative operations.</p>
     * 
     * @param context the ApplicationExecutionContext
     * @param processId the process ID to execute
     * @param input the request payload
     * @return Mono emitting the process result
     */
    public Mono<ProcessResult> executeProcessById(
            ApplicationExecutionContext context,
            String processId,
            Map<String, Object> input) {
        
        long startTime = System.currentTimeMillis();
        String executionId = UUID.randomUUID().toString();
        
        log.debug("Executing process by ID: {} (tenant: {})", processId, context.getTenantId());
        
        ProcessMapping mapping = ProcessMapping.builder()
                .processId(processId)
                .operationId(processId)
                .build();
        
        return executeWithMapping(context, mapping, input, executionId, startTime)
                .timeout(getTimeout())
                .onErrorResume(error -> handleError(error, processId, executionId, startTime));
    }
    
    /**
     * Executes a process with the resolved mapping.
     */
    private Mono<ProcessResult> executeWithMapping(
            ApplicationExecutionContext context,
            ProcessMapping mapping,
            Map<String, Object> input,
            String executionId,
            long startTime) {
        
        String processId = mapping.getProcessId();
        String version = mapping.getProcessVersion();
        
        // Get the plugin from registry
        ProcessPlugin plugin = registry.get(processId, version)
                .orElseThrow(() -> new ProcessNotFoundException(processId, version));
        
        ProcessMetadata metadata = plugin.getMetadata();
        
        // Log deprecated process warning
        if (metadata != null && metadata.isDeprecated()) {
            log.warn("Process {} is deprecated. Consider using: {}",
                    processId, metadata.getReplacedBy());
        }
        
        // Publish execution started event and record metrics
        publishExecutionStarted(processId, plugin.getVersion(), executionId, 
                mapping.getOperationId(), context.getTenantId());
        
        // Validate permissions
        return validateProcessPermissions(context, metadata)
                .then(Mono.defer(() -> {
                    // Build execution context
                    ProcessExecutionContext processContext = ProcessExecutionContext.builder()
                            .appContext(context)
                            .processId(processId)
                            .operationId(mapping.getOperationId())
                            .processMapping(mapping)
                            .inputs(input != null ? input : Map.of())
                            .executionId(executionId)
                            .startTime(startTime)
                            .correlationId(extractCorrelationId(context))
                            .build();
                    
                    // Validate inputs
                    return plugin.validate(processContext)
                            .flatMap(validationResult -> {
                                if (!validationResult.isValid()) {
                                    return Mono.just(ProcessResult.builder()
                                            .status(ProcessResult.Status.BUSINESS_ERROR)
                                            .errorCode("VALIDATION_FAILED")
                                            .errorMessage("Input validation failed")
                                            .metadata("validationErrors", validationResult.getErrors())
                                            .build());
                                }
                                
                                // Execute the process
                                log.debug("Executing plugin: {} v{}", processId, plugin.getVersion());
                                return plugin.execute(processContext);
                            });
                }))
                .map(result -> result.withTiming(startTime, executionId))
                .doOnSuccess(result -> {
                    long duration = System.currentTimeMillis() - startTime;
                    if (result.isSuccess()) {
                        log.debug("Process {} completed successfully in {}ms", processId, duration);
                        publishExecutionCompleted(processId, plugin.getVersion(), executionId,
                                mapping.getOperationId(), context.getTenantId(), duration, result.getStatus());
                    } else {
                        log.warn("Process {} completed with status {} in {}ms: {}",
                                processId, result.getStatus(), duration, result.getErrorMessage());
                        publishExecutionCompleted(processId, plugin.getVersion(), executionId,
                                mapping.getOperationId(), context.getTenantId(), duration, result.getStatus());
                    }
                });
    }
    
    /**
     * Validates that the context has the required permissions for the process.
     */
    private Mono<Void> validateProcessPermissions(
            ApplicationExecutionContext context,
            ProcessMetadata metadata) {
        
        if (metadata == null) {
            return Mono.empty();
        }
        
        // Check required permissions
        if (metadata.hasRequiredPermissions()) {
            return authorizationService.hasAllPermissions(
                    context.getContext(),
                    metadata.getRequiredPermissions()
            ).flatMap(hasAll -> {
                if (!hasAll) {
                    return Mono.error(new AccessDeniedException(
                            "Missing required process permissions: " + metadata.getRequiredPermissions()));
                }
                return Mono.empty();
            });
        }
        
        // Check required roles (alternative to permissions)
        if (metadata.hasRequiredRoles()) {
            return authorizationService.hasAnyRole(
                    context.getContext(),
                    metadata.getRequiredRoles()
            ).flatMap(hasAny -> {
                if (!hasAny) {
                    return Mono.error(new AccessDeniedException(
                            "Missing required process roles: " + metadata.getRequiredRoles()));
                }
                return Mono.empty();
            });
        }
        
        // Check required features
        if (metadata.getRequiredFeatures() != null && !metadata.getRequiredFeatures().isEmpty()) {
            for (String feature : metadata.getRequiredFeatures()) {
                if (!context.isFeatureEnabled(feature)) {
                    return Mono.error(new IllegalStateException(
                            "Required feature not enabled: " + feature));
                }
            }
        }
        
        return Mono.empty();
    }
    
    /**
     * Extracts the correlation ID from the context.
     */
    private String extractCorrelationId(ApplicationExecutionContext context) {
        // Try to get from security context or generate new
        if (context.getSecurityContext() != null) {
            // Could extract from security context if available
        }
        return UUID.randomUUID().toString();
    }
    
    /**
     * Gets the configured timeout.
     */
    private Duration getTimeout() {
        return properties.getSecurity().getMaxExecutionTime();
    }
    
    /**
     * Handles errors during process execution.
     */
    private Mono<ProcessResult> handleError(
            Throwable error,
            String processId,
            String executionId,
            long startTime) {
        
        long duration = System.currentTimeMillis() - startTime;
        String errorCode;
        ProcessResult.Status status;
        
        if (error instanceof ProcessNotFoundException) {
            log.error("Process not found: {} ({}ms)", processId, duration);
            errorCode = "PROCESS_NOT_FOUND";
            status = ProcessResult.Status.TECHNICAL_ERROR;
        } else if (error instanceof AccessDeniedException) {
            log.warn("Access denied for process: {} ({}ms) - {}", processId, duration, error.getMessage());
            errorCode = "ACCESS_DENIED";
            status = ProcessResult.Status.BUSINESS_ERROR;
        } else if (error instanceof java.util.concurrent.TimeoutException) {
            log.error("Process timeout: {} ({}ms)", processId, duration);
            errorCode = "PROCESS_TIMEOUT";
            status = ProcessResult.Status.TECHNICAL_ERROR;
        } else {
            log.error("Process execution failed: {} ({}ms)", processId, duration, error);
            errorCode = error.getClass().getSimpleName();
            status = ProcessResult.Status.TECHNICAL_ERROR;
        }
        
        // Publish failure event and record metrics
        publishExecutionFailed(processId, null, executionId, processId, null, 
                duration, errorCode, error.getMessage(), error);
        
        return Mono.just(ProcessResult.builder()
                .status(status)
                .errorCode(errorCode)
                .errorMessage(error.getMessage())
                .exception(status == ProcessResult.Status.TECHNICAL_ERROR ? error : null)
                .executionId(executionId)
                .executionTimeMs(duration)
                .build());
    }
    
    // ==================== Event and Metrics Publishing ====================
    
    private void publishExecutionStarted(String processId, String version, String executionId,
                                         String operationId, UUID tenantId) {
        if (eventPublisher != null) {
            eventPublisher.publishExecutionStarted(processId, version, executionId, operationId, tenantId);
        }
        if (metricsService != null) {
            metricsService.recordExecutionStart(processId, executionId);
        }
    }
    
    private void publishExecutionCompleted(String processId, String version, String executionId,
                                           String operationId, UUID tenantId, long durationMs,
                                           ProcessResult.Status status) {
        if (eventPublisher != null) {
            eventPublisher.publishExecutionCompleted(processId, version, executionId, 
                    operationId, tenantId, durationMs, status);
        }
        if (metricsService != null) {
            metricsService.recordExecutionComplete(processId, executionId, durationMs, status);
        }
    }
    
    private void publishExecutionFailed(String processId, String version, String executionId,
                                        String operationId, UUID tenantId, long durationMs,
                                        String errorCode, String errorMessage, Throwable exception) {
        if (eventPublisher != null) {
            eventPublisher.publishExecutionFailed(processId, version, executionId, 
                    operationId, tenantId, durationMs, errorCode, errorMessage, exception);
        }
        if (metricsService != null) {
            metricsService.recordExecutionComplete(processId, executionId, durationMs, 
                    ProcessResult.Status.TECHNICAL_ERROR);
            metricsService.recordError(processId, errorCode, 
                    exception != null ? exception.getClass().getName() : null);
        }
    }
}
