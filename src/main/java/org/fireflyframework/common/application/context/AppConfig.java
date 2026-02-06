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

package org.fireflyframework.application.context;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Immutable configuration container for application-level settings.
 * Contains tenant-specific configuration and provider settings.
 * 
 * <p>This class represents the configuration context for multi-tenant applications,
 * including provider-specific configurations that may vary per tenant.</p>
 * 
 * <p>Configuration data is retrieved from common-platform-config-mgmt.</p>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Value
@Builder(toBuilder = true)
@With
public class AppConfig {
    
    /**
     * Unique identifier of the tenant/organization.
     * This comes from common-platform-config-mgmt.
     */
    @NotNull
    UUID tenantId;
    
    /**
     * Display name of the tenant
     */
    String tenantName;
    
    /**
     * Map of provider configurations for this tenant.
     * Key: Provider name/type (e.g., "PAYMENT_GATEWAY", "KYC_PROVIDER")
     * Value: Provider-specific configuration as a map
     */
    Map<String, ProviderConfig> providers;
    
    /**
     * Feature flags for this tenant
     */
    Map<String, Boolean> featureFlags;
    
    /**
     * Tenant-specific settings
     */
    Map<String, String> settings;
    
    /**
     * Environment-specific configuration
     */
    String environment;
    
    /**
     * Whether the tenant is active
     */
    @Builder.Default
    boolean active = true;
    
    /**
     * Gets a provider configuration by provider type
     * 
     * @param providerType the type of provider
     * @return Optional containing the provider config if found
     */
    public Optional<ProviderConfig> getProvider(String providerType) {
        return Optional.ofNullable(providers != null ? providers.get(providerType) : null);
    }
    
    /**
     * Checks if a provider is configured for this tenant
     * 
     * @param providerType the type of provider
     * @return true if the provider is configured
     */
    public boolean hasProvider(String providerType) {
        return providers != null && providers.containsKey(providerType);
    }
    
    /**
     * Checks if a feature flag is enabled
     * 
     * @param feature the feature flag name
     * @return true if the feature is enabled, false otherwise
     */
    public boolean isFeatureEnabled(String feature) {
        return featureFlags != null && Boolean.TRUE.equals(featureFlags.get(feature));
    }
    
    /**
     * Gets a setting value
     * 
     * @param key the setting key
     * @return the setting value or null if not found
     */
    public String getSetting(String key) {
        return settings != null ? settings.get(key) : null;
    }
    
    /**
     * Gets a setting value with a default
     * 
     * @param key the setting key
     * @param defaultValue the default value to return if not found
     * @return the setting value or default
     */
    public String getSetting(String key, String defaultValue) {
        String value = getSetting(key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Immutable provider configuration
     */
    @Value
    @Builder(toBuilder = true)
    @With
    public static class ProviderConfig {
        
        /**
         * Provider type/name
         */
        @NotNull
        String providerType;
        
        /**
         * Provider implementation class or identifier
         */
        String implementation;
        
        /**
         * Provider-specific configuration properties
         */
        Map<String, Object> properties;
        
        /**
         * Whether this provider is enabled
         */
        @Builder.Default
        boolean enabled = true;
        
        /**
         * Provider priority (for cases where multiple providers of same type exist)
         */
        @Builder.Default
        int priority = 0;
        
        /**
         * Gets a property value
         * 
         * @param key the property key
         * @param <T> the expected type
         * @return the property value or null if not found
         */
        @SuppressWarnings("unchecked")
        public <T> T getProperty(String key) {
            return properties != null ? (T) properties.get(key) : null;
        }
        
        /**
         * Gets a property value with a default
         * 
         * @param key the property key
         * @param defaultValue the default value
         * @param <T> the expected type
         * @return the property value or default
         */
        @SuppressWarnings("unchecked")
        public <T> T getProperty(String key, T defaultValue) {
            T value = getProperty(key);
            return value != null ? value : defaultValue;
        }
        
        /**
         * Checks if a property exists
         * 
         * @param key the property key
         * @return true if the property exists
         */
        public boolean hasProperty(String key) {
            return properties != null && properties.containsKey(key);
        }
    }
}
