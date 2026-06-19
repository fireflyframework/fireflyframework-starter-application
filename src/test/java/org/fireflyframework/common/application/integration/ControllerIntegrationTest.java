/*
 * Copyright 2024-2026 Firefly Software Foundation
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

package org.fireflyframework.common.application.integration;

import org.fireflyframework.common.application.context.AppConfig;
import org.fireflyframework.common.application.context.AppContext;
import org.fireflyframework.common.application.context.ApplicationExecutionContext;
import org.fireflyframework.common.application.controller.AbstractApplicationController;
import org.fireflyframework.common.application.controller.AbstractResourceController;
import org.fireflyframework.common.application.resolver.ConfigResolver;
import org.fireflyframework.common.application.resolver.ContextResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration test demonstrating both product-agnostic controller types:
 * <ul>
 *   <li>{@link AbstractApplicationController} (application-layer endpoints)</li>
 *   <li>{@link AbstractResourceController} (resource endpoints)</li>
 * </ul>
 *
 * <p>This test validates the resolution flow:</p>
 * <ol>
 *   <li>The validated security principal yields the authenticated subject + tenant</li>
 *   <li>The {@link ContextResolver} produces a product-agnostic {@link AppContext}
 *       (subject, tenant, roles, permissions)</li>
 *   <li>The {@link ConfigResolver} loads tenant {@link AppConfig}</li>
 *   <li>The controller assembles a complete {@link ApplicationExecutionContext}</li>
 * </ol>
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

    private String testSubject;
    private UUID testTenantId;

    private TestApplicationController applicationController;
    private TestResourceController resourceController;

    @BeforeEach
    void setUp() {
        testSubject = "user-" + UUID.randomUUID();
        testTenantId = UUID.randomUUID();

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
    @DisplayName("Scenario 1: Application-layer endpoint")
    void testApplicationLayerEndpoint() {
        // Given: application-layer endpoint with subject + tenant context
        AppContext appContext = AppContext.builder()
                .subject(testSubject)
                .tenantId(testTenantId)
                .roles(Set.of("customer:onboard"))
                .permissions(Set.of("profile:create"))
                .build();

        AppConfig appConfig = AppConfig.builder()
                .tenantId(testTenantId)
                .tenantName("Test Bank")
                .build();

        when(contextResolver.resolveContext(any(ServerWebExchange.class)))
                .thenReturn(Mono.just(appContext));
        when(configResolver.resolveConfig(testTenantId))
                .thenReturn(Mono.just(appConfig));

        // When: call application-layer controller endpoint
        Mono<ApplicationExecutionContext> result = applicationController.handleOnboarding(exchange);

        // Then: context is resolved with subject + tenant + roles
        StepVerifier.create(result)
                .assertNext(ctx -> {
                    assertThat(ctx.getContext().getSubject()).isEqualTo(testSubject);
                    assertThat(ctx.getContext().getTenantId()).isEqualTo(testTenantId);
                    assertThat(ctx.getContext().getRoles()).contains("customer:onboard");
                    assertThat(ctx.getContext().getPermissions()).contains("profile:create");
                })
                .verifyComplete();

        verify(contextResolver).resolveContext(eq(exchange));
        verify(configResolver).resolveConfig(testTenantId);
    }

    @Test
    @DisplayName("Scenario 2: Resource endpoint")
    void testResourceEndpoint() {
        // Given: resource endpoint with subject + tenant + roles/permissions
        AppContext appContext = AppContext.builder()
                .subject(testSubject)
                .tenantId(testTenantId)
                .roles(Set.of("owner", "transaction:viewer"))
                .permissions(Set.of("transaction:read", "transaction:list"))
                .build();

        AppConfig appConfig = AppConfig.builder()
                .tenantId(testTenantId)
                .tenantName("Test Bank")
                .build();

        when(contextResolver.resolveContext(any(ServerWebExchange.class)))
                .thenReturn(Mono.just(appContext));
        when(configResolver.resolveConfig(testTenantId))
                .thenReturn(Mono.just(appConfig));

        // When: call resource controller endpoint
        Mono<ApplicationExecutionContext> result = resourceController.listTransactions(exchange);

        // Then: context is resolved with subject + tenant + authorities
        StepVerifier.create(result)
                .assertNext(ctx -> {
                    assertThat(ctx.getContext().getSubject()).isEqualTo(testSubject);
                    assertThat(ctx.getContext().getTenantId()).isEqualTo(testTenantId);
                    assertThat(ctx.getContext().getRoles()).contains("owner", "transaction:viewer");
                    assertThat(ctx.getContext().getPermissions()).contains("transaction:read");
                })
                .verifyComplete();

        verify(contextResolver).resolveContext(eq(exchange));
        verify(configResolver).resolveConfig(testTenantId);
    }

    @Test
    @DisplayName("Scenario 3: End-to-end flow across both controller types")
    void testEndToEndFlow() {
        // Step 1: application-layer endpoint
        AppContext onboardingContext = AppContext.builder()
                .subject(testSubject)
                .tenantId(testTenantId)
                .roles(Set.of("customer:onboard"))
                .build();

        AppConfig config = AppConfig.builder().tenantId(testTenantId).build();

        when(contextResolver.resolveContext(any(ServerWebExchange.class)))
                .thenReturn(Mono.just(onboardingContext));
        when(configResolver.resolveConfig(testTenantId))
                .thenReturn(Mono.just(config));

        StepVerifier.create(applicationController.handleOnboarding(exchange))
                .assertNext(ctx -> {
                    assertThat(ctx.getContext().getSubject()).isEqualTo(testSubject);
                    assertThat(ctx.getContext().getRoles()).contains("customer:onboard");
                })
                .verifyComplete();

        // Step 2: resource endpoint resolved from the same authenticated principal
        AppContext resourceContext = AppContext.builder()
                .subject(testSubject)
                .tenantId(testTenantId)
                .roles(Set.of("owner", "transaction:viewer"))
                .build();

        when(contextResolver.resolveContext(any(ServerWebExchange.class)))
                .thenReturn(Mono.just(resourceContext));

        StepVerifier.create(resourceController.listTransactions(exchange))
                .assertNext(ctx -> {
                    assertThat(ctx.getContext().getSubject()).isEqualTo(testSubject);
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
        public Mono<ApplicationExecutionContext> listTransactions(ServerWebExchange exchange) {
            return resolveExecutionContext(exchange);
        }
    }
}
