package org.fireflyframework.application.context;

import org.fireflyframework.application.context.AppSecurityContext.SecurityConfigSource;
import org.fireflyframework.application.context.AppSecurityContext.SecurityEvaluationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AppSecurityContext Tests")
class AppSecurityContextTest {
    
    @Test
    @DisplayName("Should create security context with builder")
    void shouldCreateSecurityContextWithBuilder() {
        AppSecurityContext context = AppSecurityContext.builder()
            .endpoint("/api/accounts")
            .httpMethod("POST")
            .requiredRoles(Set.of("ADMIN"))
            .requiredPermissions(Set.of("CREATE_ACCOUNT"))
            .authorized(true)
            .configSource(SecurityConfigSource.ANNOTATION)
            .build();
        
        assertEquals("/api/accounts", context.getEndpoint());
        assertEquals("POST", context.getHttpMethod());
        assertTrue(context.getRequiredRoles().contains("ADMIN"));
        assertTrue(context.getRequiredPermissions().contains("CREATE_ACCOUNT"));
        assertTrue(context.isAuthorized());
        assertEquals(SecurityConfigSource.ANNOTATION, context.getConfigSource());
    }
    
    @Test
    @DisplayName("Should check if has required roles")
    void shouldCheckHasRequiredRoles() {
        AppSecurityContext withRoles = AppSecurityContext.builder()
            .requiredRoles(Set.of("USER"))
            .build();
        
        AppSecurityContext withoutRoles = AppSecurityContext.builder()
            .build();
        
        AppSecurityContext withEmptyRoles = AppSecurityContext.builder()
            .requiredRoles(Set.of())
            .build();
        
        assertTrue(withRoles.hasRequiredRoles());
        assertFalse(withoutRoles.hasRequiredRoles());
        assertFalse(withEmptyRoles.hasRequiredRoles());
    }
    
    @Test
    @DisplayName("Should check if has required permissions")
    void shouldCheckHasRequiredPermissions() {
        AppSecurityContext withPermissions = AppSecurityContext.builder()
            .requiredPermissions(Set.of("READ"))
            .build();
        
        AppSecurityContext withoutPermissions = AppSecurityContext.builder()
            .build();
        
        assertTrue(withPermissions.hasRequiredPermissions());
        assertFalse(withoutPermissions.hasRequiredPermissions());
    }
    
    @Test
    @DisplayName("Should check if requires specific role")
    void shouldCheckRequiresRole() {
        AppSecurityContext context = AppSecurityContext.builder()
            .requiredRoles(Set.of("ADMIN", "EDITOR"))
            .build();
        
        assertTrue(context.requiresRole("ADMIN"));
        assertTrue(context.requiresRole("EDITOR"));
        assertFalse(context.requiresRole("VIEWER"));
    }
    
    @Test
    @DisplayName("Should check if requires specific permission")
    void shouldCheckRequiresPermission() {
        AppSecurityContext context = AppSecurityContext.builder()
            .requiredPermissions(Set.of("READ", "WRITE"))
            .build();
        
        assertTrue(context.requiresPermission("READ"));
        assertTrue(context.requiresPermission("WRITE"));
        assertFalse(context.requiresPermission("DELETE"));
    }
    
    @Test
    @DisplayName("Should store and retrieve security attributes")
    void shouldStoreAndRetrieveSecurityAttributes() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("key1", "value1");
        attributes.put("key2", 123);
        
        AppSecurityContext context = AppSecurityContext.builder()
            .securityAttributes(attributes)
            .build();
        
