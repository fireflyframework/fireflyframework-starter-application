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

import org.fireflyframework.application.context.AppContext;
import org.fireflyframework.application.resolver.DefaultContextResolver;
import org.fireflyframework.common.application.spi.SessionContext;
import org.fireflyframework.common.application.spi.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration test for SessionManager with DefaultContextResolver.
 * 
 * <p>Tests the complete flow of:</p>
 * <ol>
 *   <li>Extracting partyId from X-Party-Id header</li>
 *   <li>Calling SessionManager to get session</li>
 *   <li>Extracting roles and permissions from session</li>
 *   <li>Building AppContext with resolved data</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SessionManager Integration Tests")
class SessionManagerIntegrationTest {
    
    @Mock
    private SessionManager sessionManager;
    
    private DefaultContextResolver contextResolver;
    
    private UUID testPartyId;
    private UUID testTenantId;
    private UUID testContractId;
    private UUID testProductId;
    
    @BeforeEach
    void setUp() {
        // Create resolver with mocked SessionManager
        contextResolver = new DefaultContextResolver(sessionManager);
        
        // Test UUIDs
        testPartyId = UUID.randomUUID();
        testTenantId = UUID.randomUUID();
        testContractId = UUID.randomUUID();
        testProductId = UUID.randomUUID();
    }
    
    @Test
    @DisplayName("Should resolve roles from SessionManager for party-level context")
    void shouldResolveRolesForPartyLevel() {
        // Given: Session with multiple contracts
        SessionContext session = createSessionWithMultipleContracts();
        when(sessionManager.createOrGetSession(any(ServerWebExchange.class)))
            .thenReturn(Mono.just(session));
        
        ServerWebExchange exchange = createExchangeWithPartyHeader(testPartyId);
        
        // When: Resolve full context (no contract/product specified = party-level)
        Mono<AppContext> result = contextResolver.resolveContext(exchange, null, null);
        
        // Then: All roles from all contracts returned
        StepVerifier.create(result)
            .assertNext(context -> {
                assertThat(context.getRoles()).isNotEmpty();
                assertThat(context.getRoles()).containsExactlyInAnyOrder("owner", "account_viewer");
            })
            .verifyComplete();
        
        verify(sessionManager, times(2)).createOrGetSession(any(ServerWebExchange.class)); // Called for both roles and permissions
    }
    
    @Test
    @DisplayName("Should resolve roles from SessionManager for contract-level context")
    void shouldResolveRolesForContractLevel() {
        // Given: Session with specific contract
        SessionContext session = createSessionWithMultipleContracts();
        when(sessionManager.createOrGetSession(any(ServerWebExchange.class)))
            .thenReturn(Mono.just(session));
        
        ServerWebExchange exchange = createExchangeWithPartyHeader(testPartyId);
        
        // When: Resolve context for specific contract
        Mono<AppContext> result = contextResolver.resolveContext(exchange, testContractId, null);
        
        // Then: Only roles for that contract returned
        StepVerifier.create(result)
            .assertNext(context -> {
                assertThat(context.getRoles()).isNotEmpty();
                assertThat(context.getRoles()).contains("owner");
                assertThat(context.getContractId()).isEqualTo(testContractId);
            })
            .verifyComplete();
        
        verify(sessionManager, times(2)).createOrGetSession(any(ServerWebExchange.class)); // Called for both roles and permissions
    }
    
    @Test
    @DisplayName("Should resolve roles from SessionManager for product-level context")
    void shouldResolveRolesForProductLevel() {
        // Given: Session with specific contract+product
        SessionContext session = createSessionWithMultipleContracts();
        when(sessionManager.createOrGetSession(any(ServerWebExchange.class)))
            .thenReturn(Mono.just(session));
        
        ServerWebExchange exchange = createExchangeWithPartyHeader(testPartyId);
        
        // When: Resolve context for specific contract+product
        Mono<AppContext> result = contextResolver.resolveContext(exchange, testContractId, testProductId);
        
        // Then: Only roles for that contract+product returned
        StepVerifier.create(result)
            .assertNext(context -> {
                assertThat(context.getRoles()).isNotEmpty();
                assertThat(context.getRoles()).contains("owner");
                assertThat(context.getContractId()).isEqualTo(testContractId);
                assertThat(context.getProductId()).isEqualTo(testProductId);
            })
            .verifyComplete();
        
        verify(sessionManager, times(2)).createOrGetSession(any(ServerWebExchange.class)); // Called for both roles and permissions
    }
    
