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

package org.fireflyframework.application.service;

import org.fireflyframework.application.context.AppConfig;
import org.fireflyframework.application.context.AppContext;
import org.fireflyframework.application.context.ApplicationExecutionContext;
import org.fireflyframework.application.resolver.ConfigResolver;
import org.fireflyframework.application.resolver.ContextResolver;
import org.fireflyframework.application.security.SecurityAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AbstractApplicationService}.
 */
@DisplayName("AbstractApplicationService Tests")
class AbstractApplicationServiceTest {
    
    private ContextResolver contextResolver;
    private ConfigResolver configResolver;
    private SecurityAuthorizationService authorizationService;
    private TestApplicationService applicationService;
    private ServerWebExchange exchange;
    
    @BeforeEach
    void setUp() {
        contextResolver = mock(ContextResolver.class);
        configResolver = mock(ConfigResolver.class);
        authorizationService = mock(SecurityAuthorizationService.class);
        applicationService = new TestApplicationService(contextResolver, configResolver, authorizationService);
        exchange = mock(ServerWebExchange.class);
    }
    
    @Test
    @DisplayName("Should resolve execution context successfully")
    void shouldResolveExecutionContext() {
        // Given
        UUID tenantId = UUID.randomUUID();
        AppContext appContext = AppContext.builder()
                .partyId(UUID.randomUUID())
                .tenantId(tenantId)
                .contractId(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .build();
        
        AppConfig appConfig = AppConfig.builder()
                .tenantId(tenantId)
                .active(true)
                .providers(new HashMap<>())
                .featureFlags(new HashMap<>())
                .settings(new HashMap<>())
                .build();
        
        when(contextResolver.resolveContext(exchange)).thenReturn(Mono.just(appContext));
        when(configResolver.resolveConfig(tenantId)).thenReturn(Mono.just(appConfig));
        
        // When/Then
        StepVerifier.create(applicationService.resolveExecutionContext(exchange))
                .assertNext(executionContext -> {
                    assertThat(executionContext.getContext()).isEqualTo(appContext);
                    assertThat(executionContext.getConfig()).isEqualTo(appConfig);
                })
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should validate context successfully when requirements are met")
    void shouldValidateContextSuccessfully() {
        // Given
        ApplicationExecutionContext context = createExecutionContext();
        
        // When/Then
        StepVerifier.create(applicationService.validateContext(context, true, true))
                .expectNext(context)
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should fail validation when contract is required but missing")
    void shouldFailValidationWhenContractMissing() {
        // Given
        ApplicationExecutionContext context = ApplicationExecutionContext.builder()
                .context(AppContext.builder()
                        .partyId(UUID.randomUUID())
                        .tenantId(UUID.randomUUID())
                        .productId(UUID.randomUUID())
                        .build())
                .config(AppConfig.builder()
                        .tenantId(UUID.randomUUID())
                        .active(true)
                        .providers(new HashMap<>())
                        .featureFlags(new HashMap<>())
                        .settings(new HashMap<>())
                        .build())
                .build();
        
        // When/Then
        StepVerifier.create(applicationService.validateContext(context, true, false))
                .expectErrorMatches(error -> 
                        error instanceof IllegalStateException &&
                        error.getMessage().contains("Contract ID is required"))
                .verify();
    }
    
    @Test
    @DisplayName("Should fail validation when product is required but missing")
    void shouldFailValidationWhenProductMissing() {
        // Given
        ApplicationExecutionContext context = ApplicationExecutionContext.builder()
                .context(AppContext.builder()
                        .partyId(UUID.randomUUID())
                        .tenantId(UUID.randomUUID())
                        .contractId(UUID.randomUUID())
                        .build())
                .config(AppConfig.builder()
                        .tenantId(UUID.randomUUID())
                        .active(true)
                        .providers(new HashMap<>())
                        .featureFlags(new HashMap<>())
                        .settings(new HashMap<>())
                        .build())
                .build();
        
        // When/Then
        StepVerifier.create(applicationService.validateContext(context, false, true))
                .expectErrorMatches(error -> 
                        error instanceof IllegalStateException &&
                        error.getMessage().contains("Product ID is required"))
                .verify();
    }
    
    @Test
    @DisplayName("Should require role successfully when present")
    void shouldRequireRoleSuccessfully() {
        // Given
        ApplicationExecutionContext context = createExecutionContext();
        when(authorizationService.hasRole(context.getContext(), "ROLE_ADMIN"))
                .thenReturn(Mono.just(true));
        
        // When/Then
        StepVerifier.create(applicationService.requireRole(context, "ROLE_ADMIN"))
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should fail when required role is missing")
    void shouldFailWhenRequiredRoleMissing() {
        // Given
        ApplicationExecutionContext context = createExecutionContext();
        when(authorizationService.hasRole(context.getContext(), "ROLE_SUPERADMIN"))
                .thenReturn(Mono.just(false));
        
        // When/Then
        StepVerifier.create(applicationService.requireRole(context, "ROLE_SUPERADMIN"))
                .expectError(AccessDeniedException.class)
                .verify();
    }
    
    @Test
    @DisplayName("Should require permission successfully when granted")
    void shouldRequirePermissionSuccessfully() {
        // Given
        ApplicationExecutionContext context = createExecutionContext();
        when(authorizationService.hasPermission(context.getContext(), "WRITE"))
                .thenReturn(Mono.just(true));
        
        // When/Then
        StepVerifier.create(applicationService.requirePermission(context, "WRITE"))
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should fail when required permission is missing")
    void shouldFailWhenRequiredPermissionMissing() {
        // Given
        ApplicationExecutionContext context = createExecutionContext();
        when(authorizationService.hasPermission(context.getContext(), "DELETE"))
                .thenReturn(Mono.just(false));
        
        // When/Then
        StepVerifier.create(applicationService.requirePermission(context, "DELETE"))
                .expectError(AccessDeniedException.class)
                .verify();
    }
    
    @Test
    @DisplayName("Should get provider config successfully")
    void shouldGetProviderConfig() {
        // Given
        ApplicationExecutionContext context = createExecutionContextWithProvider();
        
        // When/Then
        StepVerifier.create(applicationService.getProviderConfig(context, "payment"))
                .assertNext(providerConfig -> {
                    assertThat(providerConfig.getProviderType()).isEqualTo("payment");
                    assertThat(providerConfig.getImplementation()).isEqualTo("stripe");
                })
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should fail when provider is not configured")
    void shouldFailWhenProviderNotConfigured() {
        // Given
        ApplicationExecutionContext context = createExecutionContext();
        
        // When/Then
        StepVerifier.create(applicationService.getProviderConfig(context, "nonexistent"))
                .expectErrorMatches(error -> 
                        error instanceof IllegalStateException &&
                        error.getMessage().contains("Provider not configured"))
                .verify();
    }
    
    @Test
    @DisplayName("Should check if feature is enabled")
    void shouldCheckIfFeatureIsEnabled() {
        // Given
        ApplicationExecutionContext context = createExecutionContextWithFeature();
        
        // When/Then
        StepVerifier.create(applicationService.isFeatureEnabled(context, "new-ui"))
                .expectNext(true)
                .verifyComplete();
        
        StepVerifier.create(applicationService.isFeatureEnabled(context, "old-feature"))
                .expectNext(false)
                .verifyComplete();
    }
    
    private ApplicationExecutionContext createExecutionContext() {
        return ApplicationExecutionContext.builder()
                .context(AppContext.builder()
                        .partyId(UUID.randomUUID())
                        .tenantId(UUID.randomUUID())
                        .contractId(UUID.randomUUID())
                        .productId(UUID.randomUUID())
                        .build())
                .config(AppConfig.builder()
                        .tenantId(UUID.randomUUID())
                        .active(true)
                        .providers(new HashMap<>())
                        .featureFlags(new HashMap<>())
                        .settings(new HashMap<>())
                        .build())
                .build();
    }
    
    private ApplicationExecutionContext createExecutionContextWithProvider() {
        Map<String, AppConfig.ProviderConfig> providers = new HashMap<>();
        providers.put("payment", AppConfig.ProviderConfig.builder()
                .providerType("payment")
                .implementation("stripe")
                .enabled(true)
                .properties(new HashMap<>())
                .build());
        
        return ApplicationExecutionContext.builder()
                .context(AppContext.builder()
                        .partyId(UUID.randomUUID())
                        .tenantId(UUID.randomUUID())
                        .contractId(UUID.randomUUID())
                        .productId(UUID.randomUUID())
                        .build())
                .config(AppConfig.builder()
                        .tenantId(UUID.randomUUID())
                        .active(true)
                        .providers(providers)
                        .featureFlags(new HashMap<>())
                        .settings(new HashMap<>())
                        .build())
                .build();
    }
    
    private ApplicationExecutionContext createExecutionContextWithFeature() {
        Map<String, Boolean> featureFlags = new HashMap<>();
        featureFlags.put("new-ui", true);
        
        return ApplicationExecutionContext.builder()
                .context(AppContext.builder()
                        .partyId(UUID.randomUUID())
                        .tenantId(UUID.randomUUID())
                        .contractId(UUID.randomUUID())
                        .productId(UUID.randomUUID())
                        .build())
                .config(AppConfig.builder()
                        .tenantId(UUID.randomUUID())
                        .active(true)
                        .providers(new HashMap<>())
                        .featureFlags(featureFlags)
                        .settings(new HashMap<>())
                        .build())
                .build();
    }
    
    /**
     * Test implementation of AbstractApplicationService.
     */
    private static class TestApplicationService extends AbstractApplicationService {
        
        protected TestApplicationService(ContextResolver contextResolver,
                                        ConfigResolver configResolver,
                                        SecurityAuthorizationService authorizationService) {
            super(contextResolver, configResolver, authorizationService);
        }
    }
}
