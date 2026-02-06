package org.fireflyframework.application.context;

import org.fireflyframework.application.context.AppConfig.ProviderConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AppConfig Tests")
class AppConfigTest {
    
    @Test
    @DisplayName("Should create AppConfig with builder")
    void shouldCreateAppConfigWithBuilder() {
        UUID tenantId = UUID.randomUUID();
        String tenantName = "Test Tenant";
        String environment = "dev";
        
        AppConfig config = AppConfig.builder()
            .tenantId(tenantId)
            .tenantName(tenantName)
            .environment(environment)
            .active(true)
            .build();
        
        assertEquals(tenantId, config.getTenantId());
        assertEquals(tenantName, config.getTenantName());
        assertEquals(environment, config.getEnvironment());
        assertTrue(config.isActive());
    }
    
    @Test
    @DisplayName("Should get provider configuration by type")
    void shouldGetProviderByType() {
        ProviderConfig paymentProvider = ProviderConfig.builder()
            .providerType("PAYMENT_GATEWAY")
            .implementation("stripe")
            .build();
        
        Map<String, ProviderConfig> providers = Map.of("PAYMENT_GATEWAY", paymentProvider);
        
        AppConfig config = AppConfig.builder()
            .tenantId(UUID.randomUUID())
            .providers(providers)
            .build();
        
        assertTrue(config.getProvider("PAYMENT_GATEWAY").isPresent());
        assertEquals("stripe", config.getProvider("PAYMENT_GATEWAY").get().getImplementation());
        assertFalse(config.getProvider("NONEXISTENT").isPresent());
    }
    
    @Test
    @DisplayName("Should check if provider exists")
    void shouldCheckHasProvider() {
        ProviderConfig kycProvider = ProviderConfig.builder()
            .providerType("KYC_PROVIDER")
            .build();
        
        AppConfig config = AppConfig.builder()
            .tenantId(UUID.randomUUID())
            .providers(Map.of("KYC_PROVIDER", kycProvider))
            .build();
        
        assertTrue(config.hasProvider("KYC_PROVIDER"));
        assertFalse(config.hasProvider("EMAIL_PROVIDER"));
    }
    
    @Test
    @DisplayName("Should handle null providers gracefully")
    void shouldHandleNullProviders() {
        AppConfig config = AppConfig.builder()
            .tenantId(UUID.randomUUID())
            .build();
        
        assertFalse(config.hasProvider("ANY_PROVIDER"));
        assertFalse(config.getProvider("ANY_PROVIDER").isPresent());
    }
    
    @Test
    @DisplayName("Should check if feature flag is enabled")
    void shouldCheckFeatureFlag() {
        Map<String, Boolean> featureFlags = Map.of(
            "FEATURE_A", true,
            "FEATURE_B", false
        );
        
        AppConfig config = AppConfig.builder()
            .tenantId(UUID.randomUUID())
            .featureFlags(featureFlags)
            .build();
        
        assertTrue(config.isFeatureEnabled("FEATURE_A"));
        assertFalse(config.isFeatureEnabled("FEATURE_B"));
        assertFalse(config.isFeatureEnabled("NONEXISTENT"));
    }
    
    @Test
    @DisplayName("Should get setting value")
    void shouldGetSetting() {
        Map<String, String> settings = Map.of(
            "MAX_RETRY", "3",
            "TIMEOUT", "30"
        );
        
        AppConfig config = AppConfig.builder()
            .tenantId(UUID.randomUUID())
            .settings(settings)
            .build();
        
        assertEquals("3", config.getSetting("MAX_RETRY"));
        assertEquals("30", config.getSetting("TIMEOUT"));
        assertNull(config.getSetting("NONEXISTENT"));
    }
    
    @Test
    @DisplayName("Should get setting with default value")
    void shouldGetSettingWithDefault() {
        Map<String, String> settings = Map.of("KEY1", "value1");
        
        AppConfig config = AppConfig.builder()
            .tenantId(UUID.randomUUID())
            .settings(settings)
            .build();
        
        assertEquals("value1", config.getSetting("KEY1", "default"));
        assertEquals("default", config.getSetting("NONEXISTENT", "default"));
    }
    
    @Test
    @DisplayName("Should default active to true")
    void shouldDefaultActiveToTrue() {
        AppConfig config = AppConfig.builder()
            .tenantId(UUID.randomUUID())
            .build();
        
        assertTrue(config.isActive());
    }
}

@DisplayName("ProviderConfig Tests")
class ProviderConfigTest {
    
    @Test
    @DisplayName("Should create ProviderConfig with builder")
    void shouldCreateProviderConfigWithBuilder() {
        Map<String, Object> properties = Map.of("apiKey", "secret123", "timeout", 5000);
        
        ProviderConfig config = ProviderConfig.builder()
            .providerType("PAYMENT_GATEWAY")
            .implementation("stripe")
            .properties(properties)
            .enabled(true)
            .priority(10)
            .build();
        
        assertEquals("PAYMENT_GATEWAY", config.getProviderType());
        assertEquals("stripe", config.getImplementation());
        assertTrue(config.isEnabled());
        assertEquals(10, config.getPriority());
    }
    
    @Test
    @DisplayName("Should get property value")
    void shouldGetProperty() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("apiKey", "secret123");
        properties.put("timeout", 5000);
        properties.put("enabled", true);
        
        ProviderConfig config = ProviderConfig.builder()
            .providerType("TEST_PROVIDER")
            .properties(properties)
            .build();
        
        assertEquals("secret123", config.getProperty("apiKey"));
        assertEquals(5000, config.<Integer>getProperty("timeout"));
        assertEquals(true, config.getProperty("enabled"));
        assertNull(config.getProperty("nonexistent"));
    }
    
    @Test
    @DisplayName("Should get property with default value")
    void shouldGetPropertyWithDefault() {
        Map<String, Object> properties = Map.of("key1", "value1");
        
        ProviderConfig config = ProviderConfig.builder()
            .providerType("TEST_PROVIDER")
            .properties(properties)
            .build();
        
        assertEquals("value1", config.getProperty("key1", "default"));
        assertEquals("default", config.getProperty("nonexistent", "default"));
        assertEquals(100, config.getProperty("nonexistent", 100));
    }
    
    @Test
    @DisplayName("Should check if property exists")
    void shouldCheckHasProperty() {
        ProviderConfig config = ProviderConfig.builder()
            .providerType("TEST_PROVIDER")
            .properties(Map.of("key1", "value1"))
            .build();
        
        assertTrue(config.hasProperty("key1"));
        assertFalse(config.hasProperty("nonexistent"));
    }
    
    @Test
    @DisplayName("Should handle null properties gracefully")
    void shouldHandleNullProperties() {
        ProviderConfig config = ProviderConfig.builder()
            .providerType("TEST_PROVIDER")
            .build();
        
        assertNull(config.getProperty("anyKey"));
        assertFalse(config.hasProperty("anyKey"));
        assertEquals("default", config.getProperty("anyKey", "default"));
    }
    
    @Test
    @DisplayName("Should default enabled to true")
    void shouldDefaultEnabledToTrue() {
        ProviderConfig config = ProviderConfig.builder()
            .providerType("TEST_PROVIDER")
            .build();
        
        assertTrue(config.isEnabled());
    }
    
    @Test
    @DisplayName("Should default priority to zero")
    void shouldDefaultPriorityToZero() {
        ProviderConfig config = ProviderConfig.builder()
            .providerType("TEST_PROVIDER")
            .build();
        
        assertEquals(0, config.getPriority());
    }
}
