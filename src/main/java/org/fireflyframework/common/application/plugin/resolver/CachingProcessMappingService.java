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

package org.fireflyframework.application.plugin.resolver;

import org.fireflyframework.application.plugin.ProcessMapping;
import org.fireflyframework.application.plugin.config.PluginProperties;
import org.fireflyframework.application.plugin.service.ProcessMappingService;
import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * ProcessMappingService implementation with caching.
 * 
 * <p>This service resolves API operation → process mappings from the config-mgmt
 * service and caches results for performance. Cache entries can be invalidated
 * per-tenant when configurations change.</p>
 * 
 * <h3>Resolution Strategy</h3>
 * <p>Mappings are resolved with the following priority:</p>
 * <ol>
 *   <li>Exact match: tenant + operation + product + channel</li>
 *   <li>Product match: tenant + operation + product</li>
 *   <li>Tenant match: tenant + operation</li>
 *   <li>Global default: operation only</li>
 * </ol>
 * 
 * <h3>Caching</h3>
 * <p>Uses Caffeine cache with configurable TTL and max entries. Background refresh
 * can be enabled to keep cache warm without blocking requests.</p>
 * 
 * <h3>Configuration</h3>
 * <pre>
 * firefly:
 *   application:
 *     plugin:
 *       cache:
 *         enabled: true
 *         ttl: PT1H
 *         max-entries: 1000
 *         background-refresh: true
 * </pre>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Slf4j
public class CachingProcessMappingService implements ProcessMappingService {
    
    private final WebClient configMgmtClient;
    private final PluginProperties properties;
    private final AsyncCache<MappingCacheKey, ProcessMapping> cache;
    private final Duration responseTimeout;
    
    /**
     * Base URL path for config-mgmt API process mappings.
     */
    private static final String MAPPINGS_API_PATH = "/api/v1/api-process-mappings";
    
    /**
     * Constructor that accepts a pre-configured WebClient.
     */
    public CachingProcessMappingService(
            WebClient configMgmtClient,
            PluginProperties properties) {
        this.configMgmtClient = configMgmtClient;
        this.properties = properties;
        this.responseTimeout = properties.getRemoteTimeout();
        
        // Build cache based on configuration
        PluginProperties.CacheProperties cacheConfig = properties.getCache();
        
        Caffeine<Object, Object> cacheBuilder = Caffeine.newBuilder()
                .maximumSize(cacheConfig.getMaxEntries())
                .expireAfterWrite(cacheConfig.getTtl())
                .recordStats();
        
        if (cacheConfig.isBackgroundRefresh()) {
            cacheBuilder.refreshAfterWrite(cacheConfig.getTtl().dividedBy(2));
        }
        
        this.cache = cacheBuilder.buildAsync();
        
        log.info("Initialized CachingProcessMappingService: ttl={}, maxEntries={}, refresh={}, responseTimeout={}",
                cacheConfig.getTtl(), cacheConfig.getMaxEntries(), cacheConfig.isBackgroundRefresh(), responseTimeout);
    }
    
