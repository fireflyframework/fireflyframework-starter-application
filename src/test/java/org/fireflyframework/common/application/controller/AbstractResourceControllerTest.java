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

package org.fireflyframework.common.application.controller;

import org.fireflyframework.common.application.context.AppConfig;
import org.fireflyframework.common.application.context.AppContext;
import org.fireflyframework.common.application.context.ApplicationExecutionContext;
import org.fireflyframework.common.application.resolver.ConfigResolver;
import org.fireflyframework.common.application.resolver.ContextResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AbstractResourceController}.
 *
 * <p>The resource controller is a thin, product-agnostic base: it resolves the
 * {@link AppContext} (subject, tenant, roles, permissions) from the {@link ContextResolver}
 * and provides generic operation logging. It carries no contract/product scoping.</p>
 */
@DisplayName("AbstractResourceController Tests")
@ExtendWith(MockitoExtension.class)
class AbstractResourceControllerTest {

    @Mock
    private ContextResolver contextResolver;

    @Mock
    private ConfigResolver configResolver;

    @Mock
    private ServerWebExchange exchange;

    private TestResourceController controller;

    private String testSubject;
    private UUID testTenantId;

    @BeforeEach
    void setUp() {
        controller = new TestResourceController();
        ReflectionTestUtils.setField(controller, "contextResolver", contextResolver);
        ReflectionTestUtils.setField(controller, "configResolver", configResolver);

        testSubject = "user-" + UUID.randomUUID();
        testTenantId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should log operation with operation name")
    void shouldLogOperation() {
        assertDoesNotThrow(() -> controller.testLogOperation("testOperation"));
    }

    @Test
    @DisplayName("Should handle null operation name in logging")
    void shouldHandleNullOperationNameInLogging() {
        assertDoesNotThrow(() -> controller.testLogOperation(null));
    }

    @Test
    @DisplayName("Should resolve execution context (subject + tenant + roles + permissions)")
    void shouldResolveExecutionContext() {
        // Given
        AppContext appContext = AppContext.builder()
                .subject(testSubject)
                .tenantId(testTenantId)
                .roles(Set.of("transaction:viewer"))
                .permissions(Set.of("transaction:read"))
                .build();

        AppConfig appConfig = AppConfig.builder()
                .tenantId(testTenantId)
                .tenantName("Test Tenant")
                .build();

        when(contextResolver.resolveContext(any(ServerWebExchange.class)))
                .thenReturn(Mono.just(appContext));
        when(configResolver.resolveConfig(testTenantId))
                .thenReturn(Mono.just(appConfig));

        // When
        Mono<ApplicationExecutionContext> result = controller.testResolveExecutionContext(exchange);

        // Then
        StepVerifier.create(result)
                .assertNext(ctx -> {
                    assertThat(ctx).isNotNull();
                    assertThat(ctx.getContext().getSubject()).isEqualTo(testSubject);
                    assertThat(ctx.getContext().getTenantId()).isEqualTo(testTenantId);
                    assertThat(ctx.getContext().getRoles()).containsExactly("transaction:viewer");
                    assertThat(ctx.getContext().getPermissions()).containsExactly("transaction:read");
                })
                .verifyComplete();

        verify(contextResolver).resolveContext(eq(exchange));
        verify(configResolver).resolveConfig(testTenantId);
    }

    @Test
    @DisplayName("Should propagate context resolution error")
    void shouldPropagateContextResolutionError() {
        when(contextResolver.resolveContext(any(ServerWebExchange.class)))
                .thenReturn(Mono.error(new IllegalStateException("No authenticated principal")));

        assertThrows(IllegalStateException.class,
                () -> controller.testResolveExecutionContext(exchange).block());
    }

    /**
     * Concrete test implementation of {@link AbstractResourceController}
     * to expose protected methods for testing.
     */
    static class TestResourceController extends AbstractResourceController {

        public void testLogOperation(String operation) {
            logOperation(operation);
        }

        public Mono<ApplicationExecutionContext> testResolveExecutionContext(ServerWebExchange exchange) {
            return resolveExecutionContext(exchange);
        }
    }
}
