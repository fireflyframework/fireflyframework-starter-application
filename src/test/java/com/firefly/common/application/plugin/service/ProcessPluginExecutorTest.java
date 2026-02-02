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

package com.firefly.common.application.plugin.service;

import com.firefly.common.application.context.AppConfig;
import com.firefly.common.application.context.AppContext;
import com.firefly.common.application.context.ApplicationExecutionContext;
import com.firefly.common.application.plugin.*;
import com.firefly.common.application.plugin.config.PluginProperties;
import com.firefly.common.application.plugin.event.PluginEventPublisher;
import com.firefly.common.application.plugin.exception.ProcessNotFoundException;
import com.firefly.common.application.plugin.metrics.PluginMetricsService;
import com.firefly.common.application.security.SecurityAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ProcessPluginExecutor Tests")
class ProcessPluginExecutorTest {
    
    @Mock
    private ProcessPluginRegistry registry;
    
    @Mock
    private ProcessMappingService mappingService;
    
    @Mock
    private SecurityAuthorizationService authorizationService;
    
    @Mock
    private PluginProperties properties;
    
    @Mock
    private PluginProperties.SecurityProperties securityConfig;
    
    @Mock
    private PluginEventPublisher eventPublisher;
    
    @Mock
    private PluginMetricsService metricsService;
    
    private ProcessPluginExecutor executor;
    
    private ApplicationExecutionContext testContext;
    private UUID testTenantId;
    private UUID testProductId;
    
    @BeforeEach
    void setUp() {
        executor = new ProcessPluginExecutor(registry, mappingService, authorizationService, properties, 
                eventPublisher, metricsService);
        
        testTenantId = UUID.randomUUID();
        testProductId = UUID.randomUUID();
        
        testContext = ApplicationExecutionContext.builder()
                .context(AppContext.builder()
                        .tenantId(testTenantId)
                        .productId(testProductId)
                        .partyId(UUID.randomUUID())
                        .permissions(Set.of("accounts:create", "accounts:read"))
                        .roles(Set.of("USER", "ADMIN"))
                        .build())
                .config(AppConfig.builder()
                        .tenantId(testTenantId)
                        .build())
                .build();
        
        // Default mocking for timeout
        when(properties.getSecurity()).thenReturn(securityConfig);
        when(securityConfig.getMaxExecutionTime()).thenReturn(Duration.ofSeconds(30));
    }
    
    private ProcessPlugin createMockPlugin(String processId, String version) {
        return createMockPlugin(processId, version, null, ProcessResult.success(Map.of("result", "ok")));
    }
    
    private ProcessPlugin createMockPlugin(String processId, String version, ProcessMetadata metadata, ProcessResult result) {
        ProcessPlugin plugin = mock(ProcessPlugin.class);
        when(plugin.getProcessId()).thenReturn(processId);
        when(plugin.getVersion()).thenReturn(version);
        when(plugin.getMetadata()).thenReturn(metadata);
        when(plugin.validate(any())).thenReturn(Mono.just(ValidationResult.valid()));
        when(plugin.execute(any())).thenReturn(Mono.just(result));
        return plugin;
    }
    
    @Nested
    @DisplayName("Process Execution Tests")
    class ProcessExecutionTests {
        
        @Test
        @DisplayName("Should execute process successfully")
        void shouldExecuteProcessSuccessfully() {
            ProcessMapping mapping = ProcessMapping.builder()
                    .operationId("createAccount")
                    .processId("vanilla-account-creation")
                    .processVersion("1.0.0")
                    .build();
            
            ProcessPlugin plugin = createMockPlugin("vanilla-account-creation", "1.0.0");
            
            when(mappingService.resolveMapping(eq(testTenantId), eq("createAccount"), eq(testProductId), isNull()))
                    .thenReturn(Mono.just(mapping));
            when(registry.get("vanilla-account-creation", "1.0.0"))
                    .thenReturn(Optional.of(plugin));
            
            StepVerifier.create(executor.executeProcess(testContext, "createAccount", Map.of("amount", 100)))
                    .assertNext(result -> {
                        assertTrue(result.isSuccess());
                        @SuppressWarnings("unchecked")
                        Map<String, Object> output = (Map<String, Object>) result.getOutput();
                        assertEquals("ok", output.get("result"));
                    })
                    .verifyComplete();
            
            verify(plugin).validate(any());
            verify(plugin).execute(any());
        }
        
