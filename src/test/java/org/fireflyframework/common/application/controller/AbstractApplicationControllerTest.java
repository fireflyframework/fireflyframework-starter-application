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

package org.fireflyframework.application.controller;

import org.fireflyframework.application.context.AppConfig;
import org.fireflyframework.application.context.AppContext;
import org.fireflyframework.application.context.ApplicationExecutionContext;
import org.fireflyframework.application.resolver.ConfigResolver;
import org.fireflyframework.application.resolver.ContextResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AbstractApplicationController.
 * Tests application-layer context resolution (no contract or product).
 */
@ExtendWith(MockitoExtension.class)
class AbstractApplicationControllerTest {
    
    @Mock
    private ContextResolver contextResolver;
    
    @Mock
    private ConfigResolver configResolver;
    
    @Mock
    private ServerWebExchange exchange;
    
    private TestApplicationController controller;
    
    private UUID testPartyId;
    private UUID testTenantId;
    
    @BeforeEach
    void setUp() {
        controller = new TestApplicationController();
        ReflectionTestUtils.setField(controller, "contextResolver", contextResolver);
        ReflectionTestUtils.setField(controller, "configResolver", configResolver);
        
        testPartyId = UUID.randomUUID();
        testTenantId = UUID.randomUUID();
    }
    
    @Test
    void shouldResolveApplicationLayerContext() {
        // Given
        AppContext appContext = AppContext.builder()
                .partyId(testPartyId)
                .tenantId(testTenantId)
                .contractId(null)  // No contract for application-layer
                .productId(null)   // No product for application-layer
                .roles(Set.of("customer:onboard"))
                .permissions(Set.of())
                .build();
        
        AppConfig appConfig = AppConfig.builder()
                .tenantId(testTenantId)
                .tenantName("Test Tenant")
                .build();
        
        when(contextResolver.resolveContext(any(ServerWebExchange.class), isNull(), isNull()))
                .thenReturn(Mono.just(appContext));
        when(configResolver.resolveConfig(testTenantId))
                .thenReturn(Mono.just(appConfig));
        
        // When
        Mono<ApplicationExecutionContext> result = controller.resolveExecutionContext(exchange);
        
        // Then
        StepVerifier.create(result)
                .assertNext(ctx -> {
                    assertThat(ctx).isNotNull();
                    assertThat(ctx.getContext()).isEqualTo(appContext);
                    assertThat(ctx.getConfig()).isEqualTo(appConfig);
                    assertThat(ctx.getContext().getPartyId()).isEqualTo(testPartyId);
                    assertThat(ctx.getContext().getTenantId()).isEqualTo(testTenantId);
                    assertThat(ctx.getContext().getContractId()).isNull();
                    assertThat(ctx.getContext().getProductId()).isNull();
                })
                .verifyComplete();
        
        verify(contextResolver).resolveContext(eq(exchange), isNull(), isNull());
        verify(configResolver).resolveConfig(testTenantId);
    }
    
    @Test
    void shouldHandleContextResolutionError() {
        // Given
        when(contextResolver.resolveContext(any(ServerWebExchange.class), isNull(), isNull()))
                .thenReturn(Mono.error(new IllegalStateException("X-Party-Id header not found")));
        
        // When
        Mono<ApplicationExecutionContext> result = controller.resolveExecutionContext(exchange);
        
        // Then
        StepVerifier.create(result)
                .expectErrorMatches(error -> 
                    error instanceof IllegalStateException &&
                    error.getMessage().contains("X-Party-Id header not found"))
                .verify();
    }
    
    @Test
    void shouldHandleConfigResolutionError() {
        // Given
        AppContext appContext = AppContext.builder()
                .partyId(testPartyId)
                .tenantId(testTenantId)
                .build();
        
        when(contextResolver.resolveContext(any(ServerWebExchange.class), isNull(), isNull()))
                .thenReturn(Mono.just(appContext));
        when(configResolver.resolveConfig(testTenantId))
                .thenReturn(Mono.error(new RuntimeException("Config service unavailable")));
        
        // When
        Mono<ApplicationExecutionContext> result = controller.resolveExecutionContext(exchange);
        
        // Then
        StepVerifier.create(result)
                .expectErrorMatches(error -> 
                    error instanceof RuntimeException &&
                    error.getMessage().contains("Config service unavailable"))
                .verify();
    }
    
    @Test
    void shouldResolveContextWithRolesAndPermissions() {
        // Given
        AppContext appContext = AppContext.builder()
                .partyId(testPartyId)
                .tenantId(testTenantId)
                .roles(Set.of("customer:onboard", "customer:viewer"))
                .permissions(Set.of("profile:read", "profile:update"))
                .build();
        
        AppConfig appConfig = AppConfig.builder()
                .tenantId(testTenantId)
                .build();
        
        when(contextResolver.resolveContext(any(ServerWebExchange.class), isNull(), isNull()))
                .thenReturn(Mono.just(appContext));
        when(configResolver.resolveConfig(testTenantId))
                .thenReturn(Mono.just(appConfig));
        
        // When
        Mono<ApplicationExecutionContext> result = controller.resolveExecutionContext(exchange);
        
        // Then
        StepVerifier.create(result)
                .assertNext(ctx -> {
                    assertThat(ctx.getContext().getRoles()).containsExactlyInAnyOrder(
                        "customer:onboard", "customer:viewer");
                    assertThat(ctx.getContext().getPermissions()).containsExactlyInAnyOrder(
                        "profile:read", "profile:update");
                })
                .verifyComplete();
    }
    
    /**
     * Concrete test implementation of AbstractApplicationController.
     */
    private static class TestApplicationController extends AbstractApplicationController {
        // Test implementation - inherits all functionality from AbstractApplicationController
    }
}
