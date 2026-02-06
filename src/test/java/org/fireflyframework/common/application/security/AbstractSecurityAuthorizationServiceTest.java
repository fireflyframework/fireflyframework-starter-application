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

package org.fireflyframework.application.security;

import org.fireflyframework.application.context.AppContext;
import org.fireflyframework.application.context.AppSecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AbstractSecurityAuthorizationService}.
 */
@DisplayName("AbstractSecurityAuthorizationService Tests")
class AbstractSecurityAuthorizationServiceTest {
    
    private TestSecurityAuthorizationService authorizationService;
    private AppContext context;
    
    @BeforeEach
    void setUp() {
        authorizationService = new TestSecurityAuthorizationService();
        context = AppContext.builder()
                .partyId(UUID.randomUUID())
                .tenantId(UUID.randomUUID())
                .contractId(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .roles(Set.of("ROLE_USER", "ROLE_ADMIN"))
                .permissions(Set.of("READ", "WRITE"))
                .build();
    }
    
    @Test
    @DisplayName("Should authorize anonymous access when allowed")
    void shouldAuthorizeAnonymousAccess() {
        // Given
        AppSecurityContext securityContext = AppSecurityContext.builder()
                .endpoint("/public")
                .httpMethod("GET")
                .allowAnonymous(true)
                .build();
        
        // When/Then
        StepVerifier.create(authorizationService.authorize(context, securityContext))
                .assertNext(result -> {
                    assertThat(result.isAuthorized()).isTrue();
                    assertThat(result.getAuthorizationFailureReason()).isNull();
                })
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should authorize when required role is present")
    void shouldAuthorizeWhenRolePresent() {
        // Given
        AppSecurityContext securityContext = AppSecurityContext.builder()
                .endpoint("/admin")
                .httpMethod("GET")
                .requiredRoles(Set.of("ROLE_ADMIN"))
                .build();
        
        // When/Then
        StepVerifier.create(authorizationService.authorize(context, securityContext))
                .assertNext(result -> {
                    assertThat(result.isAuthorized()).isTrue();
                    assertThat(result.getAuthorizationFailureReason()).isNull();
                })
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should deny when required role is missing")
    void shouldDenyWhenRoleMissing() {
        // Given
        AppSecurityContext securityContext = AppSecurityContext.builder()
                .endpoint("/superadmin")
                .httpMethod("GET")
                .requiredRoles(Set.of("ROLE_SUPERADMIN"))
                .build();
        
        // When/Then
        StepVerifier.create(authorizationService.authorize(context, securityContext))
                .assertNext(result -> {
                    assertThat(result.isAuthorized()).isFalse();
                    assertThat(result.getAuthorizationFailureReason()).isEqualTo("Required roles not present");
                })
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should authorize when required permission is granted")
    void shouldAuthorizeWhenPermissionGranted() {
        // Given
        AppSecurityContext securityContext = AppSecurityContext.builder()
                .endpoint("/data")
                .httpMethod("GET")
                .requiredPermissions(Set.of("READ"))
                .build();
        
        // When/Then
        StepVerifier.create(authorizationService.authorize(context, securityContext))
                .assertNext(result -> {
                    assertThat(result.isAuthorized()).isTrue();
                    assertThat(result.getAuthorizationFailureReason()).isNull();
                })
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should deny when required permission is missing")
    void shouldDenyWhenPermissionMissing() {
        // Given
        AppSecurityContext securityContext = AppSecurityContext.builder()
                .endpoint("/delete")
                .httpMethod("DELETE")
                .requiredPermissions(Set.of("DELETE"))
                .build();
        
        // When/Then
        StepVerifier.create(authorizationService.authorize(context, securityContext))
                .assertNext(result -> {
                    assertThat(result.isAuthorized()).isFalse();
                    assertThat(result.getAuthorizationFailureReason()).isEqualTo("Required permissions not granted");
                })
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should check both roles and permissions when required")
    void shouldCheckBothRolesAndPermissions() {
        // Given
        AppSecurityContext securityContext = AppSecurityContext.builder()
                .endpoint("/admin/write")
                .httpMethod("POST")
                .requiredRoles(Set.of("ROLE_ADMIN"))
                .requiredPermissions(Set.of("WRITE"))
                .build();
        
        // When/Then
        StepVerifier.create(authorizationService.authorize(context, securityContext))
                .assertNext(result -> assertThat(result.isAuthorized()).isTrue())
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should deny when role is present but permission is missing")
    void shouldDenyWhenRolePresentButPermissionMissing() {
        // Given
        AppSecurityContext securityContext = AppSecurityContext.builder()
                .endpoint("/admin/delete")
                .httpMethod("DELETE")
                .requiredRoles(Set.of("ROLE_ADMIN"))
                .requiredPermissions(Set.of("DELETE"))
                .build();
        
        // When/Then
        StepVerifier.create(authorizationService.authorize(context, securityContext))
                .assertNext(result -> {
                    assertThat(result.isAuthorized()).isFalse();
                    assertThat(result.getAuthorizationFailureReason()).isEqualTo("Required permissions not granted");
                })
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should check if party has specific role")
    void shouldCheckHasRole() {
        // When/Then
        StepVerifier.create(authorizationService.hasRole(context, "ROLE_ADMIN"))
                .expectNext(true)
                .verifyComplete();
        
        StepVerifier.create(authorizationService.hasRole(context, "ROLE_SUPERADMIN"))
                .expectNext(false)
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should check if party has specific permission")
    void shouldCheckHasPermission() {
        // When/Then
        StepVerifier.create(authorizationService.hasPermission(context, "READ"))
                .expectNext(true)
                .verifyComplete();
        
        StepVerifier.create(authorizationService.hasPermission(context, "DELETE"))
                .expectNext(false)
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should allow access when no specific requirements")
    void shouldAllowAccessWithNoRequirements() {
        // Given
        AppSecurityContext securityContext = AppSecurityContext.builder()
                .endpoint("/open")
                .httpMethod("GET")
                .build();
        
        // When/Then
        StepVerifier.create(authorizationService.authorize(context, securityContext))
                .assertNext(result -> assertThat(result.isAuthorized()).isTrue())
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should evaluate expression and return false by default")
    void shouldEvaluateExpression() {
        // When/Then
        StepVerifier.create(authorizationService.evaluateExpression(context, "hasRole('ADMIN')"))
                .expectNext(false)
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should deny with SecurityCenter when config source is SECURITY_CENTER")
    void shouldDenyWithSecurityCenter() {
        // Given
        AppSecurityContext securityContext = AppSecurityContext.builder()
                .endpoint("/protected")
                .httpMethod("GET")
                .configSource(AppSecurityContext.SecurityConfigSource.SECURITY_CENTER)
                .build();
        
        // When/Then
        StepVerifier.create(authorizationService.authorize(context, securityContext))
                .assertNext(result -> {
                    assertThat(result.isAuthorized()).isFalse();
                    assertThat(result.getAuthorizationFailureReason())
                            .isEqualTo("SecurityCenter integration not implemented");
                })
                .verifyComplete();
    }
    
    /**
     * Test implementation of AbstractSecurityAuthorizationService.
     */
    private static class TestSecurityAuthorizationService extends AbstractSecurityAuthorizationService {
        // Uses default implementation from abstract class
    }
}
