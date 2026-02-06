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
import org.fireflyframework.cache.manager.FireflyCacheManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

/**
 * Abstract base implementation of ConfigResolver with caching support using dedicated config cache.
 * Integrates with common-platform-config-mgmt-sdk to fetch tenant configuration.
 * 
 * <p>This implementation uses a dedicated configCacheManager from ConfigCacheAutoConfiguration
 * to avoid cache key collisions with other application caches.</p>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Slf4j
public abstract class AbstractConfigResolver implements ConfigResolver {
    
    private static final String CACHE_KEY_PREFIX = "firefly:application:config:";
    private static final Duration DEFAULT_CONFIG_TTL = Duration.ofHours(1);
    
    @Autowired(required = false)
    @Qualifier("configCacheManager")
    private FireflyCacheManager cacheManager;
    
    @Override
    public Mono<AppConfig> resolveConfig(UUID tenantId) {
        log.debug("Resolving configuration for tenant: {}", tenantId);
        
        // If cache is not available, fetch directly
        if (cacheManager == null) {
            log.debug("FireflyCacheManager not available, fetching config directly");
            return fetchConfigFromPlatform(tenantId);
        }
        
        String cacheKey = getCacheKey(tenantId);
        
        // Check cache first
        return cacheManager.get(cacheKey, AppConfig.class)
                .flatMap(cached -> {
                    if (cached.isPresent()) {
                        log.debug("Configuration found in cache for tenant: {}", tenantId);
                        return Mono.just(cached.get());
                    }
                    
                    // Cache miss - fetch from platform and cache
                    return fetchConfigFromPlatform(tenantId)
                            .flatMap(config -> cacheManager.put(cacheKey, config, getConfigTTL())
                                    .doOnSuccess(v -> log.debug("Cached configuration for tenant: {} with TTL: {}", 
                                            tenantId, getConfigTTL()))
                                    .thenReturn(config))
                            .doOnError(error -> log.error("Failed to resolve config for tenant: {}", tenantId, error));
                });
    }
    
    @Override
    public Mono<AppConfig> refreshConfig(UUID tenantId) {
        log.debug("Refreshing configuration for tenant: {}", tenantId);
        
        if (cacheManager == null) {
            log.debug("FireflyCacheManager not available, fetching config directly");
            return fetchConfigFromPlatform(tenantId);
        }
        
        String cacheKey = getCacheKey(tenantId);
        
        // Evict from cache and fetch fresh
        return cacheManager.evict(cacheKey)
                .doOnNext(evicted -> {
                    if (evicted) {
                        log.debug("Evicted cached configuration for tenant: {}", tenantId);
                    }
                })
                .then(resolveConfig(tenantId));
    }
    
    @Override
    public Mono<Boolean> isCached(UUID tenantId) {
        if (cacheManager == null) {
            return Mono.just(false);
        }
        
        String cacheKey = getCacheKey(tenantId);
        return cacheManager.exists(cacheKey);
    }
    
    /**
     * Fetches tenant configuration from the platform config management service.
     * 
     * <p>TODO: Implementation should use common-platform-config-mgmt-sdk to fetch
     * the tenant configuration including provider settings, feature flags, and
     * tenant-specific settings.</p>
     * 
     * @param tenantId the tenant ID
     * @return Mono of AppConfig
     */
    protected Mono<AppConfig> fetchConfigFromPlatform(UUID tenantId) {
        // TODO: Implement configuration fetching using platform SDK
        // Example:
        // return configManagementClient.getTenantConfig(tenantId)
        //     .map(response -> AppConfig.builder()
        //         .tenantId(response.getTenantId())
        //         .tenantName(response.getName())
        //         .providers(convertProviders(response.getProviders()))
        //         .featureFlags(response.getFeatureFlags())
        //         .settings(response.getSettings())
        //         .environment(response.getEnvironment())
        //         .active(response.isActive())
        //         .build());
        
        log.warn("fetchConfigFromPlatform not implemented, returning empty config for tenant: {}", tenantId);
        return Mono.just(AppConfig.builder()
                .tenantId(tenantId)
                .active(true)
                .build());
    }
    
    /**
     * Clears the entire configuration cache.
     * Useful for testing or when a full refresh is needed.
     * 
     * @return Mono that completes when cache is cleared
     */
    protected Mono<Void> clearCache() {
        if (cacheManager == null) {
            log.debug("FireflyCacheManager not available, nothing to clear");
            return Mono.empty();
        }
        
        return cacheManager.clear()
                .doOnSuccess(v -> log.info("Configuration cache cleared"));
    }
    
    /**
     * Clears cache for a specific tenant.
     * 
     * @param tenantId the tenant ID
     * @return Mono that completes when cache entry is evicted
     */
    protected Mono<Void> clearCacheForTenant(UUID tenantId) {
        if (cacheManager == null) {
            log.debug("FireflyCacheManager not available, nothing to clear");
            return Mono.empty();
        }
        
        String cacheKey = getCacheKey(tenantId);
        return cacheManager.evict(cacheKey)
                .doOnNext(evicted -> {
                    if (evicted) {
                        log.debug("Configuration cache cleared for tenant: {}", tenantId);
                    }
                })
                .then();
    }
    
    /**
     * Gets the cache key for a tenant configuration.
     * 
     * <p>Cache keys follow the Firefly naming convention:</p>
     * <ul>
     *   <li>Format: {@code firefly:application:config:{tenantId}}</li>
     *   <li>Example: {@code firefly:application:config:123e4567-e89b-12d3-a456-426614174000}</li>
     * </ul>
     * 
     * @param tenantId the tenant ID
     * @return the cache key following Firefly conventions
     */
    private String getCacheKey(UUID tenantId) {
        return CACHE_KEY_PREFIX + tenantId.toString();
    }
    
    /**
     * Gets the TTL duration for cached configurations.
     * Subclasses can override this to provide custom TTL values.
     * 
     * @return the TTL duration
     */
    protected Duration getConfigTTL() {
        return DEFAULT_CONFIG_TTL;
    }
}
