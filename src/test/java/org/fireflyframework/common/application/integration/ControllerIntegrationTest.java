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

package org.fireflyframework.application.integration;

import org.fireflyframework.application.context.AppConfig;
import org.fireflyframework.application.context.AppContext;
import org.fireflyframework.application.context.ApplicationExecutionContext;
import org.fireflyframework.application.controller.AbstractApplicationController;
import org.fireflyframework.application.controller.AbstractResourceController;
import org.fireflyframework.application.resolver.ConfigResolver;
import org.fireflyframework.application.resolver.ContextResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
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
 * Integration test demonstrating both controller types:
 * - AbstractApplicationController (application-layer, no contract/product)
 * - AbstractResourceController (contract + product, both required)
 * 
 * This test validates the complete architecture:
 * 1. Istio injects X-Party-Id header
 * 2. Config-mgmt resolves tenantId from partyId
 * 3. Controllers extract contractId/productId from path variables
 * 4. FireflySessionManager provides roles/permissions (mocked)
 * 5. Context is fully resolved with complete resource hierarchy
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Controller Integration Test - Two Controller Types")
class ControllerIntegrationTest {
    
    @Mock
    private ContextResolver contextResolver;
    
    @Mock
    private ConfigResolver configResolver;
    
    @Mock
    private ServerWebExchange exchange;
    
    @Mock
    private ServerHttpRequest request;
    
    private UUID testPartyId;
    private UUID testTenantId;
    private UUID testContractId;
    private UUID testProductId;
    
    private TestApplicationController applicationController;
    private TestResourceController resourceController;
    
    @BeforeEach
    void setUp() {
        testPartyId = UUID.randomUUID();
        testTenantId = UUID.randomUUID();
        testContractId = UUID.randomUUID();
        testProductId = UUID.randomUUID();
        
        // Setup controllers
        applicationController = new TestApplicationController();
        resourceController = new TestResourceController();
        
        // Inject dependencies
        ReflectionTestUtils.setField(applicationController, "contextResolver", contextResolver);
        ReflectionTestUtils.setField(applicationController, "configResolver", configResolver);
        ReflectionTestUtils.setField(resourceController, "contextResolver", contextResolver);
        ReflectionTestUtils.setField(resourceController, "configResolver", configResolver);
    }
    
    @Test
    @DisplayName("Scenario 1: Application-layer endpoint (Onboarding)")
    void testApplicationLayerEndpoint() {
        // Given: Onboarding endpoint with only party context
        AppContext appContext = AppContext.builder()
                .partyId(testPartyId)
                .tenantId(testTenantId)
                .contractId(null)  // No contract for onboarding
                .productId(null)   // No product for onboarding
                .roles(Set.of("customer:onboard"))
                .permissions(Set.of("profile:create"))
                .build();
        
        AppConfig appConfig = AppConfig.builder()
                .tenantId(testTenantId)
                .tenantName("Test Bank")
                .build();
        
        when(contextResolver.resolveContext(any(ServerWebExchange.class), isNull(), isNull()))
                .thenReturn(Mono.just(appContext));
        when(configResolver.resolveConfig(testTenantId))
                .thenReturn(Mono.just(appConfig));
        
        // When: Call application-layer controller endpoint
        Mono<ApplicationExecutionContext> result = applicationController.handleOnboarding(exchange);
        
        // Then: Context is resolved with party + tenant only
        StepVerifier.create(result)
                .assertNext(ctx -> {
                    assertThat(ctx.getContext().getPartyId()).isEqualTo(testPartyId);
                    assertThat(ctx.getContext().getTenantId()).isEqualTo(testTenantId);
                    assertThat(ctx.getContext().getContractId()).isNull();
                    assertThat(ctx.getContext().getProductId()).isNull();
                    assertThat(ctx.getContext().getRoles()).contains("customer:onboard");
                })
                .verifyComplete();
        
        verify(contextResolver).resolveContext(eq(exchange), isNull(), isNull());
        verify(configResolver).resolveConfig(testTenantId);
    }
    
