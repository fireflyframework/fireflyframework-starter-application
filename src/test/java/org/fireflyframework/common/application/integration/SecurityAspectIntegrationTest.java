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

import org.fireflyframework.application.aop.SecurityAspect;
import org.fireflyframework.application.context.AppContext;
import org.fireflyframework.application.context.AppSecurityContext;
import org.fireflyframework.application.context.ApplicationExecutionContext;
import org.fireflyframework.application.security.EndpointSecurityRegistry;
import org.fireflyframework.application.security.SecurityAuthorizationService;
import org.fireflyframework.application.security.annotation.Secure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.security.access.AccessDeniedException;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests for {@link SecurityAspect}.
 * Tests AOP interception of @Secure annotations.
 */
@DisplayName("SecurityAspect Integration Tests")
class SecurityAspectIntegrationTest {
    
    private SecurityAuthorizationService authorizationService;
    private EndpointSecurityRegistry endpointSecurityRegistry;
    private SecurityAspect securityAspect;
    private TestService testService;
    private TestService proxiedService;
    
    @BeforeEach
    void setUp() {
        authorizationService = mock(SecurityAuthorizationService.class);
        endpointSecurityRegistry = new EndpointSecurityRegistry();
        securityAspect = new SecurityAspect(authorizationService, endpointSecurityRegistry);
        
        testService = new TestService();
        
        // Create AOP proxy
        AspectJProxyFactory factory = new AspectJProxyFactory(testService);
        factory.addAspect(securityAspect);
        proxiedService = factory.getProxy();
    }
    
    @Test
    @DisplayName("Should allow access when authorization is granted")
    void shouldAllowAccessWhenAuthorized() {
        // Given
        ApplicationExecutionContext context = createExecutionContext();
        
        when(authorizationService.authorize(any(AppContext.class), any(AppSecurityContext.class)))
                .thenReturn(Mono.just(AppSecurityContext.builder()
                        .endpoint("/test")
                        .httpMethod("GET")
                        .authorized(true)
                        .build()));
        
        // When
        String result = proxiedService.secureMethod(context);
        
        // Then
        assertThat(result).isEqualTo("success");
    }
    
    @Test
    @DisplayName("Should deny access when authorization is denied")
    void shouldDenyAccessWhenUnauthorized() {
        // Given
        ApplicationExecutionContext context = createExecutionContext();
        
        when(authorizationService.authorize(any(AppContext.class), any(AppSecurityContext.class)))
                .thenReturn(Mono.just(AppSecurityContext.builder()
                        .endpoint("/test")
                        .httpMethod("GET")
                        .authorized(false)
                        .authorizationFailureReason("Missing required role")
                        .build()));
        
        // When/Then
        assertThatThrownBy(() -> proxiedService.secureMethod(context))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Missing required role");
    }
    
    @Test
    @DisplayName("Should intercept method with @Secure annotation")
    void shouldInterceptSecureAnnotation() {
        // Given
        ApplicationExecutionContext context = createExecutionContext();
        
        when(authorizationService.authorize(any(AppContext.class), any(AppSecurityContext.class)))
                .thenReturn(Mono.just(AppSecurityContext.builder()
                        .endpoint("/test")
                        .httpMethod("GET")
                        .authorized(true)
                        .build()));
        
        // When
        String result = proxiedService.methodWithRoles(context);
        
        // Then
        assertThat(result).isEqualTo("success-with-roles");
    }
    
    @Test
    @DisplayName("Should check roles specified in @Secure annotation")
    void shouldCheckRolesFromAnnotation() {
        // Given
        ApplicationExecutionContext context = createExecutionContext();
        
        when(authorizationService.authorize(any(AppContext.class), any(AppSecurityContext.class)))
                .thenAnswer(invocation -> {
                    AppSecurityContext secContext = invocation.getArgument(1);
                    
                    // Verify that the aspect extracted roles from annotation
                    assertThat(secContext.getRequiredRoles()).containsExactly("ADMIN");
                    assertThat(secContext.getConfigSource())
                            .isEqualTo(AppSecurityContext.SecurityConfigSource.ANNOTATION);
                    
                    // Simulate authorization success since user has ADMIN role
                    return Mono.just(secContext.toBuilder()
                            .authorized(true)
                            .build());
                });
        
        // When
        String result = proxiedService.methodWithRoles(context);
        
        // Then
        assertThat(result).isEqualTo("success-with-roles");
    }
    
    @Test
    @DisplayName("Should check permissions specified in @Secure annotation")
    void shouldCheckPermissionsFromAnnotation() {
        // Given
        ApplicationExecutionContext context = createExecutionContext();
        
        when(authorizationService.authorize(any(AppContext.class), any(AppSecurityContext.class)))
                .thenAnswer(invocation -> {
                    AppSecurityContext secContext = invocation.getArgument(1);
                    
                    // Verify that the aspect extracted permissions from annotation
                    assertThat(secContext.getRequiredPermissions()).containsExactly("WRITE");
                    assertThat(secContext.getConfigSource())
                            .isEqualTo(AppSecurityContext.SecurityConfigSource.ANNOTATION);
                    
                    // Simulate authorization success since user has WRITE permission
                    return Mono.just(secContext.toBuilder()
                            .authorized(true)
                            .build());
                });
        
        // When
        String result = proxiedService.methodWithPermissions(context);
        
        // Then
        assertThat(result).isEqualTo("success-with-permissions");
    }
    
