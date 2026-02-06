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

package org.fireflyframework.application.plugin;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a mapping from an API operation to a process plugin.
 * 
 * <p>ProcessMapping defines which process plugin should handle a specific
 * API operation for a given tenant, product, and channel combination.
 * This enables tenant-specific and product-specific customizations.</p>
 * 
 * <h3>Resolution Priority</h3>
 * <p>Mappings are resolved with the following priority (highest first):</p>
 * <ol>
 *   <li>Tenant + Product + Channel specific</li>
 *   <li>Tenant + Product specific</li>
 *   <li>Tenant + Channel specific</li>
 *   <li>Tenant specific</li>
 *   <li>Default (vanilla) mapping</li>
 * </ol>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Value
@Builder(toBuilder = true)
public class ProcessMapping {
    
    /**
     * Unique identifier for this mapping.
     */
    UUID id;
    
    /**
     * Tenant this mapping applies to (null for default/vanilla mapping).
     */
    UUID tenantId;
    
    /**
     * Product this mapping applies to (null for all products).
     */
    UUID productId;
    
    /**
     * Channel type this mapping applies to (null for all channels).
     * Example: "MOBILE", "WEB", "API", "BRANCH"
     */
    String channelType;
    
    /**
     * The API path this mapping applies to.
     * Example: "/api/v1/accounts"
     */
    String apiPath;
    
    /**
     * The HTTP method (GET, POST, PUT, DELETE).
     */
    String httpMethod;
    
    /**
     * The operation identifier within the API.
     * Example: "createAccount", "getAccount", "listAccounts"
     */
    String operationId;
    
    /**
     * The process plugin ID to execute.
     */
    String processId;
    
    /**
     * Optional specific version of the process to use.
     * If null, the latest version is used.
     */
    String processVersion;
    
    /**
     * Priority for ordering when multiple mappings match.
     * Lower values = higher priority.
     */
    @Builder.Default
    int priority = 0;
    
    /**
     * Whether this mapping is active.
     */
    @Builder.Default
    boolean active = true;
    
    /**
     * When this mapping becomes effective.
     */
    Instant effectiveFrom;
    
    /**
     * When this mapping expires.
     */
    Instant effectiveTo;
    
    /**
     * Additional configuration parameters for the process.
     */
    @Singular
    Map<String, Object> parameters;
    
    /**
     * Metadata about this mapping (created, updated, etc).
     */
    @Singular("metadata")
    Map<String, Object> metadata;
    
    /**
     * Checks if this is a default/vanilla mapping (no tenant specific).
     * 
     * @return true if this is a vanilla mapping
     */
    public boolean isVanilla() {
        return tenantId == null;
    }
    
    /**
     * Checks if this mapping applies to a specific product.
     * 
     * @return true if product-specific
     */
    public boolean isProductSpecific() {
        return productId != null;
    }
    
    /**
     * Checks if this mapping applies to a specific channel.
     * 
     * @return true if channel-specific
     */
    public boolean isChannelSpecific() {
        return channelType != null && !channelType.isEmpty();
    }
    
    /**
     * Checks if this mapping is currently effective based on time constraints.
     * 
     * @return true if currently effective
     */
    public boolean isCurrentlyEffective() {
        if (!active) {
            return false;
        }
        Instant now = Instant.now();
        if (effectiveFrom != null && now.isBefore(effectiveFrom)) {
            return false;
        }
        if (effectiveTo != null && now.isAfter(effectiveTo)) {
            return false;
        }
        return true;
    }
    
    /**
     * Gets a parameter value.
     * 
     * @param key the parameter key
     * @param type the expected type
     * @param <T> the type parameter
     * @return the parameter value, or null if not present
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key, Class<T> type) {
        if (parameters == null) {
            return null;
        }
        Object value = parameters.get(key);
        return type.isInstance(value) ? (T) value : null;
    }
    
    /**
     * Gets a parameter with a default value.
     * 
     * @param key the parameter key
     * @param defaultValue the default if not present
     * @param <T> the type parameter
     * @return the parameter value or default
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key, T defaultValue) {
        if (parameters == null) {
            return defaultValue;
        }
        Object value = parameters.get(key);
        return value != null ? (T) value : defaultValue;
    }
    
    /**
     * Calculates the specificity score for this mapping.
     * Higher score = more specific (higher priority).
     * 
     * @return the specificity score
     */
    public int getSpecificityScore() {
        int score = 0;
        if (tenantId != null) score += 100;
        if (productId != null) score += 10;
        if (channelType != null) score += 1;
        return score;
    }
    
    /**
     * Creates a vanilla/default mapping.
     * 
     * @param operationId the operation ID
     * @param processId the default process ID
     * @return a vanilla ProcessMapping
     */
    public static ProcessMapping vanilla(String operationId, String processId) {
        return ProcessMapping.builder()
                .operationId(operationId)
                .processId(processId)
                .build();
    }
    
    /**
     * Creates a tenant-specific mapping.
     * 
     * @param tenantId the tenant ID
     * @param operationId the operation ID
     * @param processId the process ID
     * @return a tenant-specific ProcessMapping
     */
    public static ProcessMapping forTenant(UUID tenantId, String operationId, String processId) {
        return ProcessMapping.builder()
                .tenantId(tenantId)
                .operationId(operationId)
                .processId(processId)
                .build();
    }
}
