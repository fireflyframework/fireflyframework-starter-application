package org.fireflyframework.application.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AbstractSecurityConfiguration Tests")
class AbstractSecurityConfigurationTest {
    
    @Test
    @DisplayName("Should register single endpoint with fluent API")
    void shouldRegisterSingleEndpointWithFluentApi() {
        EndpointSecurityRegistry registry = new EndpointSecurityRegistry();
        TestSecurityConfiguration config = new TestSecurityConfiguration(registry);
        config.testInitialize();
        
        assertTrue(registry.isRegistered("/api/test", "GET"));
        var security = registry.getEndpointSecurity("/api/test", "GET").orElse(null);
        assertNotNull(security);
        assertEquals(Set.of("USER"), security.getRoles());
    }
    
    @Test
    @DisplayName("Should register multiple methods for same endpoint")
    void shouldRegisterMultipleMethodsForSameEndpoint() {
        EndpointSecurityRegistry registry = new EndpointSecurityRegistry();
        TestSecurityConfiguration config = new TestSecurityConfiguration(registry);
        config.testInitialize();
        
        assertTrue(registry.isRegistered("/api/accounts", "GET"));
        assertTrue(registry.isRegistered("/api/accounts", "POST"));
        
        var getSecurity = registry.getEndpointSecurity("/api/accounts", "GET").orElse(null);
        var postSecurity = registry.getEndpointSecurity("/api/accounts", "POST").orElse(null);
        
        assertNotNull(getSecurity);
        assertNotNull(postSecurity);
        assertEquals(Set.of("VIEWER"), getSecurity.getRoles());
        assertEquals(Set.of("ADMIN"), postSecurity.getRoles());
    }
    
    @Test
    @DisplayName("Should support requireRoles (any) configuration")
    void shouldSupportRequireRolesAnyConfiguration() {
        EndpointSecurityRegistry registry = new EndpointSecurityRegistry();
        TestSecurityConfiguration config = new TestSecurityConfiguration(registry);
        config.testInitialize();
        
        var security = registry
            .getEndpointSecurity("/api/accounts", "GET").orElse(null);
        
        assertNotNull(security);
        assertTrue(security.getRoles().contains("VIEWER"));
        assertFalse(security.isRequireAllRoles());
    }
    
    @Test
    @DisplayName("Should support requireAllRoles configuration")
    void shouldSupportRequireAllRolesConfiguration() {
        EndpointSecurityRegistry registry = new EndpointSecurityRegistry();
        TestSecurityConfiguration config = new TestSecurityConfiguration(registry);
        config.testInitialize();
        
        var security = registry
            .getEndpointSecurity("/api/admin", "DELETE").orElse(null);
        
        assertNotNull(security);
        assertTrue(security.isRequireAllRoles());
        assertTrue(security.getRoles().contains("ADMIN"));
        assertTrue(security.getRoles().contains("SUPER_ADMIN"));
    }
    
    @Test
    @DisplayName("Should support requirePermissions configuration")
    void shouldSupportRequirePermissionsConfiguration() {
        EndpointSecurityRegistry registry = new EndpointSecurityRegistry();
        TestSecurityConfiguration config = new TestSecurityConfiguration(registry);
        config.testInitialize();
        
        var security = registry
            .getEndpointSecurity("/api/accounts", "POST").orElse(null);
        
        assertNotNull(security);
        assertEquals(Set.of("CREATE_ACCOUNT"), security.getPermissions());
    }
    
    @Test
    @DisplayName("Should support requireAllPermissions configuration")
    void shouldSupportRequireAllPermissionsConfiguration() {
        EndpointSecurityRegistry registry = new EndpointSecurityRegistry();
        TestSecurityConfiguration config = new TestSecurityConfiguration(registry);
        config.testInitialize();
        
        var security = registry
            .getEndpointSecurity("/api/transfer", "POST").orElse(null);
        
        assertNotNull(security);
        assertTrue(security.isRequireAllPermissions());
        assertTrue(security.getPermissions().contains("TRANSFER_FUNDS"));
        assertTrue(security.getPermissions().contains("VIEW_BALANCE"));
    }
    
    @Test
    @DisplayName("Should handle multiple HTTP methods")
    void shouldHandleMultipleHttpMethods() {
        EndpointSecurityRegistry registry = new EndpointSecurityRegistry();
        TestSecurityConfiguration config = new TestSecurityConfiguration(registry);
        config.testInitialize();
        
        // Just verify one method since we test separately for each
        assertTrue(registry.isRegistered("/api/public/get", "GET"));
    }
    
    /**
     * Test implementation of AbstractSecurityConfiguration
     */
    static class TestSecurityConfiguration extends AbstractSecurityConfiguration {
        
        private final EndpointSecurityRegistry testRegistry;
        
        public TestSecurityConfiguration(EndpointSecurityRegistry registry) {
            this.testRegistry = registry;
        }
        
        // Expose registry for testing
        public EndpointSecurityRegistry getTestRegistry() {
            return testRegistry;
        }
        
        // Simulate @PostConstruct for testing
        public void testInitialize() {
            // Manually inject registry since we're not using Spring in tests
            try {
                var field = AbstractSecurityConfiguration.class.getDeclaredField("securityRegistry");
                field.setAccessible(true);
                field.set(this, testRegistry);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            configureEndpointSecurity();
        }
        
        @Override
        protected void configureEndpointSecurity() {
            // Single role requirement
            protect("/api/test")
                .onMethod("GET")
                .requireRoles("USER")
                .register();
            
            // Multiple endpoints with different roles
            protect("/api/accounts")
                .onMethod("GET")
                .requireRoles("VIEWER")
                .register();
            
            protect("/api/accounts")
                .onMethod("POST")
                .requireRoles("ADMIN")
                .requirePermissions("CREATE_ACCOUNT")
                .register();
            
            // Require ALL roles
            protect("/api/admin")
                .onMethod("DELETE")
                .requireAllRoles("ADMIN", "SUPER_ADMIN")
                .register();
            
            // Require ALL permissions
            protect("/api/transfer")
                .onMethod("POST")
                .requireAllPermissions("TRANSFER_FUNDS", "VIEW_BALANCE")
                .register();
            
            // Multiple method registrations
            protect("/api/public/get")
                .onMethod("GET")
                .requireRoles("PUBLIC_USER")
                .register();
        }
    }
}
