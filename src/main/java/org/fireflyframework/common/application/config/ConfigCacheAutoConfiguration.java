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

import org.fireflyframework.cache.config.CacheAutoConfiguration;
import org.fireflyframework.cache.core.CacheType;
import org.fireflyframework.cache.factory.CacheManagerFactory;
import org.fireflyframework.cache.manager.FireflyCacheManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.time.Duration;

/**
 * Auto-configuration for application configuration cache.
 * <p>
 * Creates a dedicated cache manager for tenant/application configuration data.
 * <p>
 * The configuration cache uses:
 * <ul>
 *   <li>Key prefix: {@code firefly:application:config}</li>
 *   <li>TTL: 1 hour (tenant configurations are relatively stable)</li>
 *   <li>Preferred type: REDIS (for distributed config management)</li>
 *   <li>Fallback: Caffeine (for single-instance deployments)</li>
 * </ul>
 */
@AutoConfiguration
@AutoConfigureAfter(CacheAutoConfiguration.class)
@ConditionalOnClass({FireflyCacheManager.class, CacheManagerFactory.class})
@Slf4j
public class ConfigCacheAutoConfiguration {

    private static final String CONFIG_CACHE_KEY_PREFIX = "firefly:application:config";
    private static final Duration CONFIG_CACHE_TTL = Duration.ofHours(1);

    public ConfigCacheAutoConfiguration() {
        log.info("ConfigCacheAutoConfiguration loaded");
    }

    /**
     * Creates a dedicated cache manager for application/tenant configuration.
     * <p>
     * This cache manager is independent from other application caches,
     * providing isolation for configuration data.
     *
     * @param factory the cache manager factory
     * @return a dedicated cache manager for configurations
     */
    @Bean("configCacheManager")
    @ConditionalOnBean(CacheManagerFactory.class)
    @ConditionalOnMissingBean(name = "configCacheManager")
    public FireflyCacheManager configCacheManager(CacheManagerFactory factory) {
        String description = String.format(
                "Application Configuration Cache - Stores tenant/app configs (TTL: %d hour)",
                CONFIG_CACHE_TTL.toHours()
        );

        // Use AUTO to select the best available provider (Redis, Hazelcast, JCache, or Caffeine)
        return factory.createCacheManager(
                "app-config",
                CacheType.AUTO,
                CONFIG_CACHE_KEY_PREFIX,
                CONFIG_CACHE_TTL,
                description,
                "lib-common-application.ConfigCacheAutoConfiguration"
        );
    }
}