    @Test
    @DisplayName("Should resolve permissions from SessionManager")
    void shouldResolvePermissionsFromSessionManager() {
        // Given: Session with role scopes (permissions)
        SessionContext session = createSessionWithPermissions();
        when(sessionManager.createOrGetSession(any(ServerWebExchange.class)))
            .thenReturn(Mono.just(session));
        
        ServerWebExchange exchange = createExchangeWithPartyHeader(testPartyId);
        
        // When: Resolve context with contract+product
        Mono<AppContext> result = contextResolver.resolveContext(exchange, testContractId, testProductId);
        
        // Then: Permissions in format {roleCode}:{actionType}:{resourceType}
        StepVerifier.create(result)
            .assertNext(context -> {
                assertThat(context.getPermissions()).isNotEmpty();
                assertThat(context.getPermissions()).contains(
                    "owner:READ:BALANCE",
                    "owner:WRITE:TRANSACTION",
                    "owner:READ:TRANSACTION"
                );
            })
            .verifyComplete();
        
        verify(sessionManager, times(2)).createOrGetSession(any(ServerWebExchange.class)); // Called for both roles and permissions
    }
    
    @Test
    @DisplayName("Should handle gracefully when SessionManager is unavailable")
    void shouldHandleGracefullyWhenSessionManagerUnavailable() {
        // Given: Resolver with null sessionManager (simulating unavailability)
        DefaultContextResolver resolverWithoutSessionManager = new DefaultContextResolver(null);
        
        ServerWebExchange exchange = createExchangeWithPartyHeader(testPartyId);
        
        // When: Try to resolve context
        Mono<AppContext> result = resolverWithoutSessionManager.resolveContext(exchange, null, null);
        
        // Then: Returns context with empty roles and permissions (graceful degradation)
        StepVerifier.create(result)
            .assertNext(context -> {
                assertThat(context.getRoles()).isEmpty();
                assertThat(context.getPermissions()).isEmpty();
            })
            .verifyComplete();
    }
    
    @Test
    @DisplayName("Should handle errors from SessionManager gracefully")
    void shouldHandleSessionManagerErrors() {
        // Given: SessionManager throws error
        when(sessionManager.createOrGetSession(any(ServerWebExchange.class)))
            .thenReturn(Mono.error(new RuntimeException("Security Center unavailable")));
        
        ServerWebExchange exchange = createExchangeWithPartyHeader(testPartyId);
        
        // When: Resolve context
        Mono<AppContext> result = contextResolver.resolveContext(exchange, null, null);
        
        // Then: Returns context with empty roles and permissions (graceful degradation)
        StepVerifier.create(result)
            .assertNext(context -> {
                assertThat(context.getRoles()).isEmpty();
                assertThat(context.getPermissions()).isEmpty();
            })
            .verifyComplete();
        
        verify(sessionManager, times(2)).createOrGetSession(any(ServerWebExchange.class)); // Called for both roles and permissions
    }
    
    @Test
    @DisplayName("Should extract multiple permissions from multiple role scopes")
    void shouldExtractMultiplePermissionsFromMultipleScopes() {
        // Given: Session with multiple role scopes
        SessionContext session = createSessionWithMultipleScopes();
        when(sessionManager.createOrGetSession(any(ServerWebExchange.class)))
            .thenReturn(Mono.just(session));
        
        ServerWebExchange exchange = createExchangeWithPartyHeader(testPartyId);
        
        // When: Resolve context
        Mono<AppContext> result = contextResolver.resolveContext(exchange, testContractId, testProductId);
        
        // Then: All permissions extracted
        StepVerifier.create(result)
            .assertNext(context -> {
                assertThat(context.getPermissions()).hasSize(5);
                assertThat(context.getPermissions()).contains(
                    "owner:READ:BALANCE",
                    "owner:WRITE:TRANSACTION",
                    "owner:READ:TRANSACTION",
                    "owner:DELETE:TRANSACTION",
                    "owner:EXECUTE:TRANSFER"
                );
            })
            .verifyComplete();
    }
    
