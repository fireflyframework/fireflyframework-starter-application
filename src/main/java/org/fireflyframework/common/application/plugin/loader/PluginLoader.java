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

package org.fireflyframework.application.plugin.loader;

import org.fireflyframework.application.plugin.ProcessPlugin;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Strategy interface for loading process plugins from various sources.
 * 
 * <p>Implementations provide different mechanisms for discovering and
 * loading plugins:</p>
 * <ul>
 *   <li>{@code SpringBeanPluginLoader} - Loads from Spring application context</li>
 *   <li>{@code JarPluginLoader} - Loads from external JAR files</li>
 *   <li>{@code RemoteRepositoryPluginLoader} - Loads from Git/Maven/HTTP repositories</li>
 * </ul>
 * 
 * <h3>Loader Priority</h3>
 * <p>Loaders are processed in order of priority (lower = higher priority).
 * This allows Spring bean plugins to take precedence over JAR plugins.</p>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
public interface PluginLoader {
    
    /**
     * Returns the type identifier for this loader.
     * 
     * <p>Examples: "spring-bean", "jar", "remote-maven", "remote-git"</p>
     * 
     * @return the loader type identifier
     */
    String getLoaderType();
    
    /**
     * Returns the priority of this loader.
     * 
     * <p>Lower values = higher priority. Loaders are processed in
     * ascending order of priority.</p>
     * 
     * @return the priority value
     */
    int getPriority();
    
    /**
     * Checks if this loader is enabled.
     * 
     * @return true if the loader is enabled
     */
    boolean isEnabled();
    
    /**
     * Checks if this loader supports the given plugin descriptor.
     * 
     * @param descriptor the plugin descriptor
     * @return true if this loader can load the described plugin
     */
    boolean supports(PluginDescriptor descriptor);
    
    /**
     * Discovers and loads all plugins from this loader's source.
     * 
     * <p>This method is called during application startup to discover
     * all available plugins. It should return all plugins that can be
     * loaded from this source.</p>
     * 
     * @return Flux of discovered plugins
     */
    Flux<ProcessPlugin> discoverPlugins();
    
    /**
     * Loads a specific plugin based on its descriptor.
     * 
     * @param descriptor the plugin descriptor
     * @return Mono emitting the loaded plugin
     */
    Mono<ProcessPlugin> loadPlugin(PluginDescriptor descriptor);
    
    /**
     * Unloads a plugin.
     * 
     * <p>This method is called when a plugin needs to be removed from
     * the system. Implementations should clean up any resources associated
     * with the plugin.</p>
     * 
     * @param processId the process ID to unload
     * @return Mono that completes when unloading is done
     */
    Mono<Void> unloadPlugin(String processId);
    
    /**
     * Reloads a plugin.
     * 
     * <p>This method is called during hot-reload to update a plugin.
     * The default implementation unloads and re-loads the plugin.</p>
     * 
     * @param descriptor the plugin descriptor
     * @return Mono emitting the reloaded plugin
     */
    default Mono<ProcessPlugin> reloadPlugin(PluginDescriptor descriptor) {
        return unloadPlugin(descriptor.getProcessId())
                .then(loadPlugin(descriptor));
    }
    
    /**
     * Initializes the loader.
     * 
     * <p>Called during application startup to perform any necessary
     * initialization (e.g., setting up file watchers).</p>
     * 
     * @return Mono that completes when initialization is done
     */
    default Mono<Void> initialize() {
        return Mono.empty();
    }
    
    /**
     * Shuts down the loader.
     * 
     * <p>Called during application shutdown to clean up resources.</p>
     * 
     * @return Mono that completes when shutdown is done
     */
    default Mono<Void> shutdown() {
        return Mono.empty();
    }
    
    /**
     * Checks if this loader supports hot-reload.
     * 
     * @return true if hot-reload is supported
     */
    default boolean supportsHotReload() {
        return false;
    }
}
