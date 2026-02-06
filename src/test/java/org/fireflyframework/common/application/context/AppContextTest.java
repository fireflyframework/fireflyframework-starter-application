package org.fireflyframework.application.context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AppContext Tests")
class AppContextTest {
    
    @Test
    @DisplayName("Should create AppContext with builder")
    void shouldCreateAppContextWithBuilder() {
        UUID partyId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Set<String> roles = Set.of("ACCOUNT_OWNER", "ADMIN");
        Set<String> permissions = Set.of("READ", "WRITE", "DELETE");
        
        AppContext context = AppContext.builder()
            .partyId(partyId)
            .contractId(contractId)
            .productId(productId)
            .tenantId(tenantId)
            .roles(roles)
            .permissions(permissions)
            .build();
        
        assertEquals(partyId, context.getPartyId());
        assertEquals(contractId, context.getContractId());
        assertEquals(productId, context.getProductId());
        assertEquals(tenantId, context.getTenantId());
        assertEquals(roles, context.getRoles());
        assertEquals(permissions, context.getPermissions());
    }
    
    @Test
    @DisplayName("Should check if context has specific role")
    void shouldCheckHasRole() {
        AppContext context = AppContext.builder()
            .partyId(UUID.randomUUID())
            .roles(Set.of("ACCOUNT_OWNER", "VIEWER"))
            .build();
        
        assertTrue(context.hasRole("ACCOUNT_OWNER"));
        assertTrue(context.hasRole("VIEWER"));
        assertFalse(context.hasRole("ADMIN"));
    }
    
    @Test
    @DisplayName("Should check if context has any of specified roles")
    void shouldCheckHasAnyRole() {
        AppContext context = AppContext.builder()
            .partyId(UUID.randomUUID())
            .roles(Set.of("ACCOUNT_OWNER"))
            .build();
        
        assertTrue(context.hasAnyRole("ACCOUNT_OWNER", "ADMIN"));
        assertTrue(context.hasAnyRole("VIEWER", "ACCOUNT_OWNER"));
        assertFalse(context.hasAnyRole("ADMIN", "VIEWER"));
    }
    
    @Test
    @DisplayName("Should check if context has all specified roles")
    void shouldCheckHasAllRoles() {
        AppContext context = AppContext.builder()
            .partyId(UUID.randomUUID())
            .roles(Set.of("ACCOUNT_OWNER", "ADMIN", "VIEWER"))
            .build();
        
        assertTrue(context.hasAllRoles("ACCOUNT_OWNER", "ADMIN"));
        assertTrue(context.hasAllRoles("VIEWER"));
        assertFalse(context.hasAllRoles("ACCOUNT_OWNER", "EDITOR"));
    }
    
    @Test
    @DisplayName("Should handle null roles gracefully")
    void shouldHandleNullRoles() {
        AppContext context = AppContext.builder()
            .partyId(UUID.randomUUID())
            .build();
        
        assertFalse(context.hasRole("ADMIN"));
        assertFalse(context.hasAnyRole("ADMIN", "VIEWER"));
        assertFalse(context.hasAllRoles("ADMIN"));
    }
    
    @Test
    @DisplayName("Should check if context has specific permission")
    void shouldCheckHasPermission() {
        AppContext context = AppContext.builder()
            .partyId(UUID.randomUUID())
            .permissions(Set.of("READ", "WRITE"))
            .build();
        
        assertTrue(context.hasPermission("READ"));
        assertTrue(context.hasPermission("WRITE"));
        assertFalse(context.hasPermission("DELETE"));
    }
    
    @Test
    @DisplayName("Should check if context has contract")
    void shouldCheckHasContract() {
        AppContext withContract = AppContext.builder()
            .partyId(UUID.randomUUID())
            .contractId(UUID.randomUUID())
            .build();
        
        AppContext withoutContract = AppContext.builder()
            .partyId(UUID.randomUUID())
            .build();
        
        assertTrue(withContract.hasContract());
        assertFalse(withoutContract.hasContract());
    }
    
    @Test
    @DisplayName("Should check if context has product")
    void shouldCheckHasProduct() {
        AppContext withProduct = AppContext.builder()
            .partyId(UUID.randomUUID())
            .productId(UUID.randomUUID())
            .build();
        
        AppContext withoutProduct = AppContext.builder()
            .partyId(UUID.randomUUID())
            .build();
        
        assertTrue(withProduct.hasProduct());
        assertFalse(withoutProduct.hasProduct());
    }
    
    @Test
    @DisplayName("Should store and retrieve attributes")
    void shouldStoreAndRetrieveAttributes() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("key1", "value1");
        attributes.put("key2", 123);
        attributes.put("key3", true);
        
        AppContext context = AppContext.builder()
            .partyId(UUID.randomUUID())
            .attributes(attributes)
            .build();
        
        assertEquals("value1", context.getAttribute("key1"));
        assertEquals(123, context.<Integer>getAttribute("key2"));
        assertEquals(true, context.getAttribute("key3"));
        assertNull(context.getAttribute("nonexistent"));
    }
    
    @Test
    @DisplayName("Should handle null attributes gracefully")
    void shouldHandleNullAttributes() {
        AppContext context = AppContext.builder()
            .partyId(UUID.randomUUID())
            .build();
        
        assertNull(context.getAttribute("anyKey"));
    }
    
    @Test
    @DisplayName("Should support immutable updates with withers")
    void shouldSupportImmutableUpdates() {
        UUID originalPartyId = UUID.randomUUID();
        UUID newPartyId = UUID.randomUUID();
        
        AppContext original = AppContext.builder()
            .partyId(originalPartyId)
            .build();
        
        AppContext updated = original.withPartyId(newPartyId);
        
        assertEquals(originalPartyId, original.getPartyId());
        assertEquals(newPartyId, updated.getPartyId());
        assertNotSame(original, updated);
    }
    
    @Test
    @DisplayName("Should support builder pattern with toBuilder")
    void shouldSupportToBuilder() {
        UUID originalContractId = UUID.randomUUID();
        UUID newContractId = UUID.randomUUID();
        
        AppContext original = AppContext.builder()
            .partyId(UUID.randomUUID())
            .contractId(originalContractId)
            .build();
        
        AppContext updated = original.toBuilder()
            .contractId(newContractId)
            .build();
        
        assertEquals(originalContractId, original.getContractId());
        assertEquals(newContractId, updated.getContractId());
    }
}
