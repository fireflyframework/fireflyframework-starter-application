/*
 * Copyright 2025 Firefly Software Solutions Inc
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

package com.firefly.common.application.plugin.loader;

import com.firefly.common.application.plugin.DelegatingProcessPlugin;
import com.firefly.common.application.plugin.ProcessMetadata;
import com.firefly.common.application.plugin.ProcessPlugin;
import com.firefly.common.application.plugin.annotation.FireflyProcess;
import com.firefly.common.application.plugin.config.PluginProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Plugin loader that discovers and loads process plugins from the Spring application context.
 * 
 * <p>This loader scans for beans annotated with {@code @FireflyProcess} that implement
 * the {@code ProcessPlugin} interface. It is the default and highest-priority loader.</p>
 * 
 * <h3>Discovery Process</h3>
 * <ol>
 *   <li>Scans the application context for beans annotated with {@code @FireflyProcess}</li>
 *   <li>Validates that each bean implements {@code ProcessPlugin}</li>
 *   <li>Builds metadata from the annotation attributes</li>
 *   <li>Wraps beans that need metadata enrichment</li>
 * </ol>
 * 
 * <h3>Configuration</h3>
 * <pre>
 * firefly:
 *   application:
 *     plugin:
 *       loaders:
 *         spring-bean:
 *           enabled: true
 *           priority: 0
 * </pre>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpringBeanPluginLoader implements PluginLoader, ApplicationContextAware {
    
    public static final String LOADER_TYPE = "spring-bean";
    public static final int DEFAULT_PRIORITY = 0;
    
    private final PluginProperties properties;
    
    private ApplicationContext applicationContext;
    
    /**
     * Cache of loaded plugins by process ID.
     */
    private final Map<String, ProcessPlugin> loadedPlugins = new ConcurrentHashMap<>();
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    
    @Override
    public String getLoaderType() {
        return LOADER_TYPE;
    }
    
    @Override
    public int getPriority() {
        return properties.getLoaders().getSpringBean().getPriority();
    }
    
    @Override
    public boolean isEnabled() {
        return properties.isEnabled() && properties.getLoaders().getSpringBean().isEnabled();
    }
    
    @Override
    public boolean supports(PluginDescriptor descriptor) {
        return descriptor != null && LOADER_TYPE.equals(descriptor.getSourceType());
    }
    
    @Override
    public Flux<ProcessPlugin> discoverPlugins() {
        if (!isEnabled()) {
            log.debug("Spring bean plugin loader is disabled, skipping discovery");
            return Flux.empty();
        }
        
        return Flux.defer(() -> {
            log.info("Discovering process plugins from Spring context...");
            
            // Find all beans annotated with @FireflyProcess
            Map<String, Object> annotatedBeans = applicationContext
                    .getBeansWithAnnotation(FireflyProcess.class);
            
            return Flux.fromIterable(annotatedBeans.entrySet())
                    .flatMap(entry -> processBean(entry.getKey(), entry.getValue()))
                    .doOnComplete(() -> log.info("Discovered {} process plugins from Spring context", 
                            loadedPlugins.size()));
        });
    }
    
    @Override
    public Mono<ProcessPlugin> loadPlugin(PluginDescriptor descriptor) {
        if (!supports(descriptor)) {
            return Mono.error(new IllegalArgumentException(
                    "Descriptor source type must be 'spring-bean', got: " + descriptor.getSourceType()));
        }
        
        return Mono.fromCallable(() -> {
            String processId = descriptor.getProcessId();
            
            // Check if already loaded
            ProcessPlugin cached = loadedPlugins.get(processId);
            if (cached != null && !descriptor.isForceReload()) {
                return cached;
            }
            
            // Try to find by class name
            String className = descriptor.getClassName();
            if (className != null) {
                try {
                    Class<?> clazz = Class.forName(className);
                    Object bean = applicationContext.getBean(clazz);
                    // Process synchronously in this context
                    return processBeanSync(clazz.getSimpleName(), bean);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("Plugin class not found: " + className, e);
                }
            }
            
            // Try to find by process ID (bean name or @FireflyProcess.id)
            Map<String, Object> annotatedBeans = applicationContext
                    .getBeansWithAnnotation(FireflyProcess.class);
            
            for (Map.Entry<String, Object> entry : annotatedBeans.entrySet()) {
                Object bean = entry.getValue();
                FireflyProcess annotation = bean.getClass().getAnnotation(FireflyProcess.class);
                if (annotation != null && processId.equals(annotation.id())) {
                    // Process synchronously in this context
                    return processBeanSync(entry.getKey(), bean);
                }
            }
            
            throw new IllegalArgumentException("Plugin not found: " + processId);
        });
    }
    
    @Override
    public Mono<Void> unloadPlugin(String processId) {
        return Mono.fromRunnable(() -> {
            ProcessPlugin removed = loadedPlugins.remove(processId);
            if (removed != null) {
                log.info("Unloaded Spring bean plugin: {}", processId);
            }
        });
    }
    
    /**
     * Processes a Spring bean to extract or wrap it as a ProcessPlugin.
     */
    private Mono<ProcessPlugin> processBean(String beanName, Object bean) {
        return Mono.fromCallable(() -> processBeanSync(beanName, bean))
                .filter(p -> p != null);
    }
    
    /**
     * Synchronous version of processBean for use in non-reactive contexts.
     */
    private ProcessPlugin processBeanSync(String beanName, Object bean) {
        if (!(bean instanceof ProcessPlugin)) {
            log.warn("Bean {} is annotated with @FireflyProcess but does not implement ProcessPlugin, skipping",
                    beanName);
            return null;
        }
        
        ProcessPlugin plugin = (ProcessPlugin) bean;
        FireflyProcess annotation = bean.getClass().getAnnotation(FireflyProcess.class);
        
        if (annotation == null) {
            log.warn("Bean {} implements ProcessPlugin but is not annotated with @FireflyProcess", beanName);
            return null;
        }
        
        // Build metadata from annotation if the plugin doesn't provide its own
        ProcessMetadata existingMetadata = plugin.getMetadata();
        if (existingMetadata == null || existingMetadata.getProcessId() == null) {
            plugin = wrapWithMetadata(plugin, annotation);
        }
        
        loadedPlugins.put(plugin.getProcessId(), plugin);
        log.debug("Loaded Spring bean plugin: {} v{}", plugin.getProcessId(), plugin.getVersion());
        
        return plugin;
    }
    
    /**
     * Wraps a ProcessPlugin with metadata derived from the annotation.
     */
    private ProcessPlugin wrapWithMetadata(ProcessPlugin plugin, FireflyProcess annotation) {
        ProcessMetadata metadata = buildMetadataFromAnnotation(annotation);
        return DelegatingProcessPlugin.wrap(plugin, metadata);
    }
    
    /**
     * Builds ProcessMetadata from the FireflyProcess annotation.
     */
    private ProcessMetadata buildMetadataFromAnnotation(FireflyProcess annotation) {
        ProcessMetadata.ProcessMetadataBuilder builder = ProcessMetadata.builder()
                .processId(annotation.id())
                .version(annotation.version())
                .sourceType(LOADER_TYPE)
                .vanilla(annotation.vanilla())
                .deprecated(annotation.deprecated());
        
        if (!annotation.name().isEmpty()) {
            builder.name(annotation.name());
        }
        if (!annotation.description().isEmpty()) {
            builder.description(annotation.description());
        }
        if (!annotation.category().isEmpty()) {
            builder.category(annotation.category());
        }
        if (!annotation.replacedBy().isEmpty()) {
            builder.replacedBy(annotation.replacedBy());
        }
        if (annotation.capabilities().length > 0) {
            builder.capabilities(new HashSet<>(Arrays.asList(annotation.capabilities())));
        }
        if (annotation.requiredPermissions().length > 0) {
            builder.requiredPermissions(new HashSet<>(Arrays.asList(annotation.requiredPermissions())));
        }
        if (annotation.requiredRoles().length > 0) {
            builder.requiredRoles(new HashSet<>(Arrays.asList(annotation.requiredRoles())));
        }
        if (annotation.requiredFeatures().length > 0) {
            builder.requiredFeatures(new HashSet<>(Arrays.asList(annotation.requiredFeatures())));
        }
        if (annotation.tags().length > 0) {
            builder.tags(new HashSet<>(Arrays.asList(annotation.tags())));
        }
        
        return builder.build();
    }
}
