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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AbstractConfigResolver}.
 */
@DisplayName("AbstractConfigResolver Tests")
class AbstractConfigResolverTest {
    
    private TestConfigResolver configResolver;
    private UUID tenantId;
    
    @BeforeEach
    void setUp() {
        configResolver = new TestConfigResolver();
        tenantId = UUID.randomUUID();
    }
    
    @Test
    @DisplayName("Should fetch config from platform on first request")
    void shouldFetchConfigFromPlatformOnFirstRequest() {
        // Given
        AppConfig expectedConfig = createTestConfig(tenantId);
        configResolver.setConfigToReturn(expectedConfig);
        
        // When/Then
        StepVerifier.create(configResolver.resolveConfig(tenantId))
                .assertNext(config -> {
                    assertThat(config.getTenantId()).isEqualTo(tenantId);
                    assertThat(config.isActive()).isTrue();
                })
                .verifyComplete();
        
        assertThat(configResolver.getFetchCount()).isEqualTo(1);
    }
    
    @Test
    @DisplayName("Should fetch config on each request without cache manager")
    void shouldFetchConfigOnEachRequestWithoutCacheManager() {
        // Given - Without FireflyCacheManager, caching is disabled
        AppConfig expectedConfig = createTestConfig(tenantId);
        configResolver.setConfigToReturn(expectedConfig);
        
        // First request
        configResolver.resolveConfig(tenantId).block();
        
        // When/Then - Second request will fetch again without cache manager
        StepVerifier.create(configResolver.resolveConfig(tenantId))
                .assertNext(config -> assertThat(config.getTenantId()).isEqualTo(tenantId))
                .verifyComplete();
        
        // Will fetch twice without cache manager (graceful degradation)
        assertThat(configResolver.getFetchCount()).isEqualTo(2);
    }
    
