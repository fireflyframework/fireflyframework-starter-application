package org.fireflyframework.application.context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ApplicationExecutionContext Tests")
class ApplicationExecutionContextTest {
    
    @Test
    @DisplayName("Should create execution context with builder")
    void shouldCreateExecutionContextWithBuilder() {
        UUID partyId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        
        AppContext context = AppContext.builder()
            .partyId(partyId)
            .tenantId(tenantId)
            .contractId(contractId)
            .roles(Set.of("USER"))
            .build();
        
        AppConfig config = AppConfig.builder()
            .tenantId(tenantId)
            .tenantName("Test Tenant")
            .build();
        
        AppSecurityContext securityContext = AppSecurityContext.builder()
            .endpoint("/api/test")
            .httpMethod("GET")
            .authorized(true)
            .build();
        
        ApplicationExecutionContext execContext = ApplicationExecutionContext.builder()
            .context(context)
            .config(config)
            .securityContext(securityContext)
            .build();
        
        assertNotNull(execContext.getContext());
        assertNotNull(execContext.getConfig());
        assertNotNull(execContext.getSecurityContext());
        assertEquals(partyId, execContext.getPartyId());
        assertEquals(tenantId, execContext.getTenantId());
        assertEquals(contractId, execContext.getContractId());
    }
    
    @Test
    @DisplayName("Should create minimal execution context")
    void shouldCreateMinimalExecutionContext() {
        UUID partyId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        
        ApplicationExecutionContext context = ApplicationExecutionContext.createMinimal(partyId, tenantId);
        
        assertNotNull(context);
        assertEquals(partyId, context.getPartyId());
        assertEquals(tenantId, context.getTenantId());
        assertNotNull(context.getContext());
        assertNotNull(context.getConfig());
        assertNull(context.getSecurityContext());
    }
    
    @Test
    @DisplayName("Should get tenantId from config")
    void shouldGetTenantIdFromConfig() {
        UUID tenantId = UUID.randomUUID();
        
        ApplicationExecutionContext context = ApplicationExecutionContext.createMinimal(
            UUID.randomUUID(), tenantId);
        
        assertEquals(tenantId, context.getTenantId());
    }
    
    @Test
    @DisplayName("Should get partyId from context")
    void shouldGetPartyIdFromContext() {
        UUID partyId = UUID.randomUUID();
        
        ApplicationExecutionContext context = ApplicationExecutionContext.createMinimal(
            partyId, UUID.randomUUID());
        
        assertEquals(partyId, context.getPartyId());
    }
    
    @Test
    @DisplayName("Should get contractId from context")
    void shouldGetContractIdFromContext() {
        UUID contractId = UUID.randomUUID();
        
        AppContext appContext = AppContext.builder()
            .partyId(UUID.randomUUID())
            .contractId(contractId)
            .build();
        
        ApplicationExecutionContext context = ApplicationExecutionContext.builder()
            .context(appContext)
            .config(AppConfig.builder().tenantId(UUID.randomUUID()).build())
            .build();
        
        assertEquals(contractId, context.getContractId());
    }
    
    @Test
    @DisplayName("Should get productId from context")
    void shouldGetProductIdFromContext() {
        UUID productId = UUID.randomUUID();
        
        AppContext appContext = AppContext.builder()
            .partyId(UUID.randomUUID())
            .productId(productId)
            .build();
        
        ApplicationExecutionContext context = ApplicationExecutionContext.builder()
            .context(appContext)
            .config(AppConfig.builder().tenantId(UUID.randomUUID()).build())
            .build();
        
        assertEquals(productId, context.getProductId());
    }
    
