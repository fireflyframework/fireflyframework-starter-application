package org.fireflyframework.common.application.context;

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
        String subject = "user-123";
        UUID tenantId = UUID.randomUUID();

        AppContext context = AppContext.builder()
            .subject(subject)
            .tenantId(tenantId)
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
        assertEquals(subject, execContext.getSubject());
        assertEquals(tenantId, execContext.getTenantId());
    }

    @Test
    @DisplayName("Should create minimal execution context")
    void shouldCreateMinimalExecutionContext() {
        String subject = "user-123";
        UUID tenantId = UUID.randomUUID();

        ApplicationExecutionContext context = ApplicationExecutionContext.createMinimal(subject, tenantId);

        assertNotNull(context);
        assertEquals(subject, context.getSubject());
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
            "user-123", tenantId);

        assertEquals(tenantId, context.getTenantId());
    }

    @Test
    @DisplayName("Should get subject from context")
    void shouldGetSubjectFromContext() {
        String subject = "user-456";

        ApplicationExecutionContext context = ApplicationExecutionContext.createMinimal(
            subject, UUID.randomUUID());

        assertEquals(subject, context.getSubject());
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
            .context(AppContext.builder().subject("user-123").build())
            .config(AppConfig.builder().tenantId(UUID.randomUUID()).build())
            .securityContext(authorizedSecurity)
            .build();

        ApplicationExecutionContext unauthorized = ApplicationExecutionContext.builder()
            .context(AppContext.builder().subject("user-123").build())
            .config(AppConfig.builder().tenantId(UUID.randomUUID()).build())
            .securityContext(unauthorizedSecurity)
            .build();

        ApplicationExecutionContext noSecurity = ApplicationExecutionContext.builder()
            .context(AppContext.builder().subject("user-123").build())
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
            .subject("user-123")
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
            .context(AppContext.builder().subject("user-123").build())
            .config(appConfig)
            .build();

        assertTrue(context.isFeatureEnabled("FEATURE_A"));
        assertFalse(context.isFeatureEnabled("FEATURE_B"));
        assertFalse(context.isFeatureEnabled("FEATURE_C"));
    }

    @Test
    @DisplayName("Should support immutable updates with withers")
    void shouldSupportImmutableUpdates() {
        String originalSubject = "user-original";
        String newSubject = "user-new";

        AppContext originalContext = AppContext.builder()
            .subject(originalSubject)
            .build();

        ApplicationExecutionContext original = ApplicationExecutionContext.builder()
            .context(originalContext)
            .config(AppConfig.builder().tenantId(UUID.randomUUID()).build())
            .build();

        AppContext newContext = originalContext.withSubject(newSubject);
        ApplicationExecutionContext updated = original.withContext(newContext);

        assertEquals(originalSubject, original.getSubject());
        assertEquals(newSubject, updated.getSubject());
        assertNotSame(original, updated);
    }

    @Test
    @DisplayName("Should support toBuilder pattern")
    void shouldSupportToBuilder() {
        UUID originalTenantId = UUID.randomUUID();
        UUID newTenantId = UUID.randomUUID();

        ApplicationExecutionContext original = ApplicationExecutionContext.createMinimal(
            "user-123", originalTenantId);

        ApplicationExecutionContext updated = original.toBuilder()
            .config(AppConfig.builder().tenantId(newTenantId).build())
            .build();

        assertEquals(originalTenantId, original.getTenantId());
        assertEquals(newTenantId, updated.getTenantId());
    }
}
