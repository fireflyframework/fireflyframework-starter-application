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

package org.fireflyframework.application.plugin.config;

import org.fireflyframework.application.plugin.ProcessPlugin;
import org.fireflyframework.application.plugin.ProcessPluginRegistry;
import org.fireflyframework.application.plugin.event.PluginEventPublisher;
import org.fireflyframework.application.plugin.loader.PluginLoader;
import org.fireflyframework.application.plugin.loader.SpringBeanPluginLoader;
import org.fireflyframework.application.plugin.metrics.PluginMetricsService;
import org.fireflyframework.application.plugin.service.ProcessMappingService;
import org.fireflyframework.application.plugin.service.ProcessPluginExecutor;
import org.fireflyframework.application.security.SecurityAuthorizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Auto-configuration for the Firefly Plugin Architecture.
 * 
 * <p>This configuration:</p>
 * <ul>
 *   <li>Enables plugin configuration properties</li>
 *   <li>Creates the plugin registry</li>
 *   <li>Initializes all plugin loaders in priority order</li>
 *   <li>Discovers and registers plugins from all sources</li>
 *   <li>Configures the ProcessPluginExecutor</li>
 * </ul>
 * 
 * <h3>Activation</h3>
 * <p>This configuration is activated when {@code firefly.application.plugin.enabled=true}
 * (which is the default).</p>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(PluginProperties.class)