        @Test
        @DisplayName("Should execute process by ID directly")
        void shouldExecuteProcessByIdDirectly() {
            ProcessPlugin plugin = createMockPlugin("direct-process", "1.0.0");
            
            when(registry.get("direct-process", null))
                    .thenReturn(Optional.of(plugin));
            
            StepVerifier.create(executor.executeProcessById(testContext, "direct-process", Map.of()))
                    .assertNext(result -> assertTrue(result.isSuccess()))
                    .verifyComplete();
        }
        
        @Test
        @DisplayName("Should execute process with channel type")
        void shouldExecuteProcessWithChannelType() {
            ProcessMapping mapping = ProcessMapping.builder()
                    .operationId("createAccount")
                    .processId("mobile-account-creation")
                    .processVersion("1.0.0")
                    .channelType("MOBILE")
                    .build();
            
            ProcessPlugin plugin = createMockPlugin("mobile-account-creation", "1.0.0");
            
            when(mappingService.resolveMapping(eq(testTenantId), eq("createAccount"), eq(testProductId), eq("MOBILE")))
                    .thenReturn(Mono.just(mapping));
            when(registry.get("mobile-account-creation", "1.0.0"))
                    .thenReturn(Optional.of(plugin));
            
            StepVerifier.create(executor.executeProcess(testContext, "createAccount", Map.of(), "MOBILE"))
                    .assertNext(result -> assertTrue(result.isSuccess()))
                    .verifyComplete();
        }
    }
    
    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {
        
        @Test
        @DisplayName("Should return validation error when input validation fails")
        void shouldReturnValidationErrorWhenValidationFails() {
            ProcessMapping mapping = ProcessMapping.builder()
                    .operationId("createAccount")
                    .processId("test-process")
                    .build();
            
            ProcessPlugin plugin = mock(ProcessPlugin.class);
            when(plugin.getProcessId()).thenReturn("test-process");
            when(plugin.getVersion()).thenReturn("1.0.0");
            when(plugin.getMetadata()).thenReturn(null);
            when(plugin.validate(any())).thenReturn(Mono.just(
                    ValidationResult.error("amount", "Amount must be positive")
            ));
            
            when(mappingService.resolveMapping(any(), any(), any(), any()))
                    .thenReturn(Mono.just(mapping));
            when(registry.get(anyString(), any()))
                    .thenReturn(Optional.of(plugin));
            
            StepVerifier.create(executor.executeProcess(testContext, "createAccount", Map.of("amount", -100)))
                    .assertNext(result -> {
                        assertFalse(result.isSuccess());
                        assertEquals(ProcessResult.Status.BUSINESS_ERROR, result.getStatus());
                        assertEquals("VALIDATION_FAILED", result.getErrorCode());
                    })
                    .verifyComplete();
            
            // Execute should not be called when validation fails
            verify(plugin, never()).execute(any());
        }
    }
    
    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {
        
        @Test
        @DisplayName("Should return error when mapping not found")
        void shouldReturnErrorWhenMappingNotFound() {
            when(mappingService.resolveMapping(any(), any(), any(), any()))
                    .thenReturn(Mono.empty());
            
            StepVerifier.create(executor.executeProcess(testContext, "unknownOperation", Map.of()))
                    .assertNext(result -> {
                        assertFalse(result.isSuccess());
                        assertEquals(ProcessResult.Status.TECHNICAL_ERROR, result.getStatus());
                        assertEquals("PROCESS_NOT_FOUND", result.getErrorCode());
                    })
                    .verifyComplete();
        }
        
        @Test
        @DisplayName("Should return error when plugin not found in registry")
        void shouldReturnErrorWhenPluginNotFound() {
            ProcessMapping mapping = ProcessMapping.builder()
                    .operationId("createAccount")
                    .processId("missing-process")
                    .processVersion("1.0.0")
                    .build();
            
            when(mappingService.resolveMapping(any(), any(), any(), any()))
                    .thenReturn(Mono.just(mapping));
            when(registry.get("missing-process", "1.0.0"))
                    .thenReturn(Optional.empty());
            
            StepVerifier.create(executor.executeProcess(testContext, "createAccount", Map.of()))
                    .assertNext(result -> {
                        assertFalse(result.isSuccess());
                        assertEquals(ProcessResult.Status.TECHNICAL_ERROR, result.getStatus());
                        assertEquals("PROCESS_NOT_FOUND", result.getErrorCode());
                    })
                    .verifyComplete();
        }
        
        @Test
        @DisplayName("Should handle process execution failure")
        void shouldHandleProcessExecutionFailure() {
            ProcessMapping mapping = ProcessMapping.builder()
                    .operationId("failingOp")
                    .processId("failing-process")
                    .build();
            
            ProcessPlugin plugin = mock(ProcessPlugin.class);
            when(plugin.getProcessId()).thenReturn("failing-process");
            when(plugin.getVersion()).thenReturn("1.0.0");
            when(plugin.getMetadata()).thenReturn(null);
            when(plugin.validate(any())).thenReturn(Mono.just(ValidationResult.valid()));
            when(plugin.execute(any())).thenReturn(Mono.error(new RuntimeException("Database connection failed")));
            
            when(mappingService.resolveMapping(any(), any(), any(), any()))
                    .thenReturn(Mono.just(mapping));
            when(registry.get(anyString(), any()))
                    .thenReturn(Optional.of(plugin));
            
            StepVerifier.create(executor.executeProcess(testContext, "failingOp", Map.of()))
                    .assertNext(result -> {
                        assertFalse(result.isSuccess());
                        assertEquals(ProcessResult.Status.TECHNICAL_ERROR, result.getStatus());
                        assertEquals("RuntimeException", result.getErrorCode());
                        assertTrue(result.getErrorMessage().contains("Database connection failed"));
                    })
                    .verifyComplete();
        }
    }
    