    @Test
    @DisplayName("Should skip security check when no execution context is provided")
    void shouldSkipSecurityCheckWithoutExecutionContext() {
        // When - Call without ExecutionContext
        String result = proxiedService.methodWithoutContext();
        
        // Then - Should execute without security check
        assertThat(result).isEqualTo("no-context");
    }
    
    @Test
    @DisplayName("Should check both roles and permissions from annotation")
    void shouldCheckRolesAndPermissionsFromAnnotation() {
        // Given
        ApplicationExecutionContext context = createExecutionContext();
        
        when(authorizationService.authorize(any(AppContext.class), any(AppSecurityContext.class)))
                .thenAnswer(invocation -> {
                    AppSecurityContext secContext = invocation.getArgument(1);
                    
                    // Verify that the aspect extracted both roles and permissions
                    assertThat(secContext.getRequiredRoles()).containsExactly("ADMIN");
                    assertThat(secContext.getRequiredPermissions()).containsExactly("WRITE");
                    assertThat(secContext.getConfigSource())
                            .isEqualTo(AppSecurityContext.SecurityConfigSource.ANNOTATION);
                    
                    // Simulate authorization success
                    return Mono.just(secContext.toBuilder()
                            .authorized(true)
                            .build());
                });
        
        // When
        String result = proxiedService.methodWithRolesAndPermissions(context);
        
        // Then
        assertThat(result).isEqualTo("success-with-both");
    }
    
    @Test
    @DisplayName("Should deny access when user lacks required roles")
    void shouldDenyAccessWhenUserLacksRoles() {
        // Given - Context with user that has no ADMIN role
        ApplicationExecutionContext context = ApplicationExecutionContext.builder()
                .context(AppContext.builder()
                        .partyId(UUID.randomUUID())
                        .tenantId(UUID.randomUUID())
                        .roles(Set.of("USER"))  // Only USER role, not ADMIN
                        .permissions(Set.of("READ"))
                        .build())
                .build();
        
        when(authorizationService.authorize(any(AppContext.class), any(AppSecurityContext.class)))
                .thenAnswer(invocation -> {
                    AppSecurityContext secContext = invocation.getArgument(1);
                    
                    // Simulate authorization service checking and denying
                    return Mono.just(secContext.toBuilder()
                            .authorized(false)
                            .authorizationFailureReason("User does not have required ADMIN role")
                            .build());
                });
        
        // When/Then
        assertThatThrownBy(() -> proxiedService.methodWithRoles(context))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("User does not have required ADMIN role");
    }
    
    @Test
    @DisplayName("Should pass AppContext to authorization service")
    void shouldPassAppContextToAuthorizationService() {
        // Given
        UUID partyId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        ApplicationExecutionContext context = ApplicationExecutionContext.builder()
                .context(AppContext.builder()
                        .partyId(partyId)
                        .tenantId(tenantId)
                        .roles(Set.of("ADMIN"))
                        .permissions(Set.of("WRITE"))
                        .build())
                .build();
        
        when(authorizationService.authorize(any(AppContext.class), any(AppSecurityContext.class)))
                .thenAnswer(invocation -> {
                    AppContext appContext = invocation.getArgument(0);
                    
                    // Verify that the aspect passed the correct AppContext
                    assertThat(appContext.getPartyId()).isEqualTo(partyId);
                    assertThat(appContext.getTenantId()).isEqualTo(tenantId);
                    assertThat(appContext.getRoles()).contains("ADMIN");
                    assertThat(appContext.getPermissions()).contains("WRITE");
                    
                    AppSecurityContext secContext = invocation.getArgument(1);
                    return Mono.just(secContext.toBuilder()
                            .authorized(true)
                            .build());
                });
        
        // When
        String result = proxiedService.secureMethod(context);
        
        // Then
        assertThat(result).isEqualTo("success");
    }
    
    private ApplicationExecutionContext createExecutionContext() {
        return ApplicationExecutionContext.builder()
                .context(AppContext.builder()
                        .partyId(UUID.randomUUID())
                        .tenantId(UUID.randomUUID())
                        .roles(Set.of("ADMIN", "USER"))
                        .permissions(Set.of("READ", "WRITE"))
                        .build())
                .build();
    }
    
    /**
     * Test service with @Secure annotations.
     */
    static class TestService {
        
        @Secure
        public String secureMethod(ApplicationExecutionContext context) {
            return "success";
        }
        
        @Secure(roles = {"ADMIN"})
        public String methodWithRoles(ApplicationExecutionContext context) {
            return "success-with-roles";
        }
        
        @Secure(permissions = {"WRITE"})
        public String methodWithPermissions(ApplicationExecutionContext context) {
            return "success-with-permissions";
        }
        
        @Secure(roles = {"ADMIN"}, permissions = {"WRITE"})
        public String methodWithRolesAndPermissions(ApplicationExecutionContext context) {
            return "success-with-both";
        }
        
        @Secure
        public String methodWithoutContext() {
            return "no-context";
        }
    }
}