        assertEquals("value1", context.getSecurityAttribute("key1"));
        assertEquals(123, context.<Integer>getSecurityAttribute("key2"));
        assertNull(context.getSecurityAttribute("nonexistent"));
    }
    
    @Test
    @DisplayName("Should default requiresAuthentication to true")
    void shouldDefaultRequiresAuthenticationToTrue() {
        AppSecurityContext context = AppSecurityContext.builder()
            .endpoint("/api/test")
            .build();
        
        assertTrue(context.isRequiresAuthentication());
    }
    
    @Test
    @DisplayName("Should default allowAnonymous to false")
    void shouldDefaultAllowAnonymousToFalse() {
        AppSecurityContext context = AppSecurityContext.builder()
            .endpoint("/api/test")
            .build();
        
        assertFalse(context.isAllowAnonymous());
    }
    
    @Test
    @DisplayName("Should allow anonymous access when configured")
    void shouldAllowAnonymousAccessWhenConfigured() {
        AppSecurityContext context = AppSecurityContext.builder()
            .endpoint("/api/public")
            .allowAnonymous(true)
            .requiresAuthentication(false)
            .build();
        
        assertTrue(context.isAllowAnonymous());
        assertFalse(context.isRequiresAuthentication());
    }
    
    @Test
    @DisplayName("Should store authorization failure reason")
    void shouldStoreAuthorizationFailureReason() {
        AppSecurityContext context = AppSecurityContext.builder()
            .endpoint("/api/test")
            .authorized(false)
            .authorizationFailureReason("Insufficient permissions")
            .build();
        
        assertFalse(context.isAuthorized());
        assertEquals("Insufficient permissions", context.getAuthorizationFailureReason());
    }
    
    @Test
    @DisplayName("Should support different security config sources")
    void shouldSupportDifferentSecurityConfigSources() {
        AppSecurityContext annotation = AppSecurityContext.builder()
            .configSource(SecurityConfigSource.ANNOTATION)
            .build();
        
        AppSecurityContext explicitMap = AppSecurityContext.builder()
            .configSource(SecurityConfigSource.EXPLICIT_MAP)
            .build();
        
        AppSecurityContext securityCenter = AppSecurityContext.builder()
            .configSource(SecurityConfigSource.SECURITY_CENTER)
            .build();
        
        AppSecurityContext defaultSource = AppSecurityContext.builder()
            .configSource(SecurityConfigSource.DEFAULT)
            .build();
        
        assertEquals(SecurityConfigSource.ANNOTATION, annotation.getConfigSource());
        assertEquals(SecurityConfigSource.EXPLICIT_MAP, explicitMap.getConfigSource());
        assertEquals(SecurityConfigSource.SECURITY_CENTER, securityCenter.getConfigSource());
        assertEquals(SecurityConfigSource.DEFAULT, defaultSource.getConfigSource());
    }
    
    @Test
    @DisplayName("Should store security evaluation result")
    void shouldStoreSecurityEvaluationResult() {
        Instant now = Instant.now();
        
        SecurityEvaluationResult evalResult = SecurityEvaluationResult.builder()
            .granted(true)
            .reason("Policy ALLOW_ADMIN matched")
            .evaluatedPolicy("ALLOW_ADMIN")
            .evaluatedAt(now)
            .build();
        
        AppSecurityContext context = AppSecurityContext.builder()
            .endpoint("/api/test")
            .evaluationResult(evalResult)
            .build();
        
        assertNotNull(context.getEvaluationResult());
        assertTrue(context.getEvaluationResult().isGranted());
        assertEquals("Policy ALLOW_ADMIN matched", context.getEvaluationResult().getReason());
        assertEquals(now, context.getEvaluationResult().getEvaluatedAt());
    }
}

@DisplayName("SecurityEvaluationResult Tests")
class SecurityEvaluationResultTest {
    
    @Test
    @DisplayName("Should create evaluation result with builder")
    void shouldCreateEvaluationResultWithBuilder() {
        Instant now = Instant.now();
        
        SecurityEvaluationResult result = SecurityEvaluationResult.builder()
            .granted(true)
            .reason("Access granted")
            .evaluatedPolicy("POLICY_001")
            .evaluatedAt(now)
            .build();
        
        assertTrue(result.isGranted());
        assertEquals("Access granted", result.getReason());
        assertEquals("POLICY_001", result.getEvaluatedPolicy());
        assertEquals(now, result.getEvaluatedAt());
    }
    
    @Test
    @DisplayName("Should store evaluation details")
    void shouldStoreEvaluationDetails() {
        Map<String, Object> details = Map.of(
            "evaluator", "SecurityCenter",
            "confidence", 0.95,
            "rulesEvaluated", 5
        );
        
        SecurityEvaluationResult result = SecurityEvaluationResult.builder()
            .granted(true)
            .evaluationDetails(details)
            .build();
        
        assertEquals("SecurityCenter", result.getEvaluationDetail("evaluator"));
        assertEquals(0.95, result.<Double>getEvaluationDetail("confidence"));
        assertEquals(5, result.<Integer>getEvaluationDetail("rulesEvaluated"));
        assertNull(result.getEvaluationDetail("nonexistent"));
    }
    
    @Test
    @DisplayName("Should handle denial with reason")
    void shouldHandleDenialWithReason() {
        SecurityEvaluationResult result = SecurityEvaluationResult.builder()
            .granted(false)
            .reason("User lacks required role: ADMIN")
            .evaluatedPolicy("REQUIRE_ADMIN_ROLE")
            .build();
        
        assertFalse(result.isGranted());
        assertEquals("User lacks required role: ADMIN", result.getReason());
        assertEquals("REQUIRE_ADMIN_ROLE", result.getEvaluatedPolicy());
    }
    
    @Test
    @DisplayName("Should support immutable updates with withers")
    void shouldSupportImmutableUpdates() {
        SecurityEvaluationResult original = SecurityEvaluationResult.builder()
            .granted(false)
            .reason("Original reason")
            .build();
        
        SecurityEvaluationResult updated = original.withGranted(true);
        
        assertFalse(original.isGranted());
        assertTrue(updated.isGranted());
        assertNotSame(original, updated);
    }
}