    @Nested
    @DisplayName("Permission Tests")
    class PermissionTests {
        
        @Test
        @DisplayName("Should deny access when required permissions are missing")
        void shouldDenyAccessWhenPermissionsMissing() {
            ProcessMapping mapping = ProcessMapping.builder()
                    .operationId("adminOp")
                    .processId("admin-process")
                    .build();
            
            ProcessMetadata metadata = ProcessMetadata.builder()
                    .processId("admin-process")
                    .requiredPermissions(Set.of("admin:super", "admin:delete"))
                    .build();
            
            ProcessPlugin plugin = mock(ProcessPlugin.class);
            when(plugin.getProcessId()).thenReturn("admin-process");
            when(plugin.getVersion()).thenReturn("1.0.0");
            when(plugin.getMetadata()).thenReturn(metadata);
            
            when(mappingService.resolveMapping(any(), any(), any(), any()))
                    .thenReturn(Mono.just(mapping));
            when(registry.get(anyString(), any()))
                    .thenReturn(Optional.of(plugin));
            when(authorizationService.hasAllPermissions(any(), eq(Set.of("admin:super", "admin:delete"))))
                    .thenReturn(Mono.just(false));
            
            StepVerifier.create(executor.executeProcess(testContext, "adminOp", Map.of()))
                    .assertNext(result -> {
                        assertFalse(result.isSuccess());
                        assertEquals(ProcessResult.Status.BUSINESS_ERROR, result.getStatus());
                        assertEquals("ACCESS_DENIED", result.getErrorCode());
                    })
                    .verifyComplete();
        }
        
        @Test
        @DisplayName("Should allow access when required permissions are present")
        void shouldAllowAccessWhenPermissionsPresent() {
            ProcessMapping mapping = ProcessMapping.builder()
                    .operationId("createAccount")
                    .processId("account-process")
                    .build();
            
            ProcessMetadata metadata = ProcessMetadata.builder()
                    .processId("account-process")
                    .requiredPermissions(Set.of("accounts:create"))
                    .build();
            
            ProcessPlugin plugin = createMockPlugin("account-process", "1.0.0", metadata, ProcessResult.success(Map.of()));
            
            when(mappingService.resolveMapping(any(), any(), any(), any()))
                    .thenReturn(Mono.just(mapping));
            when(registry.get(anyString(), any()))
                    .thenReturn(Optional.of(plugin));
            when(authorizationService.hasAllPermissions(any(), eq(Set.of("accounts:create"))))
                    .thenReturn(Mono.just(true));
            
            StepVerifier.create(executor.executeProcess(testContext, "createAccount", Map.of()))
                    .assertNext(result -> assertTrue(result.isSuccess()))
                    .verifyComplete();
        }
        
