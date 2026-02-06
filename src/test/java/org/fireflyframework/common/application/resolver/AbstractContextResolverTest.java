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

package org.fireflyframework.application.resolver;

import org.fireflyframework.application.context.AppContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AbstractContextResolver}.
 */
@DisplayName("AbstractContextResolver Tests")
class AbstractContextResolverTest {
    
    private TestContextResolver contextResolver;
    private ServerWebExchange exchange;
    private ServerHttpRequest request;
    private HttpHeaders headers;
    
    @BeforeEach
    void setUp() {
        contextResolver = new TestContextResolver();
        exchange = mock(ServerWebExchange.class);
        request = mock(ServerHttpRequest.class);
        headers = new HttpHeaders();
        
        when(exchange.getRequest()).thenReturn(request);
        when(request.getHeaders()).thenReturn(headers);
    }
    
    @Test
    @DisplayName("Should resolve context with all IDs")
    void shouldResolveContextWithAllIds() {
        // Given
        UUID partyId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        
        contextResolver.setPartyId(partyId);
        contextResolver.setTenantId(tenantId);
        contextResolver.setContractId(contractId);
        contextResolver.setProductId(productId);
        
        // When/Then
        StepVerifier.create(contextResolver.resolveContext(exchange))
                .assertNext(context -> {
                    assertThat(context.getPartyId()).isEqualTo(partyId);
                    assertThat(context.getTenantId()).isEqualTo(tenantId);
                    assertThat(context.getContractId()).isEqualTo(contractId);
                    assertThat(context.getProductId()).isEqualTo(productId);
                })
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should extract UUID from attribute")
    void shouldExtractUuidFromAttribute() {
        // Given
        UUID expectedId = UUID.randomUUID();
        when(exchange.getAttribute("testAttribute")).thenReturn(expectedId);
        
        // When/Then
        StepVerifier.create(contextResolver.extractUUID(exchange, "testAttribute", "testHeader"))
                .expectNext(expectedId)
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should extract UUID from header when attribute is missing")
    void shouldExtractUuidFromHeader() {
        // Given
        UUID expectedId = UUID.randomUUID();
        when(exchange.getAttribute("testAttribute")).thenReturn(null);
        headers.set("testHeader", expectedId.toString());
        
        // When/Then
        StepVerifier.create(contextResolver.extractUUID(exchange, "testAttribute", "testHeader"))
                .expectNext(expectedId)
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should return empty when UUID not found")
    void shouldReturnEmptyWhenUuidNotFound() {
        // Given
        when(exchange.getAttribute("testAttribute")).thenReturn(null);
        
        // When/Then
        StepVerifier.create(contextResolver.extractUUID(exchange, "testAttribute", "testHeader"))
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should return empty when header has invalid UUID format")
    void shouldReturnEmptyForInvalidUuidFormat() {
        // Given
        when(exchange.getAttribute("testAttribute")).thenReturn(null);
        headers.set("testHeader", "invalid-uuid");
        
        // When/Then
        StepVerifier.create(contextResolver.extractUUID(exchange, "testAttribute", "testHeader"))
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should resolve roles for context")
    void shouldResolveRoles() {
        // Given
        UUID partyId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        
        AppContext context = AppContext.builder()
                .partyId(partyId)
                .tenantId(tenantId)
                .contractId(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .build();
        
        // When/Then
        StepVerifier.create(contextResolver.resolveRoles(context, exchange))
                .assertNext(roles -> assertThat(roles).isEmpty())
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should resolve permissions for context")
    void shouldResolvePermissions() {
        // Given
        UUID partyId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        
        AppContext context = AppContext.builder()
                .partyId(partyId)
                .tenantId(tenantId)
                .contractId(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .build();
        
        // When/Then
        StepVerifier.create(contextResolver.resolvePermissions(context, exchange))
                .assertNext(permissions -> assertThat(permissions).isEmpty())
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should enrich context with roles and permissions")
    void shouldEnrichContext() {
        // Given
        UUID partyId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        
        AppContext basicContext = AppContext.builder()
                .partyId(partyId)
                .tenantId(tenantId)
                .contractId(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .build();
        
        Set<String> roles = Set.of("ROLE_ADMIN", "ROLE_USER");
        Set<String> permissions = Set.of("READ", "WRITE");
        
        TestContextResolver enrichedResolver = new TestContextResolver();
        enrichedResolver.setRoles(roles);
        enrichedResolver.setPermissions(permissions);
        
        // When/Then
        StepVerifier.create(enrichedResolver.enrichContext(basicContext, exchange))
                .assertNext(context -> {
                    assertThat(context.getRoles()).containsExactlyInAnyOrderElementsOf(roles);
                    assertThat(context.getPermissions()).containsExactlyInAnyOrderElementsOf(permissions);
                })
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should return empty when extracting UUID from path fails")
    void shouldReturnEmptyWhenExtractingFromPathFails() {
        // When/Then
        StepVerifier.create(contextResolver.extractUUIDFromPath(exchange, "variableName"))
                .verifyComplete();
    }
    
    /**
     * Test implementation of AbstractContextResolver.
     */
    private static class TestContextResolver extends AbstractContextResolver {
        
        private UUID partyId = UUID.randomUUID();
        private UUID tenantId = UUID.randomUUID();
        private UUID contractId;
        private UUID productId;
        private Set<String> roles = Set.of();
        private Set<String> permissions = Set.of();
        
        public void setPartyId(UUID partyId) {
            this.partyId = partyId;
        }
        
        public void setTenantId(UUID tenantId) {
            this.tenantId = tenantId;
        }
        
        public void setContractId(UUID contractId) {
            this.contractId = contractId;
        }
        
        public void setProductId(UUID productId) {
            this.productId = productId;
        }
        
        public void setRoles(Set<String> roles) {
            this.roles = roles;
        }
        
        public void setPermissions(Set<String> permissions) {
            this.permissions = permissions;
        }
        
        @Override
        public Mono<UUID> resolvePartyId(ServerWebExchange exchange) {
            return Mono.just(partyId);
        }
        
        @Override
        public Mono<UUID> resolveTenantId(ServerWebExchange exchange) {
            return Mono.just(tenantId);
        }
        
        @Override
        public Mono<UUID> resolveContractId(ServerWebExchange exchange) {
            return contractId != null ? Mono.just(contractId) : Mono.empty();
        }
        
        @Override
        public Mono<UUID> resolveProductId(ServerWebExchange exchange) {
            return productId != null ? Mono.just(productId) : Mono.empty();
        }
        
        @Override
        protected Mono<Set<String>> resolveRoles(AppContext context, ServerWebExchange exchange) {
            return Mono.just(roles);
        }
        
        @Override
        protected Mono<Set<String>> resolvePermissions(AppContext context, ServerWebExchange exchange) {
            return Mono.just(permissions);
        }
    }
}
