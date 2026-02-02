/*
 * Copyright 2025 Firefly Software Solutions Inc
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

package com.firefly.common.application.plugin.event;

import com.firefly.common.application.plugin.ProcessMetadata;
import com.firefly.common.application.plugin.ProcessPlugin;
import com.firefly.common.application.plugin.ProcessResult;
import com.firefly.common.application.plugin.config.PluginProperties;
import com.firefly.common.application.plugin.event.PluginEvent.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Service for publishing plugin lifecycle events.
 * 
 * <p>This service publishes Spring application events for plugin lifecycle
 * operations. Events are only published if event publishing is enabled
 * in the configuration.</p>
 * 
 * <h3>Configuration</h3>
 * <pre>
 * firefly:
 *   application:
 *     plugin:
 *       events:
 *         enabled: true
 *         publish-execution-events: true
 * </pre>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PluginEventPublisher {
    
    private final ApplicationEventPublisher eventPublisher;
    private final PluginProperties properties;
    
    /**
     * Publishes an event when a plugin is registered.
     * 
     * @param plugin the registered plugin
     * @param loaderType the type of loader that loaded the plugin
     */
    public void publishPluginRegistered(ProcessPlugin plugin, String loaderType) {
        if (!isEventsEnabled()) {
            return;
        }
        
        try {
            PluginRegisteredEvent event = new PluginRegisteredEvent(
                    this,
                    plugin.getProcessId(),
                    plugin.getVersion(),
                    plugin.getMetadata(),
                    loaderType
            );
            eventPublisher.publishEvent(event);
            log.debug("Published PluginRegisteredEvent for: {} v{}", 
                    plugin.getProcessId(), plugin.getVersion());
        } catch (Exception e) {
            log.warn("Failed to publish PluginRegisteredEvent: {}", e.getMessage());
        }
    }
    
    /**
     * Publishes an event when a plugin is unregistered.
     * 
     * @param processId the process ID
     * @param version the version
     * @param reason optional reason for unregistration
     */
    public void publishPluginUnregistered(String processId, String version, String reason) {
        if (!isEventsEnabled()) {
            return;
        }
        
        try {
            PluginUnregisteredEvent event = new PluginUnregisteredEvent(
                    this,
                    processId,
                    version,
                    reason
            );
            eventPublisher.publishEvent(event);
            log.debug("Published PluginUnregisteredEvent for: {} v{}", processId, version);
        } catch (Exception e) {
            log.warn("Failed to publish PluginUnregisteredEvent: {}", e.getMessage());
        }
    }
    
    /**
     * Publishes an event when plugin execution starts.
     * 
     * @param processId the process ID
     * @param processVersion the process version
     * @param executionId the execution ID
     * @param operationId the operation ID
     * @param tenantId the tenant ID
     */
    public void publishExecutionStarted(String processId, String processVersion,
                                        String executionId, String operationId, UUID tenantId) {
        if (!isExecutionEventsEnabled()) {
            return;
        }
        
        try {
            PluginExecutionStartedEvent event = new PluginExecutionStartedEvent(
                    this,
                    processId,
                    processVersion,
                    executionId,
                    operationId,
                    tenantId
            );
            eventPublisher.publishEvent(event);
            log.trace("Published PluginExecutionStartedEvent for: {} [{}]", processId, executionId);
        } catch (Exception e) {
            log.warn("Failed to publish PluginExecutionStartedEvent: {}", e.getMessage());
        }
    }
    
    /**
     * Publishes an event when plugin execution completes.
     * 
     * @param processId the process ID
     * @param processVersion the process version
     * @param executionId the execution ID
     * @param operationId the operation ID
     * @param tenantId the tenant ID
     * @param durationMs execution duration in milliseconds
     * @param status the result status
     */
    public void publishExecutionCompleted(String processId, String processVersion,
                                          String executionId, String operationId, UUID tenantId,
                                          long durationMs, ProcessResult.Status status) {
        if (!isExecutionEventsEnabled()) {
            return;
        }
        
        try {
            PluginExecutionCompletedEvent event = new PluginExecutionCompletedEvent(
                    this,
                    processId,
                    processVersion,
                    executionId,
                    operationId,
                    tenantId,
                    durationMs,
                    status
            );
            eventPublisher.publishEvent(event);
            log.trace("Published PluginExecutionCompletedEvent for: {} [{}] in {}ms", 
                    processId, executionId, durationMs);
        } catch (Exception e) {
            log.warn("Failed to publish PluginExecutionCompletedEvent: {}", e.getMessage());
        }
    }
    
    /**
     * Publishes an event when plugin execution fails.
     * 
     * @param processId the process ID
     * @param processVersion the process version
     * @param executionId the execution ID
     * @param operationId the operation ID
     * @param tenantId the tenant ID
     * @param durationMs execution duration in milliseconds
     * @param errorCode the error code
     * @param errorMessage the error message
     * @param exception the exception that caused the failure
     */
    public void publishExecutionFailed(String processId, String processVersion,
                                       String executionId, String operationId, UUID tenantId,
                                       long durationMs, String errorCode, String errorMessage,
                                       Throwable exception) {
        if (!isExecutionEventsEnabled()) {
            return;
        }
        
        try {
            PluginExecutionFailedEvent event = new PluginExecutionFailedEvent(
                    this,
                    processId,
                    processVersion,
                    executionId,
                    operationId,
                    tenantId,
                    durationMs,
                    errorCode,
                    errorMessage,
                    exception
            );
            eventPublisher.publishEvent(event);
            log.trace("Published PluginExecutionFailedEvent for: {} [{}] error: {}", 
                    processId, executionId, errorCode);
        } catch (Exception e) {
            log.warn("Failed to publish PluginExecutionFailedEvent: {}", e.getMessage());
        }
    }
    
    /**
     * Publishes an event when a plugin health check fails.
     * 
     * @param processId the process ID
     * @param processVersion the process version
     * @param healthMessage the health check failure message
     */
    public void publishHealthCheckFailed(String processId, String processVersion, String healthMessage) {
        if (!isEventsEnabled()) {
            return;
        }
        
        try {
            PluginHealthCheckFailedEvent event = new PluginHealthCheckFailedEvent(
                    this,
                    processId,
                    processVersion,
                    healthMessage
            );
            eventPublisher.publishEvent(event);
            log.debug("Published PluginHealthCheckFailedEvent for: {} - {}", processId, healthMessage);
        } catch (Exception e) {
            log.warn("Failed to publish PluginHealthCheckFailedEvent: {}", e.getMessage());
        }
    }
    
    /**
     * Publishes an event when plugin system initialization completes.
     * 
     * @param totalPlugins total number of unique plugins
     * @param totalVersions total number of plugin versions
     * @param initializationTimeMs initialization time in milliseconds
     */
    public void publishSystemInitialized(int totalPlugins, int totalVersions, long initializationTimeMs) {
        if (!isEventsEnabled()) {
            return;
        }
        
        try {
            PluginSystemInitializedEvent event = new PluginSystemInitializedEvent(
                    this,
                    totalPlugins,
                    totalVersions,
                    initializationTimeMs
            );
            eventPublisher.publishEvent(event);
            log.info("Published PluginSystemInitializedEvent: {} plugins, {} versions in {}ms",
                    totalPlugins, totalVersions, initializationTimeMs);
        } catch (Exception e) {
            log.warn("Failed to publish PluginSystemInitializedEvent: {}", e.getMessage());
        }
    }
    
    private boolean isEventsEnabled() {
        return properties.getEvents() != null && properties.getEvents().isEnabled();
    }
    
    private boolean isExecutionEventsEnabled() {
        return isEventsEnabled() && 
               properties.getEvents().isPublishExecutionEvents();
    }
}