    @Test
    @DisplayName("Should filter inactive contracts when resolving roles")
    void shouldFilterInactiveContracts() {
        // Given: Session with active and inactive contracts
        SessionContext session = createSessionWithInactiveContracts();
        when(sessionManager.createOrGetSession(any(ServerWebExchange.class)))
            .thenReturn(Mono.just(session));
        
        ServerWebExchange exchange = createExchangeWithPartyHeader(testPartyId);
        
        // When: Resolve context
        Mono<AppContext> result = contextResolver.resolveContext(exchange, null, null);
        
        // Then: Only roles from active contracts
        StepVerifier.create(result)
            .assertNext(context -> {
                assertThat(context.getRoles()).containsExactly("owner");
                assertThat(context.getRoles()).doesNotContain("inactive_role");
            })
            .verifyComplete();
    }
    
    @Test
    @DisplayName("Should filter inactive role scopes when resolving permissions")
    void shouldFilterInactiveRoleScopes() {
        // Given: Session with active and inactive role scopes
        SessionContext session = createSessionWithInactiveScopes();
        when(sessionManager.createOrGetSession(any(ServerWebExchange.class)))
            .thenReturn(Mono.just(session));
        
        ServerWebExchange exchange = createExchangeWithPartyHeader(testPartyId);
        
        // When: Resolve context
        Mono<AppContext> result = contextResolver.resolveContext(exchange, testContractId, testProductId);
        
        // Then: Only active permissions
        StepVerifier.create(result)
            .assertNext(context -> {
                assertThat(context.getPermissions()).contains("owner:READ:BALANCE");
                assertThat(context.getPermissions()).doesNotContain("owner:DELETE:ACCOUNT");
            })
            .verifyComplete();
    }
    
    // ======================== Helper Methods ========================
    
    private ServerWebExchange createExchangeWithPartyHeader(UUID partyId) {
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/test")
            .header("X-Party-Id", partyId.toString())
            .header("X-Tenant-Id", testTenantId.toString())  // Add tenant header to bypass resolution
            .build();
        
        return MockServerWebExchange.from(request);
    }
    
    private SessionContext createSessionWithMultipleContracts() {
        return SessionContext.builder()
            .sessionId(UUID.randomUUID().toString())
            .partyId(testPartyId)
            .activeContracts(List.of(
                createContractWithRole(testContractId, testProductId, "owner"),
                createContractWithRole(UUID.randomUUID(), UUID.randomUUID(), "account_viewer")
            ))
            .createdAt(LocalDateTime.now())
            .status(SessionContext.SessionStatus.ACTIVE)
            .build();
    }
    
    private SessionContext createSessionWithPermissions() {
        RoleScopeInfoDTO readBalance = RoleScopeInfoDTO.builder()
            .scopeId(UUID.randomUUID())
            .actionType("READ")
            .resourceType("BALANCE")
            .isActive(true)
            .build();
        
        RoleScopeInfoDTO writeTransaction = RoleScopeInfoDTO.builder()
            .scopeId(UUID.randomUUID())
            .actionType("WRITE")
            .resourceType("TRANSACTION")
            .isActive(true)
            .build();
        
        RoleScopeInfoDTO readTransaction = RoleScopeInfoDTO.builder()
            .scopeId(UUID.randomUUID())
            .actionType("READ")
            .resourceType("TRANSACTION")
            .isActive(true)
            .build();
        
        RoleInfoDTO role = RoleInfoDTO.builder()
            .roleId(UUID.randomUUID())
            .roleCode("owner")
            .name("Account Owner")
            .isActive(true)
            .scopes(List.of(readBalance, writeTransaction, readTransaction))
            .build();
        
        ContractInfoDTO contract = ContractInfoDTO.builder()
            .contractId(testContractId)
            .contractNumber("CNT-001")
            .roleInContract(role)
            .product(ProductInfoDTO.builder()
                .productId(testProductId)
                .productName("Checking Account")
                .build())
            .isActive(true)
            .build();
        
        return SessionContext.builder()
            .sessionId(UUID.randomUUID().toString())
            .partyId(testPartyId)
            .activeContracts(List.of(contract))
            .createdAt(LocalDateTime.now())
            .status(SessionContext.SessionStatus.ACTIVE)
            .build();
    }
    
