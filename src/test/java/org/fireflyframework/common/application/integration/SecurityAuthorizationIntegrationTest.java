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

import org.fireflyframework.common.application.context.AppContext;
import org.fireflyframework.common.application.context.AppSecurityContext;
import org.fireflyframework.common.application.security.DefaultSecurityAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Set;
import java.util.UUID;

/**
 * Integration tests for {@link DefaultSecurityAuthorizationService}.
 *
 * <p>Authorization is decided <strong>solely</strong> from the roles and permissions already
 * resolved onto the product-agnostic {@link AppContext}. There is no Security Center / session
 * dependency and no contract/product scoping.</p>
 *
 * <ol>
 *   <li>Role-based checks against {@link AppContext#getRoles()}</li>
 *   <li>Permission-based checks against {@link AppContext#getPermissions()}</li>
 *   <li>Endpoint authorization via {@link AppSecurityContext} required roles/permissions</li>
 * </ol>
 */
@DisplayName("Security Authorization Integration Tests")
class SecurityAuthorizationIntegrationTest {

    private DefaultSecurityAuthorizationService authorizationService;

    private String testSubject;
    private UUID testTenantId;

    @BeforeEach
    void setUp() {
        authorizationService = new DefaultSecurityAuthorizationService();

        testSubject = "user-" + UUID.randomUUID();
        testTenantId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should use hasRole from AppContext")
    void shouldUseHasRoleFromAppContext() {
        // Given
        AppContext context = AppContext.builder()
            .subject(testSubject)
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
            .subject(testSubject)
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

    @Test
    @DisplayName("Should authorize endpoint when required role is present in AppContext")
    void shouldAuthorizeWhenRequiredRolePresent() {
        // Given
        AppContext context = AppContext.builder()
            .subject(testSubject)
            .tenantId(testTenantId)
            .roles(Set.of("owner", "account_viewer"))
            .permissions(Set.of())
            .build();

        AppSecurityContext securityContext = AppSecurityContext.builder()
            .endpoint("/api/v1/accounts")
            .httpMethod("GET")
            .requiredRoles(Set.of("owner"))
            .configSource(AppSecurityContext.SecurityConfigSource.ANNOTATION)
            .build();

        // When/Then
        StepVerifier.create(authorizationService.authorize(context, securityContext))
            .assertNext(result -> {
                org.assertj.core.api.Assertions.assertThat(result.isAuthorized()).isTrue();
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should deny endpoint when required role is missing from AppContext")
    void shouldDenyWhenRequiredRoleMissing() {
        // Given
        AppContext context = AppContext.builder()
            .subject(testSubject)
            .tenantId(testTenantId)
            .roles(Set.of("account_viewer"))
            .permissions(Set.of())
            .build();

        AppSecurityContext securityContext = AppSecurityContext.builder()
            .endpoint("/api/v1/accounts")
            .httpMethod("DELETE")
            .requiredRoles(Set.of("admin"))
            .configSource(AppSecurityContext.SecurityConfigSource.ANNOTATION)
            .build();

        // When/Then
        StepVerifier.create(authorizationService.authorize(context, securityContext))
            .assertNext(result -> {
                org.assertj.core.api.Assertions.assertThat(result.isAuthorized()).isFalse();
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should authorize endpoint when required permission is present in AppContext")
    void shouldAuthorizeWhenRequiredPermissionPresent() {
        // Given
        AppContext context = AppContext.builder()
            .subject(testSubject)
            .tenantId(testTenantId)
            .roles(Set.of("owner"))
            .permissions(Set.of("owner:READ:BALANCE"))
            .build();

        AppSecurityContext securityContext = AppSecurityContext.builder()
            .endpoint("/api/v1/balances")
            .httpMethod("GET")
            .requiredPermissions(Set.of("owner:READ:BALANCE"))
            .configSource(AppSecurityContext.SecurityConfigSource.ANNOTATION)
            .build();

        // When/Then
        StepVerifier.create(authorizationService.authorize(context, securityContext))
            .assertNext(result -> {
                org.assertj.core.api.Assertions.assertThat(result.isAuthorized()).isTrue();
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should authorize endpoint when no role or permission requirements are declared")
    void shouldAuthorizeWhenNoRequirements() {
        // Given
        AppContext context = AppContext.builder()
            .subject(testSubject)
            .tenantId(testTenantId)
            .roles(Set.of())
            .permissions(Set.of())
            .build();

        AppSecurityContext securityContext = AppSecurityContext.builder()
            .endpoint("/api/v1/public")
            .httpMethod("GET")
            .configSource(AppSecurityContext.SecurityConfigSource.ANNOTATION)
            .build();

        // When/Then
        StepVerifier.create(authorizationService.authorize(context, securityContext))
            .assertNext(result -> {
                org.assertj.core.api.Assertions.assertThat(result.isAuthorized()).isTrue();
            })
            .verifyComplete();
    }
}