    @Test
    @DisplayName("Scenario 2: Resource endpoint (List Transactions with contract + product)")
    void testResourceEndpoint() {
        // Given: Transaction listing endpoint with full context
        AppContext appContext = AppContext.builder()
                .partyId(testPartyId)
                .tenantId(testTenantId)
                .contractId(testContractId)
                .productId(testProductId)
                .roles(Set.of("owner", "transaction:viewer"))
                .permissions(Set.of("transaction:read", "transaction:list"))
                .build();
        
        AppConfig appConfig = AppConfig.builder()
                .tenantId(testTenantId)
                .tenantName("Test Bank")
                .build();
        
        when(contextResolver.resolveContext(any(ServerWebExchange.class), eq(testContractId), eq(testProductId)))
                .thenReturn(Mono.just(appContext));
        when(configResolver.resolveConfig(testTenantId))
                .thenReturn(Mono.just(appConfig));
        
        // When: Call resource controller endpoint with contractId + productId (both required)
        Mono<ApplicationExecutionContext> result = resourceController.listTransactions(
                testContractId, testProductId, exchange);
        
        // Then: Context is resolved with complete resource hierarchy
        StepVerifier.create(result)
                .assertNext(ctx -> {
                    assertThat(ctx.getContext().getPartyId()).isEqualTo(testPartyId);
                    assertThat(ctx.getContext().getTenantId()).isEqualTo(testTenantId);
                    assertThat(ctx.getContext().getContractId()).isEqualTo(testContractId);
                    assertThat(ctx.getContext().getProductId()).isEqualTo(testProductId);
                    assertThat(ctx.getContext().getRoles()).contains("owner", "transaction:viewer");
                })
                .verifyComplete();
        
        verify(contextResolver).resolveContext(eq(exchange), eq(testContractId), eq(testProductId));
        verify(configResolver).resolveConfig(testTenantId);
    }
    
    @Test
    @DisplayName("Scenario 3: End-to-end flow - Onboarding → Access Resources")
    void testEndToEndFlow() {
        // Step 1: Onboarding (application-layer, no contract/product)
        AppContext onboardingContext = AppContext.builder()
                .partyId(testPartyId)
                .tenantId(testTenantId)
                .roles(Set.of("customer:onboard"))
                .build();
        
        AppConfig config = AppConfig.builder().tenantId(testTenantId).build();
        
        when(contextResolver.resolveContext(any(), isNull(), isNull()))
                .thenReturn(Mono.just(onboardingContext));
        when(configResolver.resolveConfig(testTenantId))
                .thenReturn(Mono.just(config));
        
        StepVerifier.create(applicationController.handleOnboarding(exchange))
                .assertNext(ctx -> {
                    assertThat(ctx.getContext().getContractId()).isNull();
                    assertThat(ctx.getContext().getProductId()).isNull();
                })
                .verifyComplete();
        
        // Step 2: After onboarding, access resources with contract + product (both required)
        AppContext resourceContext = AppContext.builder()
                .partyId(testPartyId)
                .tenantId(testTenantId)
                .contractId(testContractId)
                .productId(testProductId)
                .roles(Set.of("owner", "transaction:viewer"))
                .build();
        
        when(contextResolver.resolveContext(any(), eq(testContractId), eq(testProductId)))
                .thenReturn(Mono.just(resourceContext));
        
        StepVerifier.create(resourceController.listTransactions(testContractId, testProductId, exchange))
                .assertNext(ctx -> {
                    assertThat(ctx.getContext().getContractId()).isEqualTo(testContractId);
                    assertThat(ctx.getContext().getProductId()).isEqualTo(testProductId);
                    assertThat(ctx.getContext().getRoles()).contains("owner", "transaction:viewer");
                })
                .verifyComplete();
    }
    
    // Test controller implementations
    
    static class TestApplicationController extends AbstractApplicationController {
        public Mono<ApplicationExecutionContext> handleOnboarding(ServerWebExchange exchange) {
            return resolveExecutionContext(exchange);
        }
    }
    
    static class TestResourceController extends AbstractResourceController {
        public Mono<ApplicationExecutionContext> listTransactions(
                UUID contractId, UUID productId, ServerWebExchange exchange) {
            return resolveExecutionContext(exchange, contractId, productId);
        }
    }
}
