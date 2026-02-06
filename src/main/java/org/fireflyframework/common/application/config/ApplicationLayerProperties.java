/*
 * Copyright 2024-2026 Firefly Software Solutions Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fireflyframework.application.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Application Layer.
 * 
 * <p>Configure in application.yml:</p>
 * <pre>
 * firefly:
 *   application:
 *     security:
 *       enabled: true
 *       use-security-center: true
 *     context:
 *       cache-enabled: true
 *       cache-ttl: 300
 * </pre>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "firefly.application")
public class ApplicationLayerProperties {
    
    /**
     * Security configuration
     */
    private Security security = new Security();
    
    /**
     * Context resolution configuration
     */
    private Context context = new Context();
    
    /**
     * Configuration management settings
     */
    private Config config = new Config();
    
    @Data
    public static class Security {
        /**
         * Whether security is enabled
         */
        private boolean enabled = true;
        
        /**
         * Whether to use SecurityCenter for authorization
         */
        private boolean useSecurityCenter = true;
        
        /**
         * Default roles when no explicit security is configured
         */
        private String[] defaultRoles = {};
        
        /**
         * Whether to fail on missing security configuration
         */
        private boolean failOnMissing = false;
    }
    
    @Data
    public static class Context {
        /**
         * Whether context caching is enabled
         */
        private boolean cacheEnabled = true;
        
        /**
         * Context cache TTL in seconds
         */
        private int cacheTtl = 300;
        
        /**
         * Maximum cache size
         */
        private int cacheMaxSize = 1000;
    }
    
    @Data
    public static class Config {
        /**
         * Whether config caching is enabled
         */
        private boolean cacheEnabled = true;
        
        /**
         * Config cache TTL in seconds
         */
        private int cacheTtl = 600;
        
        /**
         * Whether to refresh config on startup
         */
        private boolean refreshOnStartup = false;
    }
}