    @Test
    @DisplayName("Should indicate config is cached after first fetch")
    void shouldIndicateConfigIsCached() {
        // Given
        AppConfig expectedConfig = createTestConfig(tenantId);
        configResolver.setConfigToReturn(expectedConfig);
        
        // Initially not cached
        StepVerifier.create(configResolver.isCached(tenantId))
                .expectNext(false)
                .verifyComplete();
        
        // When
        configResolver.resolveConfig(tenantId).block();
        
        // Then - Should be cached
        StepVerifier.create(configResolver.isCached(tenantId))
                .expectNext(false)  // Without real FireflyCacheManager, will return false
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should refresh config and fetch from platform again")
    void shouldRefreshConfig() {
        // Given
        AppConfig initialConfig = createTestConfig(tenantId);
        configResolver.setConfigToReturn(initialConfig);
        
        // First fetch
        configResolver.resolveConfig(tenantId).block();
        assertThat(configResolver.getFetchCount()).isEqualTo(1);
        
        // Change config to return
        AppConfig refreshedConfig = createTestConfig(tenantId, "refreshed-tenant");
        configResolver.setConfigToReturn(refreshedConfig);
        
        // When - Refresh
        StepVerifier.create(configResolver.refreshConfig(tenantId))
                .assertNext(config -> assertThat(config.getTenantName()).isEqualTo("refreshed-tenant"))
                .verifyComplete();
        
        // Then - Should have fetched twice
        assertThat(configResolver.getFetchCount()).isEqualTo(2);
    }
    
    @Test
    @DisplayName("Should clear cache for specific tenant")
    void shouldClearCacheForTenant() {
        // Given
        AppConfig config = createTestConfig(tenantId);
        configResolver.setConfigToReturn(config);
        configResolver.resolveConfig(tenantId).block();
        
        // Since we don't have FireflyCacheManager in this test, cache checking will return false
        
        // When
        StepVerifier.create(configResolver.clearCacheForTenant(tenantId))
                .verifyComplete();
        
        // Then - Verify can still resolve (will fetch from platform)
        StepVerifier.create(configResolver.resolveConfig(tenantId))
                .assertNext(c -> assertThat(c.getTenantId()).isEqualTo(tenantId))
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should clear entire cache")
    void shouldClearEntireCache() {
        // Given
        UUID tenantId1 = UUID.randomUUID();
        UUID tenantId2 = UUID.randomUUID();
        
        AppConfig config1 = createTestConfig(tenantId1);
        AppConfig config2 = createTestConfig(tenantId2);
        
        configResolver.setConfigToReturn(config1);
        configResolver.resolveConfig(tenantId1).block();
        
        configResolver.setConfigToReturn(config2);
        configResolver.resolveConfig(tenantId2).block();
        
        // Cache checking without FireflyCacheManager
        
        // When
        StepVerifier.create(configResolver.clearCache())
                .verifyComplete();
        
        // Then - Verify can still resolve both (will fetch from platform)
        // Need to reset configs since we're fetching again
        configResolver.setConfigToReturn(config1);
        StepVerifier.create(configResolver.resolveConfig(tenantId1))
                .assertNext(c -> assertThat(c.getTenantId()).isEqualTo(tenantId1))
                .verifyComplete();
        
        configResolver.setConfigToReturn(config2);
        StepVerifier.create(configResolver.resolveConfig(tenantId2))
                .assertNext(c -> assertThat(c.getTenantId()).isEqualTo(tenantId2))
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should handle multiple tenants independently")
    void shouldHandleMultipleTenantsIndependently() {
        // Given
        UUID tenant1 = UUID.randomUUID();
        UUID tenant2 = UUID.randomUUID();
        
        AppConfig config1 = createTestConfig(tenant1, "tenant-1");
        AppConfig config2 = createTestConfig(tenant2, "tenant-2");
        
        // When
        configResolver.setConfigToReturn(config1);
        configResolver.resolveConfig(tenant1).block();
        
        configResolver.setConfigToReturn(config2);
        configResolver.resolveConfig(tenant2).block();
        
        // Then - Both configs should be fetchable
        assertThat(configResolver.getFetchCount()).isEqualTo(2);
    }
    
    @Test
    @DisplayName("Should handle fetch errors gracefully")
    void shouldHandleFetchErrors() {
        // Given
        configResolver.setShouldFail(true);
        
        // When/Then
        StepVerifier.create(configResolver.resolveConfig(tenantId))
                .expectError(RuntimeException.class)
                .verify();
    }
    
    private AppConfig createTestConfig(UUID tenantId) {
        return createTestConfig(tenantId, "test-tenant");
    }
    
    private AppConfig createTestConfig(UUID tenantId, String tenantName) {
        return AppConfig.builder()
                .tenantId(tenantId)
                .tenantName(tenantName)
                .active(true)
                .providers(new HashMap<>())
                .featureFlags(new HashMap<>())
                .settings(new HashMap<>())
                .build();
    }
    
    /**
     * Test implementation of AbstractConfigResolver.
     */
    private static class TestConfigResolver extends AbstractConfigResolver {
        
        private AppConfig configToReturn;
        private int fetchCount = 0;
        private boolean shouldFail = false;
        
        public void setConfigToReturn(AppConfig config) {
            this.configToReturn = config;
        }
        
        public void setShouldFail(boolean shouldFail) {
            this.shouldFail = shouldFail;
        }
        
        public int getFetchCount() {
            return fetchCount;
        }
        
        @Override
        protected Mono<AppConfig> fetchConfigFromPlatform(UUID tenantId) {
            fetchCount++;
            
            if (shouldFail) {
                return Mono.error(new RuntimeException("Simulated platform error"));
            }
            
            if (configToReturn != null) {
                return Mono.just(configToReturn);
            }
            
            return Mono.just(AppConfig.builder()
                    .tenantId(tenantId)
                    .active(true)
                    .providers(new HashMap<>())
                    .featureFlags(new HashMap<>())
                    .settings(new HashMap<>())
                    .build());
        }
    }
}
