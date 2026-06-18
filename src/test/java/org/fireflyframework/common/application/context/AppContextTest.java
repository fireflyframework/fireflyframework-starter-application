package org.fireflyframework.common.application.context;

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
        String subject = "user-123";
        UUID tenantId = UUID.randomUUID();
        Set<String> roles = Set.of("ACCOUNT_OWNER", "ADMIN");
        Set<String> permissions = Set.of("READ", "WRITE", "DELETE");

        AppContext context = AppContext.builder()
            .subject(subject)
            .tenantId(tenantId)
            .roles(roles)
            .permissions(permissions)
            .build();

        assertEquals(subject, context.getSubject());
        assertEquals(tenantId, context.getTenantId());
        assertEquals(roles, context.getRoles());
        assertEquals(permissions, context.getPermissions());
    }

    @Test
    @DisplayName("Should check if context has specific role")
    void shouldCheckHasRole() {
        AppContext context = AppContext.builder()
            .subject("user-123")
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
            .subject("user-123")
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
            .subject("user-123")
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
            .subject("user-123")
            .build();

        assertFalse(context.hasRole("ADMIN"));
        assertFalse(context.hasAnyRole("ADMIN", "VIEWER"));
        assertFalse(context.hasAllRoles("ADMIN"));
    }

    @Test
    @DisplayName("Should check if context has specific permission")
    void shouldCheckHasPermission() {
        AppContext context = AppContext.builder()
            .subject("user-123")
            .permissions(Set.of("READ", "WRITE"))
            .build();

        assertTrue(context.hasPermission("READ"));
        assertTrue(context.hasPermission("WRITE"));
        assertFalse(context.hasPermission("DELETE"));
    }

    @Test
    @DisplayName("Should handle null permissions gracefully")
    void shouldHandleNullPermissions() {
        AppContext context = AppContext.builder()
            .subject("user-123")
            .build();

        assertFalse(context.hasPermission("READ"));
    }

    @Test
    @DisplayName("Should carry an optional tenant id")
    void shouldCarryOptionalTenantId() {
        UUID tenantId = UUID.randomUUID();

        AppContext withTenant = AppContext.builder()
            .subject("user-123")
            .tenantId(tenantId)
            .build();

        AppContext withoutTenant = AppContext.builder()
            .subject("user-123")
            .build();

        assertEquals(tenantId, withTenant.getTenantId());
        assertNull(withoutTenant.getTenantId());
    }

    @Test
    @DisplayName("Should store and retrieve attributes")
    void shouldStoreAndRetrieveAttributes() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("key1", "value1");
        attributes.put("key2", 123);
        attributes.put("key3", true);

        AppContext context = AppContext.builder()
            .subject("user-123")
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
            .subject("user-123")
            .build();

        assertNull(context.getAttribute("anyKey"));
    }

    @Test
    @DisplayName("Should support immutable updates with withers")
    void shouldSupportImmutableUpdates() {
        String originalSubject = "user-original";
        String newSubject = "user-new";

        AppContext original = AppContext.builder()
            .subject(originalSubject)
            .build();

        AppContext updated = original.withSubject(newSubject);

        assertEquals(originalSubject, original.getSubject());
        assertEquals(newSubject, updated.getSubject());
        assertNotSame(original, updated);
    }

    @Test
    @DisplayName("Should support builder pattern with toBuilder")
    void shouldSupportToBuilder() {
        Set<String> originalRoles = Set.of("VIEWER");
        Set<String> newRoles = Set.of("ADMIN");

        AppContext original = AppContext.builder()
            .subject("user-123")
            .roles(originalRoles)
            .build();

        AppContext updated = original.toBuilder()
            .roles(newRoles)
            .build();

        assertEquals(originalRoles, original.getRoles());
        assertEquals(newRoles, updated.getRoles());
    }
}
