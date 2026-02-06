package org.fireflyframework.application.security;

import org.fireflyframework.application.security.EndpointSecurityRegistry.EndpointSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EndpointSecurityRegistry Tests")
class EndpointSecurityRegistryTest {
    
    private EndpointSecurityRegistry registry;
    
    @BeforeEach
    void setUp() {
        registry = new EndpointSecurityRegistry();
    }
    
    @Test
    @DisplayName("Should register endpoint with roles")
    void shouldRegisterEndpointWithRoles() {
        EndpointSecurity security = EndpointSecurity.builder()
            .roles(Set.of("VIEWER"))
            .build();
        
        registry.registerEndpoint("/api/accounts", "GET", security);
        
        assertTrue(registry.isRegistered("/api/accounts", "GET"));
        EndpointSecurity retrieved = registry.getEndpointSecurity("/api/accounts", "GET").orElse(null);
        assertNotNull(retrieved);
        assertEquals(Set.of("VIEWER"), retrieved.getRoles());
    }
    
    @Test
    @DisplayName("Should register endpoint with permissions")
    void shouldRegisterEndpointWithPermissions() {
        EndpointSecurity security = EndpointSecurity.builder()
            .permissions(Set.of("CREATE_ACCOUNT"))
            .build();
        
        registry.registerEndpoint("/api/accounts", "POST", security);
        
        assertTrue(registry.isRegistered("/api/accounts", "POST"));
        EndpointSecurity retrieved = registry.getEndpointSecurity("/api/accounts", "POST").orElse(null);
        assertNotNull(retrieved);
        assertEquals(Set.of("CREATE_ACCOUNT"), retrieved.getPermissions());
    }
    
    @Test
    @DisplayName("Should register endpoint with both roles and permissions")
    void shouldRegisterEndpointWithRolesAndPermissions() {
        EndpointSecurity security = EndpointSecurity.builder()
            .roles(Set.of("ADMIN"))
            .permissions(Set.of("DELETE_ACCOUNT"))
            .build();
        
        registry.registerEndpoint("/api/accounts", "DELETE", security);
        
        assertTrue(registry.isRegistered("/api/accounts", "DELETE"));
        EndpointSecurity retrieved = registry.getEndpointSecurity("/api/accounts", "DELETE").orElse(null);
        assertNotNull(retrieved);
        assertEquals(Set.of("ADMIN"), retrieved.getRoles());
        assertEquals(Set.of("DELETE_ACCOUNT"), retrieved.getPermissions());
    }
    
    @Test
    @DisplayName("Should handle endpoint not registered")
    void shouldHandleEndpointNotRegistered() {
        assertFalse(registry.isRegistered("/api/nonexistent", "GET"));
        assertTrue(registry.getEndpointSecurity("/api/nonexistent", "GET").isEmpty());
    }
    
    @Test
    @DisplayName("Should handle path parameters in endpoints")
    void shouldHandlePathParameters() {
        EndpointSecurity security = EndpointSecurity.builder()
            .roles(Set.of("VIEWER"))
            .build();
        
        registry.registerEndpoint("/api/accounts/{id}", "GET", security);
        
        assertTrue(registry.isRegistered("/api/accounts/{id}", "GET"));
    }
    
    @Test
    @DisplayName("Should differentiate between HTTP methods")
    void shouldDifferentiateBetweenMethods() {
        EndpointSecurity getSecurity = EndpointSecurity.builder()
            .roles(Set.of("VIEWER"))
            .build();
        EndpointSecurity postSecurity = EndpointSecurity.builder()
            .roles(Set.of("ADMIN"))
            .build();
        
        registry.registerEndpoint("/api/accounts", "GET", getSecurity);
        registry.registerEndpoint("/api/accounts", "POST", postSecurity);
        
        assertEquals(Set.of("VIEWER"), registry.getEndpointSecurity("/api/accounts", "GET").get().getRoles());
        assertEquals(Set.of("ADMIN"), registry.getEndpointSecurity("/api/accounts", "POST").get().getRoles());
    }
    
    @Test
    @DisplayName("Should override existing registration")
    void shouldOverrideExistingRegistration() {
        EndpointSecurity security1 = EndpointSecurity.builder()
            .roles(Set.of("VIEWER"))
            .build();
        EndpointSecurity security2 = EndpointSecurity.builder()
            .roles(Set.of("ADMIN"))
            .permissions(Set.of("VIEW_ALL"))
            .build();
        
        registry.registerEndpoint("/api/accounts", "GET", security1);
        registry.registerEndpoint("/api/accounts", "GET", security2);
        
        EndpointSecurity retrieved = registry.getEndpointSecurity("/api/accounts", "GET").orElse(null);
        assertNotNull(retrieved);
        assertEquals(Set.of("ADMIN"), retrieved.getRoles());
        assertEquals(Set.of("VIEW_ALL"), retrieved.getPermissions());
    }
    
    @Test
    @DisplayName("Should allow anonymous access")
    void shouldAllowAnonymousAccess() {
        EndpointSecurity security = EndpointSecurity.builder()
            .allowAnonymous(true)
            .requiresAuthentication(false)
            .build();
        
        registry.registerEndpoint("/api/public", "GET", security);
        
        EndpointSecurity retrieved = registry.getEndpointSecurity("/api/public", "GET").orElse(null);
        assertNotNull(retrieved);
        assertTrue(retrieved.isAllowAnonymous());
        assertFalse(retrieved.isRequiresAuthentication());
    }
    
    @Test
    @DisplayName("Should require all roles and permissions")
    void shouldRequireAllRolesAndPermissions() {
        EndpointSecurity security = EndpointSecurity.builder()
            .roles(Set.of("ROLE1", "ROLE2"))
            .permissions(Set.of("PERM1", "PERM2"))
            .requireAllRoles(true)
            .requireAllPermissions(true)
            .build();
        
        registry.registerEndpoint("/api/strict", "POST", security);
        
        EndpointSecurity retrieved = registry.getEndpointSecurity("/api/strict", "POST").orElse(null);
        assertNotNull(retrieved);
        assertTrue(retrieved.isRequireAllRoles());
        assertTrue(retrieved.isRequireAllPermissions());
    }
    
    @Test
    @DisplayName("Should clear all registrations")
    void shouldClearAllRegistrations() {
        EndpointSecurity security = EndpointSecurity.builder()
            .roles(Set.of("USER"))
            .build();
        
        registry.registerEndpoint("/api/test1", "GET", security);
        registry.registerEndpoint("/api/test2", "POST", security);
        
        assertTrue(registry.isRegistered("/api/test1", "GET"));
        assertTrue(registry.isRegistered("/api/test2", "POST"));
        
        registry.clear();
        
        assertFalse(registry.isRegistered("/api/test1", "GET"));
        assertFalse(registry.isRegistered("/api/test2", "POST"));
    }
    
    @Test
    @DisplayName("Should unregister specific endpoint")
    void shouldUnregisterSpecificEndpoint() {
        EndpointSecurity security = EndpointSecurity.builder()
            .roles(Set.of("USER"))
            .build();
        
        registry.registerEndpoint("/api/test", "GET", security);
        assertTrue(registry.isRegistered("/api/test", "GET"));
        
        registry.unregisterEndpoint("/api/test", "GET");
        assertFalse(registry.isRegistered("/api/test", "GET"));
    }
}