@ConditionalOnProperty(name = "firefly.application.plugin.enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "org.fireflyframework.application.plugin")
public class PluginAutoConfiguration implements SmartLifecycle {
    
    /**
     * Phase for plugin system - runs early in startup (before web server).
     */
    private static final int PLUGIN_SYSTEM_PHASE = SmartLifecycle.DEFAULT_PHASE - 1000;
    
    /**
     * Default timeout for plugin initialization.
     */
    private static final Duration DEFAULT_INIT_TIMEOUT = Duration.ofMinutes(2);
    
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    @Autowired
    private PluginProperties properties;
    
    @Autowired
    private List<PluginLoader> pluginLoaders;
    
    @Autowired
    private ProcessPluginRegistry registry;
    
    @Autowired(required = false)
    private PluginEventPublisher eventPublisher;
    
    @Autowired(required = false)
    private PluginMetricsService metricsService;
    
    /**
     * Creates the ProcessPluginRegistry bean if not already defined.
     */
    @Bean
    @ConditionalOnMissingBean
    public ProcessPluginRegistry processPluginRegistry() {
        return new ProcessPluginRegistry();
    }
    
    /**
     * Creates the SpringBeanPluginLoader bean if not already defined.
     */
    @Bean
    @ConditionalOnMissingBean
    public SpringBeanPluginLoader springBeanPluginLoader(PluginProperties properties) {
        return new SpringBeanPluginLoader(properties);
    }
    
    /**
     * Creates the ProcessPluginExecutor bean if not already defined.
     */
    @Bean
    @ConditionalOnMissingBean
    public ProcessPluginExecutor processPluginExecutor(
            ProcessPluginRegistry registry,
            ProcessMappingService mappingService,
            SecurityAuthorizationService authorizationService,
            PluginProperties properties,
            @Autowired(required = false) PluginEventPublisher eventPublisher,
            @Autowired(required = false) PluginMetricsService metricsService) {
        return new ProcessPluginExecutor(registry, mappingService, authorizationService, 
                properties, eventPublisher, metricsService);
    }
    
    /**
     * Creates a default ProcessMappingService if not already defined.
     * This basic implementation returns a default mapping; override in applications
     * that integrate with config-mgmt.
     */
    @Bean
    @ConditionalOnMissingBean
    public ProcessMappingService processMappingService() {
        return new DefaultProcessMappingService();
    }
    
    // ==================== SmartLifecycle Implementation ====================
    
    @Override
    public void start() {
        if (!properties.isEnabled()) {
            log.info("Plugin system is disabled");
            running.set(true);
            return;
        }
        
        log.info("Starting Firefly Plugin System...");
        long startTime = System.currentTimeMillis();
        
        try {
            // Sort loaders by priority
            List<PluginLoader> sortedLoaders = pluginLoaders.stream()
                    .filter(PluginLoader::isEnabled)
                    .sorted(Comparator.comparingInt(PluginLoader::getPriority))
                    .toList();
            
            log.info("Found {} enabled plugin loaders", sortedLoaders.size());
            
            AtomicInteger pluginCount = new AtomicInteger(0);
            
            // Initialize loaders and discover plugins - BLOCKING to ensure completion
            Flux.fromIterable(sortedLoaders)
                    .concatMap(loader -> {
                        log.debug("Initializing loader: {} (priority: {})", 
                                loader.getLoaderType(), loader.getPriority());
                        return loader.initialize()
                                .then(Mono.just(loader));
                    })
                    .concatMap(loader -> {
                        log.debug("Discovering plugins from loader: {}", loader.getLoaderType());
                        return loader.discoverPlugins()
                                .doOnNext(p -> log.debug("Discovered plugin: {} v{} from {}",
                                        p.getProcessId(), p.getVersion(), loader.getLoaderType()));
                    })
                    .concatMap(plugin -> {
                        log.debug("Initializing and registering plugin: {} v{}", 
                                plugin.getProcessId(), plugin.getVersion());
                        return plugin.onInit()
                                .then(registry.register(plugin))
                                .doOnSuccess(v -> {
                                    pluginCount.incrementAndGet();
                                    if (eventPublisher != null) {
                                        String loaderType = plugin.getMetadata() != null 
                                                ? plugin.getMetadata().getSourceType() 
                                                : "unknown";
                                        eventPublisher.publishPluginRegistered(plugin, loaderType);
                                    }
                                })
                                .thenReturn(plugin);
                    })
                    .collectList()
                    .timeout(DEFAULT_INIT_TIMEOUT)
                    .block();  // BLOCKING - ensures all plugins are loaded before app starts
            
            long initTime = System.currentTimeMillis() - startTime;
            log.info("Plugin system started. Registered {} processes ({} total versions) in {}ms",
                    registry.size(), registry.totalVersionCount(), initTime);
            
            // Record metrics
            if (metricsService != null) {
                metricsService.recordInitialization(initTime, pluginCount.get());
                metricsService.setRegisteredPluginCount(registry.size());
            }
            
            // Publish system initialized event
            if (eventPublisher != null) {
                eventPublisher.publishSystemInitialized(registry.size(), registry.totalVersionCount(), initTime);
            }
            
            // Validate plugin count if required
            if (registry.size() == 0 && properties.getRegistry().isFailOnEmpty()) {
                throw new IllegalStateException("No process plugins found and failOnEmpty is true");
            }
            
            running.set(true);
            
        } catch (Exception e) {
            log.error("Failed to start plugin system", e);
            throw new IllegalStateException("Plugin system initialization failed", e);
        }
    }
    
    @Override
    public void stop() {
        if (!running.get()) {
            return;
        }
        
        log.info("Stopping Firefly Plugin System...");
        
        try {
            // Shutdown all loaders
            Flux.fromIterable(pluginLoaders)
                    .filter(PluginLoader::isEnabled)
                    .flatMap(PluginLoader::shutdown)
                    .then()
                    .timeout(Duration.ofSeconds(30))
                    .block();
            
            log.info("Plugin system stopped");
        } catch (Exception e) {
            log.warn("Error during plugin system shutdown", e);
        } finally {
            running.set(false);
        }
    }
    
    @Override
    public boolean isRunning() {
        return running.get();
    }
    
    @Override
    public int getPhase() {
        return PLUGIN_SYSTEM_PHASE;
    }
    
    @Override
    public boolean isAutoStartup() {
        return true;
    }
    
    /**
     * Default ProcessMappingService implementation.
     * Returns a vanilla mapping that uses the operationId as the processId.
     */
    private static class DefaultProcessMappingService implements ProcessMappingService {
        
        @Override
        public Mono<org.fireflyframework.application.plugin.ProcessMapping> resolveMapping(
                java.util.UUID tenantId,
                String operationId,
                java.util.UUID productId,
                String channelType) {
            
            // Default: use operationId as processId (vanilla fallback)
            return Mono.just(org.fireflyframework.application.plugin.ProcessMapping.builder()
                    .operationId(operationId)
                    .processId(operationId)  // Same as operationId for vanilla
                    .build());
        }
        
        @Override
        public Mono<Void> invalidateCache(java.util.UUID tenantId) {
            return Mono.empty();
        }
    }
}
