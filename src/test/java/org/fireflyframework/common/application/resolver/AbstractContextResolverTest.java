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

package org.fireflyframework.common.application.resolver;

import org.fireflyframework.common.application.context.AppContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link AbstractContextResolver}.
 */
@DisplayName("AbstractContextResolver Tests")
class AbstractContextResolverTest {

    private TestContextResolver contextResolver;
    private ServerWebExchange exchange;

    @BeforeEach
    void setUp() {
        contextResolver = new TestContextResolver();
        exchange = mock(ServerWebExchange.class);
    }

    @Test
    @DisplayName("Should resolve context with subject and tenant")
    void shouldResolveContextWithSubjectAndTenant() {
        // Given
        String subject = "user-123";
        UUID tenantId = UUID.randomUUID();

        contextResolver.setSubject(subject);
        contextResolver.setTenantId(tenantId);

        // When/Then
        StepVerifier.create(contextResolver.resolveContext(exchange))
                .assertNext(context -> {
                    assertThat(context.getSubject()).isEqualTo(subject);
                    assertThat(context.getTenantId()).isEqualTo(tenantId);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should resolve context with empty tenant when single-tenant")
    void shouldResolveContextWithEmptyTenant() {
        // Given
        String subject = "user-123";
        contextResolver.setSubject(subject);
        contextResolver.setTenantId(null);

        // When/Then
        StepVerifier.create(contextResolver.resolveContext(exchange))
                .assertNext(context -> {
                    assertThat(context.getSubject()).isEqualTo(subject);
                    assertThat(context.getTenantId()).isNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should default roles to empty when not overridden")
    void shouldDefaultRolesToEmpty() {
        // Given
        contextResolver.setSubject("user-123");

        // When/Then
        StepVerifier.create(contextResolver.resolveContext(exchange))
                .assertNext(context -> assertThat(context.getRoles()).isEmpty())
                .verifyComplete();
    }

    @Test
    @DisplayName("Should default permissions to empty when not overridden")
    void shouldDefaultPermissionsToEmpty() {
        // Given
        contextResolver.setSubject("user-123");

        // When/Then
        StepVerifier.create(contextResolver.resolveContext(exchange))
                .assertNext(context -> assertThat(context.getPermissions()).isEmpty())
                .verifyComplete();
    }

    @Test
    @DisplayName("Should enrich context with resolved roles and permissions")
    void shouldEnrichContextWithRolesAndPermissions() {
        // Given
        Set<String> roles = Set.of("ROLE_ADMIN", "ROLE_USER");
        Set<String> permissions = Set.of("READ", "WRITE");

        TestContextResolver enrichedResolver = new TestContextResolver();
        enrichedResolver.setSubject("user-123");
        enrichedResolver.setRoles(roles);
        enrichedResolver.setPermissions(permissions);

        // When/Then
        StepVerifier.create(enrichedResolver.resolveContext(exchange))
                .assertNext(context -> {
                    assertThat(context.getRoles()).containsExactlyInAnyOrderElementsOf(roles);
                    assertThat(context.getPermissions()).containsExactlyInAnyOrderElementsOf(permissions);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should support by default and have zero priority")
    void shouldSupportByDefault() {
        assertThat(contextResolver.supports(exchange)).isTrue();
        assertThat(contextResolver.getPriority()).isZero();
    }

    /**
     * Test implementation of AbstractContextResolver.
     */
    private static class TestContextResolver extends AbstractContextResolver {

        private String subject = "subject-" + UUID.randomUUID();
        private UUID tenantId = UUID.randomUUID();
        private Set<String> roles = Set.of();
        private Set<String> permissions = Set.of();

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public void setTenantId(UUID tenantId) {
            this.tenantId = tenantId;
        }

        public void setRoles(Set<String> roles) {
            this.roles = roles;
        }

        public void setPermissions(Set<String> permissions) {
            this.permissions = permissions;
        }

        @Override
        public Mono<String> resolveSubject(ServerWebExchange exchange) {
            return Mono.just(subject);
        }

        @Override
        public Mono<UUID> resolveTenantId(ServerWebExchange exchange) {
            return tenantId != null ? Mono.just(tenantId) : Mono.empty();
        }

        @Override
        protected Mono<Set<String>> resolveRoles(String subject, ServerWebExchange exchange) {
            return Mono.just(roles);
        }

        @Override
        protected Mono<Set<String>> resolvePermissions(String subject, ServerWebExchange exchange) {
            return Mono.just(permissions);
        }
    }
}
