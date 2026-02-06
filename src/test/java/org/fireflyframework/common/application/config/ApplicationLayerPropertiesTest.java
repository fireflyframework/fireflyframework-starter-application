package org.fireflyframework.application.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ApplicationLayerProperties Tests")
class ApplicationLayerPropertiesTest {
    
    @Test
    @DisplayName("Should create properties with defaults")
    void shouldCreatePropertiesWithDefaults() {
        ApplicationLayerProperties properties = new ApplicationLayerProperties();
        
        assertNotNull(properties.getSecurity());
        assertNotNull(properties.getContext());
        assertNotNull(properties.getConfig());
    }
    
    @Test
    @DisplayName("Should have default security settings")
    void shouldHaveDefaultSecuritySettings() {
        ApplicationLayerProperties properties = new ApplicationLayerProperties();
        ApplicationLayerProperties.Security security = properties.getSecurity();
        
        assertTrue(security.isEnabled());
        assertTrue(security.isUseSecurityCenter());
        assertNotNull(security.getDefaultRoles());
        assertEquals(0, security.getDefaultRoles().length);
        assertFalse(security.isFailOnMissing());
    }
    
    @Test
    @DisplayName("Should have default context settings")
    void shouldHaveDefaultContextSettings() {
        ApplicationLayerProperties properties = new ApplicationLayerProperties();
        ApplicationLayerProperties.Context context = properties.getContext();
        
        assertTrue(context.isCacheEnabled());
        assertEquals(300, context.getCacheTtl());
        assertEquals(1000, context.getCacheMaxSize());
    }
    
    @Test
    @DisplayName("Should have default config settings")
    void shouldHaveDefaultConfigSettings() {
        ApplicationLayerProperties properties = new ApplicationLayerProperties();
        ApplicationLayerProperties.Config config = properties.getConfig();
        
        assertTrue(config.isCacheEnabled());
        assertEquals(600, config.getCacheTtl());
        assertFalse(config.isRefreshOnStartup());
    }
    
    @Test
    @DisplayName("Should allow custom security settings")
    void shouldAllowCustomSecuritySettings() {
        ApplicationLayerProperties properties = new ApplicationLayerProperties();
        ApplicationLayerProperties.Security security = properties.getSecurity();
        
        security.setEnabled(false);
        security.setUseSecurityCenter(false);
        security.setDefaultRoles(new String[]{"USER", "GUEST"});
        security.setFailOnMissing(true);
        
        assertFalse(security.isEnabled());
        assertFalse(security.isUseSecurityCenter());
        assertArrayEquals(new String[]{"USER", "GUEST"}, security.getDefaultRoles());
        assertTrue(security.isFailOnMissing());
    }
    
    @Test
    @DisplayName("Should allow custom context settings")
    void shouldAllowCustomContextSettings() {
        ApplicationLayerProperties properties = new ApplicationLayerProperties();
        ApplicationLayerProperties.Context context = properties.getContext();
        
        context.setCacheEnabled(false);
        context.setCacheTtl(60);
        context.setCacheMaxSize(500);
        
        assertFalse(context.isCacheEnabled());
        assertEquals(60, context.getCacheTtl());
        assertEquals(500, context.getCacheMaxSize());
    }
    
    @Test
    @DisplayName("Should allow custom config settings")
    void shouldAllowCustomConfigSettings() {
        ApplicationLayerProperties properties = new ApplicationLayerProperties();
        ApplicationLayerProperties.Config config = properties.getConfig();
        
        config.setCacheEnabled(false);
        config.setCacheTtl(120);
        config.setRefreshOnStartup(true);
        
        assertFalse(config.isCacheEnabled());
        assertEquals(120, config.getCacheTtl());
        assertTrue(config.isRefreshOnStartup());
    }
    
    @Test
    @DisplayName("Should allow replacing entire security object")
    void shouldAllowReplacingEntireSecurityObject() {
        ApplicationLayerProperties properties = new ApplicationLayerProperties();
        ApplicationLayerProperties.Security newSecurity = new ApplicationLayerProperties.Security();
        
        newSecurity.setEnabled(false);
        properties.setSecurity(newSecurity);
        
        assertSame(newSecurity, properties.getSecurity());
        assertFalse(properties.getSecurity().isEnabled());
    }
    
    @Test
    @DisplayName("Should allow replacing entire context object")
    void shouldAllowReplacingEntireContextObject() {
        ApplicationLayerProperties properties = new ApplicationLayerProperties();
        ApplicationLayerProperties.Context newContext = new ApplicationLayerProperties.Context();
        
        newContext.setCacheEnabled(false);
        properties.setContext(newContext);
        
        assertSame(newContext, properties.getContext());
        assertFalse(properties.getContext().isCacheEnabled());
    }
    
    @Test
    @DisplayName("Should allow replacing entire config object")
    void shouldAllowReplacingEntireConfigObject() {
        ApplicationLayerProperties properties = new ApplicationLayerProperties();
        ApplicationLayerProperties.Config newConfig = new ApplicationLayerProperties.Config();
        
        newConfig.setRefreshOnStartup(true);
        properties.setConfig(newConfig);
        
        assertSame(newConfig, properties.getConfig());
        assertTrue(properties.getConfig().isRefreshOnStartup());
    }
}
