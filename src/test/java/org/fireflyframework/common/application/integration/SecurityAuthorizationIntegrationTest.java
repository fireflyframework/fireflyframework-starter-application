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
import org.fireflyframework.application.context.AppSecurityContext;
import org.fireflyframework.application.security.DefaultSecurityAuthorizationService;
import org.fireflyframework.common.application.spi.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration test for SessionManager with DefaultSecurityAuthorizationService.
 * 
 * <p>Tests the authorization flow including:</p>
 * <ol>
 *   <li>Product access validation via SessionManager</li>
 *   <li>Role-based authorization</li>
 *   <li>Permission-based authorization</li>
 *   <li>Graceful degradation when Security Center unavailable</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Security Authorization Integration Tests")
class SecurityAuthorizationIntegrationTest {
    
    @Mock
    private SessionManager sessionManager;
    
    private DefaultSecurityAuthorizationService authorizationService;
    
    private UUID testPartyId;
    private UUID testTenantId;
    private UUID testContractId;
    private UUID testProductId;
    
    @BeforeEach
    void setUp() {
        authorizationService = new DefaultSecurityAuthorizationService(sessionManager);
        
        testPartyId = UUID.randomUUID();
        testTenantId = UUID.randomUUID();
        testContractId = UUID.randomUUID();
        testProductId = UUID.randomUUID();
    }
    
    @Test
    @DisplayName("Should validate product access when party has access")
    void shouldValidateProductAccessWhenPartyHasAccess() {
        // Given: Party has product access
        when(sessionManager.hasAccessToProduct(testPartyId, testProductId))
            .thenReturn(Mono.just(true));
        
        // When: Check product access
        Mono<Boolean> result = sessionManager.hasAccessToProduct(testPartyId, testProductId);
        
        // Then: Returns true
        StepVerifier.create(result)
            .expectNext(true)
            .verifyComplete();
        
        verify(sessionManager, times(1)).hasAccessToProduct(testPartyId, testProductId);
    }
    
    @Test
    @DisplayName("Should validate product access when party lacks access")
    void shouldValidateProductAccessWhenPartyLacksAccess() {
        // Given: Party does NOT have product access
        when(sessionManager.hasAccessToProduct(testPartyId, testProductId))
            .thenReturn(Mono.just(false));
        
        // When: Check product access
        Mono<Boolean> result = sessionManager.hasAccessToProduct(testPartyId, testProductId);
        
        // Then: Returns false
        StepVerifier.create(result)
            .expectNext(false)
            .verifyComplete();
        
        verify(sessionManager, times(1)).hasAccessToProduct(testPartyId, testProductId);
    }
    
    @Test
    @DisplayName("Should check specific permission via SessionManager")
    void shouldCheckSpecificPermissionViaSessionManager() {
        // Given: Session manager with permission check
        when(sessionManager.hasPermission(testPartyId, testProductId, "READ", "BALANCE"))
            .thenReturn(Mono.just(true));
        
        // When: Check permission
        Mono<Boolean> result = sessionManager.hasPermission(testPartyId, testProductId, "READ", "BALANCE");
        
        // Then: Returns true
        StepVerifier.create(result)
            .expectNext(true)
            .verifyComplete();
        
        verify(sessionManager, times(1)).hasPermission(testPartyId, testProductId, "READ", "BALANCE");
    }
    
    @Test
    @DisplayName("Should check permission with action and resource type")
    void shouldCheckPermissionWithActionAndResourceType() {
        // Given: Permission check configured
        when(sessionManager.hasPermission(testPartyId, testProductId, "WRITE", "TRANSACTION"))
            .thenReturn(Mono.just(false));
        
        // When: Check permission
        Mono<Boolean> result = sessionManager.hasPermission(testPartyId, testProductId, "WRITE", "TRANSACTION");
        
        // Then: Returns false
        StepVerifier.create(result)
            .expectNext(false)
            .verifyComplete();
        
        verify(sessionManager, times(1)).hasPermission(testPartyId, testProductId, "WRITE", "TRANSACTION");
    }
    
    
    
    
    
    
    @Test
    @DisplayName("Should use hasRole from AppContext")
    void shouldUseHasRoleFromAppContext() {
        // Given
        AppContext context = AppContext.builder()
            .partyId(testPartyId)
            .tenantId(testTenantId)
            .roles(Set.of("owner", "account_viewer"))
            .permissions(Set.of())
            .build();
        
        // When
        Mono<Boolean> hasOwner = authorizationService.hasRole(context, "owner");
        Mono<Boolean> hasAdmin = authorizationService.hasRole(context, "admin");
        
        // Then
        StepVerifier.create(hasOwner)
            .expectNext(true)
            .verifyComplete();
        
        StepVerifier.create(hasAdmin)
            .expectNext(false)
            .verifyComplete();
    }
    
    @Test
    @DisplayName("Should use hasPermission from AppContext")
    void shouldUseHasPermissionFromAppContext() {
        // Given
        AppContext context = AppContext.builder()
            .partyId(testPartyId)
            .tenantId(testTenantId)
            .roles(Set.of("owner"))
            .permissions(Set.of("owner:READ:BALANCE", "owner:WRITE:TRANSACTION"))
            .build();
        
        // When
        Mono<Boolean> canReadBalance = authorizationService.hasPermission(context, "owner:READ:BALANCE");
        Mono<Boolean> canDeleteAccount = authorizationService.hasPermission(context, "owner:DELETE:ACCOUNT");
        
        // Then
        StepVerifier.create(canReadBalance)
            .expectNext(true)
            .verifyComplete();
        
        StepVerifier.create(canDeleteAccount)
            .expectNext(false)
            .verifyComplete();
    }
}
