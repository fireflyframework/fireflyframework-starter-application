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

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Thread-safe registry for process plugins.
 * 
 * <p>The registry maintains all loaded process plugins and provides
 * lookup functionality by process ID and version. It supports:</p>
 * <ul>
 *   <li>Multiple versions of the same process</li>
 *   <li>Version-specific and latest-version lookups</li>
 *   <li>Capability-based discovery</li>
 *   <li>Hot-reload for plugin updates</li>
 * </ul>
 * 
 * <h3>Version Resolution</h3>
 * <p>When no specific version is requested, the registry returns the
 * latest version based on semantic versioning comparison.</p>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class ProcessPluginRegistry {
    
    /**
     * Main registry: processId -> (version -> plugin)
     */
    private final Map<String, NavigableMap<String, ProcessPlugin>> plugins = new ConcurrentHashMap<>();
    
    /**
     * Capability index: capability -> processIds
     */
    private final Map<String, Collection<String>> capabilityIndex = new ConcurrentHashMap<>();
    
    /**
     * Registers a process plugin.
     * 
     * <p>If a plugin with the same ID and version already exists,
     * it will be replaced (useful for hot-reload).</p>
     * 
     * @param plugin the plugin to register
     * @return Mono that completes when registration is done
     */
    public Mono<Void> register(ProcessPlugin plugin) {
        return Mono.fromRunnable(() -> {
            String processId = plugin.getProcessId();
            String version = plugin.getVersion();
            
            plugins.computeIfAbsent(processId, k -> new TreeMap<>(new SemanticVersionComparator()))
                   .put(version, plugin);
            
            // Update capability index
            ProcessMetadata metadata = plugin.getMetadata();
            if (metadata != null && metadata.getCapabilities() != null) {
                for (String capability : metadata.getCapabilities()) {
                    capabilityIndex.computeIfAbsent(capability, k -> ConcurrentHashMap.newKeySet())
                                  .add(processId);
                }
            }
            
            log.info("Registered process plugin: {} v{}", processId, version);
        });
    }
    
    /**
     * Unregisters a process plugin (all versions).
     * 
     * @param processId the process ID to unregister
     * @return Mono emitting the unregistered plugins, or empty if not found
     */
    public Mono<Collection<ProcessPlugin>> unregister(String processId) {
        return Mono.fromCallable(() -> {
            NavigableMap<String, ProcessPlugin> versions = plugins.remove(processId);
            if (versions != null) {
                // Remove from capability index
                for (ProcessPlugin plugin : versions.values()) {
                    ProcessMetadata metadata = plugin.getMetadata();
                    if (metadata != null && metadata.getCapabilities() != null) {
                        for (String capability : metadata.getCapabilities()) {
                            Collection<String> processIds = capabilityIndex.get(capability);
                            if (processIds != null) {
                                processIds.remove(processId);
                            }
                        }
                    }
                }
                log.info("Unregistered process plugin: {} ({} versions)", processId, versions.size());
                return versions.values();
            }
            return null;
        });
    }
    
    /**
     * Unregisters a specific version of a process plugin.
     * 
     * @param processId the process ID
     * @param version the version to unregister
     * @return Mono emitting the unregistered plugin, or empty if not found
     */
    public Mono<ProcessPlugin> unregister(String processId, String version) {
        return Mono.fromCallable(() -> {
            NavigableMap<String, ProcessPlugin> versions = plugins.get(processId);
            if (versions != null) {
                ProcessPlugin removed = versions.remove(version);
                if (removed != null) {
                    log.info("Unregistered process plugin: {} v{}", processId, version);
                    
                    // Clean up capability index for this version
                    ProcessMetadata metadata = removed.getMetadata();
                    if (metadata != null && metadata.getCapabilities() != null) {
                        for (String capability : metadata.getCapabilities()) {
                            // Only remove from capability index if no other version has this capability
                            boolean otherVersionHasCapability = versions.values().stream()
                                    .anyMatch(p -> {
                                        ProcessMetadata pm = p.getMetadata();
                                        return pm != null && pm.hasCapability(capability);
                                    });
                            if (!otherVersionHasCapability) {
                                Collection<String> processIds = capabilityIndex.get(capability);
                                if (processIds != null) {
                                    processIds.remove(processId);
                                    // Clean up empty capability entries
                                    if (processIds.isEmpty()) {
                                        capabilityIndex.remove(capability);
                                    }
                                }
                            }
                        }
                    }
                    
                    // Clean up empty maps
                    if (versions.isEmpty()) {
                        plugins.remove(processId);
                    }
                    return removed;
                }
            }
            return null;
        });
    }
    
    /**
     * Gets the latest version of a process plugin.
     * 
     * @param processId the process ID
     * @return Optional containing the plugin, or empty if not found
     */
    public Optional<ProcessPlugin> get(String processId) {
        NavigableMap<String, ProcessPlugin> versions = plugins.get(processId);
        if (versions == null || versions.isEmpty()) {
            return Optional.empty();
        }
        // Return the highest version (last entry in NavigableMap with our comparator)
        return Optional.of(versions.lastEntry().getValue());
    }
    
    /**
     * Gets a specific version of a process plugin.
     * 
     * @param processId the process ID
     * @param version the requested version
     * @return Optional containing the plugin, or empty if not found
     */
    public Optional<ProcessPlugin> get(String processId, String version) {
        if (version == null || version.isEmpty()) {
            return get(processId);
        }
        NavigableMap<String, ProcessPlugin> versions = plugins.get(processId);
        if (versions == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(versions.get(version));
    }
    
    /**
     * Gets the latest version of a process plugin reactively.
     * 
     * @param processId the process ID
     * @return Mono emitting the plugin, or empty if not found
     */
    public Mono<ProcessPlugin> getReactive(String processId) {
        return Mono.justOrEmpty(get(processId));
    }
    
    /**
     * Gets a specific version reactively.
     * 
     * @param processId the process ID
     * @param version the version
     * @return Mono emitting the plugin, or empty if not found
     */
    public Mono<ProcessPlugin> getReactive(String processId, String version) {
        return Mono.justOrEmpty(get(processId, version));
    }
    
    /**
     * Gets all registered plugins (latest version of each).
     * 
     * @return collection of plugins
     */
    public Collection<ProcessPlugin> getAll() {
        return plugins.values().stream()
                .filter(versions -> !versions.isEmpty())
                .map(NavigableMap::lastEntry)
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets all versions of a specific process.
     * 
     * @param processId the process ID
     * @return collection of all versions
     */
    public Collection<ProcessPlugin> getAllVersions(String processId) {
        NavigableMap<String, ProcessPlugin> versions = plugins.get(processId);
        return versions != null ? versions.values() : java.util.Collections.emptyList();
    }
    
    /**
     * Finds plugins by capability.
     * 
     * @param capability the capability to search for
     * @return Flux of plugins with the capability
     */
    public Flux<ProcessPlugin> findByCapability(String capability) {
        return Flux.defer(() -> {
            Collection<String> processIds = capabilityIndex.get(capability);
            if (processIds == null || processIds.isEmpty()) {
                return Flux.empty();
            }
            return Flux.fromIterable(processIds)
                       .flatMap(this::getReactive);
        });
    }
    
    /**
     * Finds plugins by category.
     * 
     * @param category the category to search for
     * @return Flux of plugins in the category
     */
    public Flux<ProcessPlugin> findByCategory(String category) {
        return Flux.fromIterable(getAll())
                   .filter(plugin -> {
                       ProcessMetadata metadata = plugin.getMetadata();
                       return metadata != null && category.equals(metadata.getCategory());
                   });
    }
    
    /**
     * Finds all vanilla (default) plugins.
     * 
     * @return Flux of vanilla plugins
     */
    public Flux<ProcessPlugin> findVanillaPlugins() {
        return Flux.fromIterable(getAll())
                   .filter(plugin -> {
                       ProcessMetadata metadata = plugin.getMetadata();
                       return metadata != null && metadata.isVanilla();
                   });
    }
    
    /**
     * Checks if a process is registered.
     * 
     * @param processId the process ID
     * @return true if registered
     */
    public boolean contains(String processId) {
        return plugins.containsKey(processId);
    }
    
    /**
     * Checks if a specific version is registered.
     * 
     * @param processId the process ID
     * @param version the version
     * @return true if registered
     */
    public boolean contains(String processId, String version) {
        NavigableMap<String, ProcessPlugin> versions = plugins.get(processId);
        return versions != null && versions.containsKey(version);
    }
    
    /**
     * Gets the count of registered processes (unique IDs).
     * 
     * @return process count
     */
    public int size() {
        return plugins.size();
    }
    
    /**
     * Gets the total count of registered plugin versions.
     * 
     * @return total version count
     */
    public int totalVersionCount() {
        return plugins.values().stream()
                      .mapToInt(NavigableMap::size)
                      .sum();
    }
    
    /**
     * Clears all registered plugins.
     * Use with caution - typically only for testing.
     */
    public void clear() {
        plugins.clear();
        capabilityIndex.clear();
        log.warn("Plugin registry cleared");
    }
    
    /**
     * Comparator for semantic versioning.
     * Supports versions like: 1.0.0, 1.2.3, 2.0.0-SNAPSHOT
     */
    private static class SemanticVersionComparator implements Comparator<String> {
        @Override
        public int compare(String v1, String v2) {
            if (v1 == null && v2 == null) return 0;
            if (v1 == null) return -1;
            if (v2 == null) return 1;
            
            // Split version into numeric parts and qualifier
            String[] parts1 = v1.split("[-.]");
            String[] parts2 = v2.split("[-.]");
            
            int length = Math.max(parts1.length, parts2.length);
            for (int i = 0; i < length; i++) {
                String p1 = i < parts1.length ? parts1[i] : "0";
                String p2 = i < parts2.length ? parts2[i] : "0";
                
                // Try numeric comparison first
                try {
                    int n1 = Integer.parseInt(p1);
                    int n2 = Integer.parseInt(p2);
                    if (n1 != n2) {
                        return Integer.compare(n1, n2);
                    }
                } catch (NumberFormatException e) {
                    // Fall back to string comparison for qualifiers
                    // SNAPSHOT < ALPHA < BETA < RC < (release)
                    int cmp = compareQualifiers(p1, p2);
                    if (cmp != 0) {
                        return cmp;
                    }
                }
            }
            return 0;
        }
        
        private int compareQualifiers(String q1, String q2) {
            int rank1 = getQualifierRank(q1);
            int rank2 = getQualifierRank(q2);
            if (rank1 != rank2) {
                return Integer.compare(rank1, rank2);
            }
            return q1.compareToIgnoreCase(q2);
        }
        
        private int getQualifierRank(String qualifier) {
            String upper = qualifier.toUpperCase();
            if (upper.contains("SNAPSHOT")) return 0;
            if (upper.contains("ALPHA")) return 1;
            if (upper.contains("BETA")) return 2;
            if (upper.contains("RC")) return 3;
            return 4; // Release
        }
    }
}
