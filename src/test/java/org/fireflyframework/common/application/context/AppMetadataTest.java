package org.fireflyframework.application.context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AppMetadata Tests")
class AppMetadataTest {
    
    @Test
    @DisplayName("Should create AppMetadata with builder")
    void shouldCreateAppMetadataWithBuilder() {
        AppMetadata metadata = AppMetadata.builder()
            .name("test-service")
            .version("1.0.0")
            .description("Test service description")
            .displayName("Test Service")
            .domain("test")
            .team("test-team")
            .owners(Set.of("owner@getfirefly.io"))
            .build();
        
        assertEquals("test-service", metadata.getName());
        assertEquals("1.0.0", metadata.getVersion());
        assertEquals("Test service description", metadata.getDescription());
        assertEquals("Test Service", metadata.getDisplayName());
        assertEquals("test", metadata.getDomain());
        assertEquals("test-team", metadata.getTeam());
        assertTrue(metadata.getOwners().contains("owner@getfirefly.io"));
    }
    
    @Test
    @DisplayName("Should return displayName when present")
    void shouldReturnEffectiveDisplayName() {
        AppMetadata withDisplayName = AppMetadata.builder()
            .name("service-name")
            .displayName("Service Display Name")
            .build();
        
        assertEquals("Service Display Name", withDisplayName.getEffectiveDisplayName());
        
        AppMetadata withoutDisplayName = AppMetadata.builder()
            .name("service-name")
            .build();
        
        assertEquals("service-name", withoutDisplayName.getEffectiveDisplayName());
    }
    
    @Test
    @DisplayName("Should detect production environment")
    void shouldDetectProductionEnvironment() {
        AppMetadata prod1 = AppMetadata.builder()
            .name("test")
            .environment("prod")
            .build();
        
        AppMetadata prod2 = AppMetadata.builder()
            .name("test")
            .environment("production")
            .build();
        
        AppMetadata dev = AppMetadata.builder()
            .name("test")
            .environment("dev")
            .build();
        
        assertTrue(prod1.isProduction());
        assertTrue(prod2.isProduction());
        assertFalse(dev.isProduction());
    }
    
    @Test
    @DisplayName("Should get build info by key")
    void shouldGetBuildInfo() {
        Map<String, String> buildInfo = Map.of(
            "git.commit", "abc123",
            "build.time", "2025-01-15T10:00:00Z"
        );
        
        AppMetadata metadata = AppMetadata.builder()
            .name("test")
            .buildInfo(buildInfo)
            .build();
        
        assertEquals("abc123", metadata.getBuildInfo("git.commit"));
        assertEquals("2025-01-15T10:00:00Z", metadata.getBuildInfo("build.time"));
        assertNull(metadata.getBuildInfo("nonexistent"));
    }
    
    @Test
    @DisplayName("Should get custom property")
    void shouldGetCustomProperty() {
        Map<String, Object> customProps = Map.of(
            "custom1", "value1",
            "custom2", 123
        );
        
        AppMetadata metadata = AppMetadata.builder()
            .name("test")
            .customProperties(customProps)
            .build();
        
        assertEquals("value1", metadata.getCustomProperty("custom1"));
        assertEquals(123, metadata.getCustomProperty("custom2"));
        assertNull(metadata.getCustomProperty("nonexistent"));
    }
    
    @Test
    @DisplayName("Should create display string")
    void shouldCreateDisplayString() {
        AppMetadata metadata = AppMetadata.builder()
            .name("test-service")
            .displayName("Test Service")
            .version("1.0.0")
            .domain("test")
            .description("A test service")
            .build();
        
        String display = metadata.toDisplayString();
        
        assertTrue(display.contains("Test Service"));
        assertTrue(display.contains("1.0.0"));
        assertTrue(display.contains("test"));
        assertTrue(display.contains("A test service"));
    }
    
    @Test
    @DisplayName("Should handle null startup time")
    void shouldHandleNullStartupTime() {
        AppMetadata metadata = AppMetadata.builder()
            .name("test")
            .build();
        
        assertNull(metadata.getStartupTime());
    }
    
    @Test
    @DisplayName("Should store startup time")
    void shouldStoreStartupTime() {
        Instant now = Instant.now();
        
        AppMetadata metadata = AppMetadata.builder()
            .name("test")
            .startupTime(now)
            .build();
        
        assertEquals(now, metadata.getStartupTime());
    }
    
    @Test
    @DisplayName("Should handle critical and deprecated flags")
    void shouldHandleCriticalAndDeprecatedFlags() {
        AppMetadata critical = AppMetadata.builder()
            .name("test")
            .critical(true)
            .build();
        
        AppMetadata deprecated = AppMetadata.builder()
            .name("test")
            .deprecated(true)
            .deprecationMessage("Use v2 instead")
            .build();
        
        assertTrue(critical.isCritical());
        assertFalse(critical.isDeprecated());
        
        assertFalse(deprecated.isCritical());
        assertTrue(deprecated.isDeprecated());
        assertEquals("Use v2 instead", deprecated.getDeprecationMessage());
    }
}
