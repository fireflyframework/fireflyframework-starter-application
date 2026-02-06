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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * A delegating wrapper for ProcessPlugin that provides custom metadata.
 * 
 * <p>This class is used by plugin loaders to wrap plugins with metadata
 * derived from annotations or external configuration, while delegating
 * all execution to the underlying plugin.</p>
 * 
 * <h3>Usage</h3>
 * <pre>
 * ProcessPlugin wrapped = new DelegatingProcessPlugin(originalPlugin, customMetadata);
 * </pre>
 * 
 * <p>This class eliminates code duplication between different plugin loaders
 * (SpringBeanPluginLoader, JarPluginLoader) that previously had their own
 * identical wrapper implementations.</p>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class DelegatingProcessPlugin implements ProcessPlugin {
    
    /**
     * The underlying plugin that handles actual execution.
     */
    @Getter
    private final ProcessPlugin delegate;
    
    /**
     * Custom metadata that overrides the delegate's metadata.
     */
    private final ProcessMetadata metadata;
    
    @Override
    public String getProcessId() {
        return metadata.getProcessId();
    }
    
    @Override
    public String getVersion() {
        return metadata.getVersion();
    }
    
    @Override
    public ProcessMetadata getMetadata() {
        return metadata;
    }
    
    @Override
    public Mono<ProcessResult> execute(ProcessExecutionContext context) {
        return delegate.execute(context);
    }
    
    @Override
    public Mono<ValidationResult> validate(ProcessExecutionContext context) {
        return delegate.validate(context);
    }
    
    @Override
    public Mono<ProcessResult> compensate(ProcessExecutionContext context) {
        return delegate.compensate(context);
    }
    
    @Override
    public Mono<Void> onInit() {
        return delegate.onInit();
    }
    
    @Override
    public Mono<Void> onDestroy() {
        return delegate.onDestroy();
    }
    
    @Override
    public Mono<HealthStatus> healthCheck() {
        return delegate.healthCheck();
    }
    
    /**
     * Creates a DelegatingProcessPlugin with the given delegate and metadata.
     * 
     * @param delegate the underlying plugin
     * @param metadata the custom metadata
     * @return a new DelegatingProcessPlugin
     */
    public static DelegatingProcessPlugin wrap(ProcessPlugin delegate, ProcessMetadata metadata) {
        return new DelegatingProcessPlugin(delegate, metadata);
    }
    
    /**
     * Unwraps a DelegatingProcessPlugin to get the underlying delegate.
     * If the plugin is not a DelegatingProcessPlugin, returns it as-is.
     * 
     * @param plugin the plugin to unwrap
     * @return the underlying plugin
     */
    public static ProcessPlugin unwrap(ProcessPlugin plugin) {
        if (plugin instanceof DelegatingProcessPlugin) {
            return ((DelegatingProcessPlugin) plugin).getDelegate();
        }
        return plugin;
    }
}