    /**
     * Constructor that creates a WebClient with timeout configuration from properties.
     * This is the preferred constructor for production use.
     */
    public CachingProcessMappingService(
            String configMgmtBaseUrl,
            PluginProperties properties) {
        this.properties = properties;
        this.responseTimeout = properties.getRemoteTimeout();
        
        // Create WebClient with timeout configuration
        Duration connectTimeout = properties.getRemoteTimeout();
        
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeout.toMillis())
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(connectTimeout.toMillis(), TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(connectTimeout.toMillis(), TimeUnit.MILLISECONDS)));
        
        this.configMgmtClient = WebClient.builder()
                .baseUrl(configMgmtBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
        
        // Build cache based on configuration
        PluginProperties.CacheProperties cacheConfig = properties.getCache();
        
        Caffeine<Object, Object> cacheBuilder = Caffeine.newBuilder()
                .maximumSize(cacheConfig.getMaxEntries())
                .expireAfterWrite(cacheConfig.getTtl())
                .recordStats();
        
        if (cacheConfig.isBackgroundRefresh()) {
            cacheBuilder.refreshAfterWrite(cacheConfig.getTtl().dividedBy(2));
        }
        
        this.cache = cacheBuilder.buildAsync();
        
        log.info("Initialized CachingProcessMappingService with configured timeouts: " +
                "ttl={}, maxEntries={}, refresh={}, connectTimeout={}",
                cacheConfig.getTtl(), cacheConfig.getMaxEntries(), cacheConfig.isBackgroundRefresh(), connectTimeout);
    }
    
    @Override
    public Mono<ProcessMapping> resolveMapping(
            UUID tenantId,
            String operationId,
            UUID productId,
            String channelType) {
        
        if (!properties.getCache().isEnabled()) {
            return fetchMapping(tenantId, operationId, productId, channelType);
        }
        
        MappingCacheKey cacheKey = new MappingCacheKey(tenantId, operationId, productId, channelType);
        
        return Mono.fromFuture(() -> cache.get(cacheKey, (key, executor) ->
                fetchMapping(key.tenantId, key.operationId, key.productId, key.channelType)
                        .subscribeOn(Schedulers.boundedElastic())
                        .toFuture()
        )).doOnNext(mapping -> 
                log.debug("Resolved mapping: {} -> {} (cached)", operationId, mapping.getProcessId())
        );
    }
    
    @Override
    public Mono<Void> invalidateCache(UUID tenantId) {
        return Mono.fromRunnable(() -> {
            // Remove all entries for this tenant
            cache.asMap().keySet().removeIf(key -> 
                    key.tenantId != null && key.tenantId.equals(tenantId));
            log.info("Invalidated cache for tenant: {}", tenantId);
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
    
    /**
     * Invalidates the entire cache.
     */
    public Mono<Void> invalidateAllCache() {
        return Mono.fromRunnable(() -> {
            cache.asMap().clear();
            log.info("Invalidated entire process mapping cache");
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
    
    /**
     * Fetches a mapping from the config-mgmt service.
     */
    private Mono<ProcessMapping> fetchMapping(
            UUID tenantId,
            String operationId,
            UUID productId,
            String channelType) {
        
        log.debug("Fetching mapping from config-mgmt: tenant={}, operation={}, product={}, channel={}",
                tenantId, operationId, productId, channelType);
        
        return configMgmtClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(MAPPINGS_API_PATH + "/resolve");
                    if (tenantId != null) {
                        uriBuilder.queryParam("tenantId", tenantId);
                    }
                    uriBuilder.queryParam("operationId", operationId);
                    if (productId != null) {
                        uriBuilder.queryParam("productId", productId);
                    }
                    if (channelType != null) {
                        uriBuilder.queryParam("channelType", channelType);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToMono(ApiProcessMappingResponse.class)
                .timeout(responseTimeout)
                .map(this::toProcessMapping)
                .switchIfEmpty(createDefaultMapping(operationId))
                .onErrorResume(error -> {
                    if (error instanceof java.util.concurrent.TimeoutException) {
                        log.warn("Timeout fetching mapping from config-mgmt for operation: {}, using default", operationId);
                    } else {
                        log.warn("Failed to fetch mapping from config-mgmt: {}, using default", error.getMessage());
                    }
                    return createDefaultMapping(operationId);
                });
    }
    
    /**
     * Creates a default mapping when none is configured.
     */
    private Mono<ProcessMapping> createDefaultMapping(String operationId) {
        log.debug("Using default mapping for operation: {}", operationId);
        return Mono.just(ProcessMapping.builder()
                .operationId(operationId)
                .processId(operationId)  // Use operation ID as process ID
                .build());
    }
    
    /**
     * Converts the API response to ProcessMapping.
     */
    private ProcessMapping toProcessMapping(ApiProcessMappingResponse response) {
        return ProcessMapping.builder()
                .operationId(response.operationId)
                .processId(response.processId)
                .processVersion(response.processVersion)
                .tenantId(response.tenantId)
                .productId(response.productId)
                .channelType(response.channelType)
                .build();
    }
    
    /**
     * Gets cache statistics.
     */
    public CacheStatistics getCacheStatistics() {
        var stats = cache.synchronous().stats();
        return new CacheStatistics(
                stats.hitCount(),
                stats.missCount(),
                stats.loadCount(),
                stats.evictionCount(),
                cache.asMap().size()
        );
    }
    
    /**
     * Cache key for process mappings.
     */
    private record MappingCacheKey(
            UUID tenantId,
            String operationId,
            UUID productId,
            String channelType
    ) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MappingCacheKey that = (MappingCacheKey) o;
            return Objects.equals(tenantId, that.tenantId) &&
                    Objects.equals(operationId, that.operationId) &&
                    Objects.equals(productId, that.productId) &&
                    Objects.equals(channelType, that.channelType);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(tenantId, operationId, productId, channelType);
        }
    }
    
    /**
     * Response DTO from config-mgmt API.
     */
    private record ApiProcessMappingResponse(
            UUID id,
            UUID tenantId,
            String operationId,
            UUID productId,
            String channelType,
            String processId,
            String processVersion,
            String loaderType,
            String sourceUri
    ) {}
    
    /**
     * Cache statistics record.
     */
    public record CacheStatistics(
            long hitCount,
            long missCount,
            long loadCount,
            long evictionCount,
            long size
    ) {
        public double hitRate() {
            long total = hitCount + missCount;
            return total > 0 ? (double) hitCount / total : 0.0;
        }
    }
}
