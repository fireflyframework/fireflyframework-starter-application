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

package org.fireflyframework.application.resolver;

import org.fireflyframework.application.context.AppConfig;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Interface for resolving tenant configuration.
 * Implementations fetch configuration from common-platform-config-mgmt.
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
public interface ConfigResolver {
    
    /**
     * Resolves configuration for the specified tenant.
     * 
     * @param tenantId the tenant ID
     * @return Mono of AppConfig
     */
    Mono<AppConfig> resolveConfig(UUID tenantId);
    
    /**
     * Refreshes cached configuration for a tenant.
     * 
     * @param tenantId the tenant ID
     * @return Mono of refreshed AppConfig
     */
    Mono<AppConfig> refreshConfig(UUID tenantId);
    
    /**
     * Checks if configuration is cached for a tenant.
     * 
     * @param tenantId the tenant ID
     * @return Mono that emits true if configuration is cached
     */
    Mono<Boolean> isCached(UUID tenantId);
}