    private SessionContext createSessionWithMultipleScopes() {
        List<RoleScopeInfoDTO> scopes = List.of(
            createScope("READ", "BALANCE"),
            createScope("WRITE", "TRANSACTION"),
            createScope("READ", "TRANSACTION"),
            createScope("DELETE", "TRANSACTION"),
            createScope("EXECUTE", "TRANSFER")
        );
        
        RoleInfoDTO role = RoleInfoDTO.builder()
            .roleId(UUID.randomUUID())
            .roleCode("owner")
            .name("Account Owner")
            .isActive(true)
            .scopes(scopes)
            .build();
        
        ContractInfoDTO contract = ContractInfoDTO.builder()
            .contractId(testContractId)
            .contractNumber("CNT-001")
            .roleInContract(role)
            .product(ProductInfoDTO.builder()
                .productId(testProductId)
                .productName("Checking Account")
                .build())
            .isActive(true)
            .build();
        
        return SessionContext.builder()
            .sessionId(UUID.randomUUID().toString())
            .partyId(testPartyId)
            .activeContracts(List.of(contract))
            .createdAt(LocalDateTime.now())
            .status(SessionContext.SessionStatus.ACTIVE)
            .build();
    }
    
    private SessionContext createSessionWithInactiveContracts() {
        return SessionContext.builder()
            .sessionId(UUID.randomUUID().toString())
            .partyId(testPartyId)
            .activeContracts(List.of(
                createContractWithRole(testContractId, testProductId, "owner", true),
                createContractWithRole(UUID.randomUUID(), UUID.randomUUID(), "inactive_role", false)
            ))
            .createdAt(LocalDateTime.now())
            .status(SessionContext.SessionStatus.ACTIVE)
            .build();
    }
    
    private SessionContext createSessionWithInactiveScopes() {
        List<RoleScopeInfoDTO> scopes = List.of(
            createScope("READ", "BALANCE", true),
            createScope("DELETE", "ACCOUNT", false)  // Inactive
        );
        
        RoleInfoDTO role = RoleInfoDTO.builder()
            .roleId(UUID.randomUUID())
            .roleCode("owner")
            .name("Account Owner")
            .isActive(true)
            .scopes(scopes)
            .build();
        
        ContractInfoDTO contract = ContractInfoDTO.builder()
            .contractId(testContractId)
            .contractNumber("CNT-001")
            .roleInContract(role)
            .product(ProductInfoDTO.builder()
                .productId(testProductId)
                .productName("Checking Account")
                .build())
            .isActive(true)
            .build();
        
        return SessionContext.builder()
            .sessionId(UUID.randomUUID().toString())
            .partyId(testPartyId)
            .activeContracts(List.of(contract))
            .createdAt(LocalDateTime.now())
            .status(SessionContext.SessionStatus.ACTIVE)
            .build();
    }
    
    private ContractInfoDTO createContractWithRole(UUID contractId, UUID productId, String roleCode) {
        return createContractWithRole(contractId, productId, roleCode, true);
    }
    
    private ContractInfoDTO createContractWithRole(UUID contractId, UUID productId, String roleCode, boolean isActive) {
        RoleInfoDTO role = RoleInfoDTO.builder()
            .roleId(UUID.randomUUID())
            .roleCode(roleCode)
            .name(roleCode + " Role")
            .isActive(true)
            .scopes(List.of())
            .build();
        
        return ContractInfoDTO.builder()
            .contractId(contractId)
            .contractNumber("CNT-" + contractId.toString().substring(0, 8))
            .roleInContract(role)
            .product(ProductInfoDTO.builder()
                .productId(productId)
                .productName("Product " + productId.toString().substring(0, 8))
                .build())
            .isActive(isActive)
            .build();
    }
    
    private RoleScopeInfoDTO createScope(String actionType, String resourceType) {
        return createScope(actionType, resourceType, true);
    }
    
    private RoleScopeInfoDTO createScope(String actionType, String resourceType, boolean isActive) {
        return RoleScopeInfoDTO.builder()
            .scopeId(UUID.randomUUID())
            .actionType(actionType)
            .resourceType(resourceType)
            .isActive(isActive)
            .build();
    }
}
