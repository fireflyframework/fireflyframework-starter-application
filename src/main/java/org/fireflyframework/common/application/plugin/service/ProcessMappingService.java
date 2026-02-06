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

package org.fireflyframework.application.plugin.service;

import org.fireflyframework.application.plugin.ProcessMapping;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Service for resolving API operations to process plugins.
 * 
 * <p>This service integrates with the Configuration Management system to
 * determine which process plugin should handle a specific API operation
 * based on tenant, product, and channel configuration.</p>
 * 
 * <h3>Resolution Priority</h3>
 * <ol>
 *   <li>Tenant + Product + Channel specific mapping</li>
 *   <li>Tenant + Product specific mapping</li>
 *   <li>Tenant + Channel specific mapping</li>
 *   <li>Tenant specific mapping</li>
 *   <li>Default (vanilla) mapping</li>
 * </ol>
 * 
 * <h3>Implementation Notes</h3>
 * <p>The default implementation in {@code PluginAutoConfiguration} returns a
 * vanilla mapping that uses the operationId as the processId. Applications
 * integrating with {@code common-platform-config-mgmt} should provide a custom
 * implementation that queries the {@code api_process_mappings} table.</p>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 * @see ProcessMapping
 * @see ProcessPluginExecutor
 */
public interface ProcessMappingService {
    
    /**
     * Resolves the process mapping for an operation.
     * 
     * @param tenantId the tenant ID (from ApplicationExecutionContext)
     * @param operationId the API operation identifier (e.g., "createAccount")
     * @param productId the product ID (may be null)
     * @param channelType the channel type (may be null)
     * @return Mono emitting the resolved ProcessMapping
     */
    Mono<ProcessMapping> resolveMapping(UUID tenantId, String operationId, UUID productId, String channelType);
    
    /**
     * Resolves the process mapping using minimal parameters.
     * 
     * @param tenantId the tenant ID
     * @param operationId the operation ID
     * @return Mono emitting the resolved ProcessMapping
     */
    default Mono<ProcessMapping> resolveMapping(UUID tenantId, String operationId) {
        return resolveMapping(tenantId, operationId, null, null);
    }
    
    /**
     * Resolves the process mapping with product context.
     * 
     * @param tenantId the tenant ID
     * @param operationId the operation ID
     * @param productId the product ID
     * @return Mono emitting the resolved ProcessMapping
     */
    default Mono<ProcessMapping> resolveMapping(UUID tenantId, String operationId, UUID productId) {
        return resolveMapping(tenantId, operationId, productId, null);
    }
    
    /**
     * Invalidates the cache for a specific tenant.
     * 
     * <p>Called when process mappings are updated in the configuration system.</p>
     * 
     * @param tenantId the tenant ID, or null to invalidate all
     * @return Mono that completes when invalidation is done
     */
    Mono<Void> invalidateCache(UUID tenantId);
    
    /**
     * Invalidates the entire mapping cache.
     * 
     * @return Mono that completes when invalidation is done
     */
    default Mono<Void> invalidateAllCache() {
        return invalidateCache(null);
    }
}