    @Test
    @DisplayName("Should check if context is authorized")
    void shouldCheckIfAuthorized() {
        AppSecurityContext authorizedSecurity = AppSecurityContext.builder()
            .endpoint("/api/test")
            .authorized(true)
            .build();
        
        AppSecurityContext unauthorizedSecurity = AppSecurityContext.builder()
            .endpoint("/api/test")
            .authorized(false)
            .build();
        
        ApplicationExecutionContext authorized = ApplicationExecutionContext.builder()
            .context(AppContext.builder().partyId(UUID.randomUUID()).build())
            .config(AppConfig.builder().tenantId(UUID.randomUUID()).build())
            .securityContext(authorizedSecurity)
            .build();
        
        ApplicationExecutionContext unauthorized = ApplicationExecutionContext.builder()
            .context(AppContext.builder().partyId(UUID.randomUUID()).build())
            .config(AppConfig.builder().tenantId(UUID.randomUUID()).build())
            .securityContext(unauthorizedSecurity)
            .build();
        
        ApplicationExecutionContext noSecurity = ApplicationExecutionContext.builder()
            .context(AppContext.builder().partyId(UUID.randomUUID()).build())
            .config(AppConfig.builder().tenantId(UUID.randomUUID()).build())
            .build();
        
        assertTrue(authorized.isAuthorized());
        assertFalse(unauthorized.isAuthorized());
        assertFalse(noSecurity.isAuthorized());
    }
    
    @Test
    @DisplayName("Should check if context has role")
    void shouldCheckIfHasRole() {
        AppContext appContext = AppContext.builder()
            .partyId(UUID.randomUUID())
            .roles(Set.of("ADMIN", "USER"))
            .build();
        
        ApplicationExecutionContext context = ApplicationExecutionContext.builder()
            .context(appContext)
            .config(AppConfig.builder().tenantId(UUID.randomUUID()).build())
            .build();
        
        assertTrue(context.hasRole("ADMIN"));
        assertTrue(context.hasRole("USER"));
        assertFalse(context.hasRole("VIEWER"));
    }
    
    @Test
    @DisplayName("Should check if feature is enabled")
    void shouldCheckIfFeatureEnabled() {
        AppConfig appConfig = AppConfig.builder()
            .tenantId(UUID.randomUUID())
            .featureFlags(Map.of("FEATURE_A", true, "FEATURE_B", false))
            .build();
        
        ApplicationExecutionContext context = ApplicationExecutionContext.builder()
            .context(AppContext.builder().partyId(UUID.randomUUID()).build())
            .config(appConfig)
            .build();
        
        assertTrue(context.isFeatureEnabled("FEATURE_A"));
        assertFalse(context.isFeatureEnabled("FEATURE_B"));
        assertFalse(context.isFeatureEnabled("FEATURE_C"));
    }
    
    @Test
    @DisplayName("Should support immutable updates with withers")
    void shouldSupportImmutableUpdates() {
        UUID originalPartyId = UUID.randomUUID();
        UUID newPartyId = UUID.randomUUID();
        
        AppContext originalContext = AppContext.builder()
            .partyId(originalPartyId)
            .build();
        
        ApplicationExecutionContext original = ApplicationExecutionContext.builder()
            .context(originalContext)
            .config(AppConfig.builder().tenantId(UUID.randomUUID()).build())
            .build();
        
        AppContext newContext = originalContext.withPartyId(newPartyId);
        ApplicationExecutionContext updated = original.withContext(newContext);
        
        assertEquals(originalPartyId, original.getPartyId());
        assertEquals(newPartyId, updated.getPartyId());
        assertNotSame(original, updated);
    }
    
    @Test
    @DisplayName("Should support toBuilder pattern")
    void shouldSupportToBuilder() {
        UUID originalTenantId = UUID.randomUUID();
        UUID newTenantId = UUID.randomUUID();
        
        ApplicationExecutionContext original = ApplicationExecutionContext.createMinimal(
            UUID.randomUUID(), originalTenantId);
        
        ApplicationExecutionContext updated = original.toBuilder()
            .config(AppConfig.builder().tenantId(newTenantId).build())
            .build();
        
        assertEquals(originalTenantId, original.getTenantId());
        assertEquals(newTenantId, updated.getTenantId());
    }
}