        @Test
        @DisplayName("Should check roles when no permissions defined")
        void shouldCheckRolesWhenNoPermissionsDefined() {
            ProcessMapping mapping = ProcessMapping.builder()
                    .operationId("userOp")
                    .processId("user-process")
                    .build();
            
            ProcessMetadata metadata = ProcessMetadata.builder()
                    .processId("user-process")
                    .requiredRoles(Set.of("USER", "VIEWER"))
                    .build();
            
            ProcessPlugin plugin = createMockPlugin("user-process", "1.0.0", metadata, ProcessResult.success(Map.of()));
            
            when(mappingService.resolveMapping(any(), any(), any(), any()))
                    .thenReturn(Mono.just(mapping));
            when(registry.get(anyString(), any()))
                    .thenReturn(Optional.of(plugin));
            when(authorizationService.hasAnyRole(any(), eq(Set.of("USER", "VIEWER"))))
                    .thenReturn(Mono.just(true));
            
            StepVerifier.create(executor.executeProcess(testContext, "userOp", Map.of()))
                    .assertNext(result -> assertTrue(result.isSuccess()))
                    .verifyComplete();
        }
    }
    
    @Nested
    @DisplayName("Deprecated Process Tests")
    class DeprecatedProcessTests {
        
        @Test
        @DisplayName("Should still execute deprecated process with warning")
        void shouldExecuteDeprecatedProcessWithWarning() {
            ProcessMapping mapping = ProcessMapping.builder()
                    .operationId("oldOp")
                    .processId("old-process")
                    .build();
            
            ProcessMetadata metadata = ProcessMetadata.builder()
                    .processId("old-process")
                    .deprecated(true)
                    .replacedBy("new-process")
                    .build();
            
            ProcessPlugin plugin = createMockPlugin("old-process", "1.0.0", metadata, ProcessResult.success(Map.of()));
            
            when(mappingService.resolveMapping(any(), any(), any(), any()))
                    .thenReturn(Mono.just(mapping));
            when(registry.get(anyString(), any()))
                    .thenReturn(Optional.of(plugin));
            
            StepVerifier.create(executor.executeProcess(testContext, "oldOp", Map.of()))
                    .assertNext(result -> assertTrue(result.isSuccess()))
                    .verifyComplete();
        }
    }
    
    @Nested
    @DisplayName("Feature Flag Tests")
    class FeatureFlagTests {
        
        @Test
        @DisplayName("Should fail when required feature is disabled")
        void shouldFailWhenRequiredFeatureDisabled() {
            ProcessMapping mapping = ProcessMapping.builder()
                    .operationId("newFeatureOp")
                    .processId("new-feature-process")
                    .build();
            
            ProcessMetadata metadata = ProcessMetadata.builder()
                    .processId("new-feature-process")
                    .requiredFeatures(Set.of("NEW_PAYMENT_FLOW"))
                    .build();
            
            ProcessPlugin plugin = mock(ProcessPlugin.class);
            when(plugin.getProcessId()).thenReturn("new-feature-process");
            when(plugin.getVersion()).thenReturn("1.0.0");
            when(plugin.getMetadata()).thenReturn(metadata);
            
            // Context with feature disabled
            ApplicationExecutionContext contextWithFeatureOff = ApplicationExecutionContext.builder()
                    .context(AppContext.builder()
                            .tenantId(testTenantId)
                            .productId(testProductId)
                            .partyId(UUID.randomUUID())
                            .build())
                    .config(AppConfig.builder()
                            .tenantId(testTenantId)
                            .featureFlags(Map.of("NEW_PAYMENT_FLOW", false))
                            .build())
                    .build();
            
            when(mappingService.resolveMapping(any(), any(), any(), any()))
                    .thenReturn(Mono.just(mapping));
            when(registry.get(anyString(), any()))
                    .thenReturn(Optional.of(plugin));
            
            StepVerifier.create(executor.executeProcess(contextWithFeatureOff, "newFeatureOp", Map.of()))
                    .assertNext(result -> {
                        assertFalse(result.isSuccess());
                        assertEquals(ProcessResult.Status.TECHNICAL_ERROR, result.getStatus());
                        assertTrue(result.getErrorMessage().contains("Required feature not enabled"));
                    })
                    .verifyComplete();
        }
    }
}
