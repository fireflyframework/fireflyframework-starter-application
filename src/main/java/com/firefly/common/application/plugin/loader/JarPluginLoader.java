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
import com.firefly.common.application.plugin.ProcessPluginRegistry;
import com.firefly.common.application.plugin.annotation.FireflyProcess;
import com.firefly.common.application.plugin.config.PluginProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import jakarta.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Plugin loader that discovers and loads process plugins from external JAR files.
 * 
 * <p>This loader scans configured directories for JAR files containing
 * classes annotated with {@code @FireflyProcess} that implement {@code ProcessPlugin}.</p>
 * 
 * <h3>Features</h3>
 * <ul>
 *   <li>Scans configured directories for plugin JARs</li>
 *   <li>Isolated classloaders for each plugin (optional)</li>
 *   <li>Hot-reload support via file system watching</li>
 *   <li>Automatic discovery of @FireflyProcess annotated classes</li>
 * </ul>
 * 
 * <h3>Configuration</h3>
 * <pre>
 * firefly:
 *   application:
 *     plugin:
 *       loaders:
 *         jar:
 *           enabled: true
 *           priority: 10
 *           scan-directories:
 *             - /opt/firefly/plugins
 *           hot-reload: true
 *           hot-reload-interval: PT30S
 *           classloader-isolation: true
 * </pre>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "firefly.application.plugin.loaders.jar.enabled", havingValue = "true", matchIfMissing = true)
public class JarPluginLoader implements PluginLoader {
    
    public static final String LOADER_TYPE = "jar";
    
    private final PluginProperties properties;
    private final ProcessPluginRegistry registry;
    
    /**
     * Map of JAR path to its classloader.
     */
    private final Map<String, URLClassLoader> classLoaders = new ConcurrentHashMap<>();
    
    /**
     * Map of process ID to the JAR that contains it.
     */
    private final Map<String, String> processToJar = new ConcurrentHashMap<>();
    
    /**
     * Map of JAR path to loaded plugins.
     */
    private final Map<String, List<ProcessPlugin>> jarToPlugins = new ConcurrentHashMap<>();
    
    /**
     * File system watcher for hot-reload.
     */
    private WatchService watchService;
    private volatile boolean watching = false;
    
    /**
     * Debounce tracking for hot-reload events.
     */
    private final Map<String, Instant> lastReloadAttempt = new ConcurrentHashMap<>();
    private static final Duration RELOAD_DEBOUNCE = Duration.ofSeconds(2);
    private final ReentrantLock reloadLock = new ReentrantLock();
    
    /**
     * Required interface classes that plugins must be able to access.
     */
    private static final List<String> REQUIRED_DEPENDENCY_CLASSES = Arrays.asList(
            "com.firefly.common.application.plugin.ProcessPlugin",
            "com.firefly.common.application.plugin.ProcessExecutionContext",
            "com.firefly.common.application.plugin.ProcessResult",
            "reactor.core.publisher.Mono"
    );
    
    /**
     * Flag to track if shutdown hook has been registered.
     */
    private final AtomicBoolean shutdownHookRegistered = new AtomicBoolean(false);
    
    public JarPluginLoader(PluginProperties properties, ProcessPluginRegistry registry) {
        this.properties = properties;
        this.registry = registry;
        registerShutdownHook();
    }
    
    /**
     * Registers a JVM shutdown hook to ensure classloaders are closed on forced shutdown.
     */
    private void registerShutdownHook() {
        if (shutdownHookRegistered.compareAndSet(false, true)) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("JVM shutdown detected, cleaning up JAR plugin classloaders");
                watching = false;
                if (watchService != null) {
                    try {
                        watchService.close();
                    } catch (IOException e) {
                        // Ignore during shutdown
                    }
                }
                // Close all classloaders synchronously
                for (String jarPath : new ArrayList<>(classLoaders.keySet())) {
                    closeClassLoader(jarPath);
                }
            }, "jar-plugin-shutdown-hook"));
        }
    }
    
    @Override
    public String getLoaderType() {
        return LOADER_TYPE;
    }
    
    @Override
    public int getPriority() {
        return properties.getLoaders().getJar().getPriority();
    }
    
    @Override
    public boolean isEnabled() {
        return properties.isEnabled() && properties.getLoaders().getJar().isEnabled();
    }
    
    @Override
    public boolean supports(PluginDescriptor descriptor) {
        return descriptor != null && LOADER_TYPE.equals(descriptor.getSourceType());
    }
    
    @Override
    public boolean supportsHotReload() {
        return properties.getLoaders().getJar().isHotReload();
    }
    
    @Override
    public Mono<Void> initialize() {
        if (!isEnabled()) {
            return Mono.empty();
        }
        
        return Mono.fromRunnable(() -> {
            log.info("Initializing JAR plugin loader");
            
            // Initialize directories
            List<String> scanDirs = properties.getLoaders().getJar().getScanDirectories();
            for (String dir : scanDirs) {
                File directory = new File(dir);
                if (!directory.exists()) {
                    log.info("Creating plugin directory: {}", dir);
                    directory.mkdirs();
                }
            }
            
            // Setup hot-reload watcher if enabled
            if (supportsHotReload()) {
                setupFileWatcher();
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
    
    @Override
    public Flux<ProcessPlugin> discoverPlugins() {
        if (!isEnabled()) {
            log.debug("JAR plugin loader is disabled, skipping discovery");
            return Flux.empty();
        }
        
        return Flux.defer(() -> {
            log.info("Discovering plugins from JAR files...");
            
            List<String> scanDirs = properties.getLoaders().getJar().getScanDirectories();
            
            return Flux.fromIterable(scanDirs)
                    .flatMap(this::scanDirectory)
                    .doOnComplete(() -> log.info("JAR plugin discovery complete. Found plugins in {} JARs", 
                            jarToPlugins.size()));
        });
    }
    
    @Override
    public Mono<ProcessPlugin> loadPlugin(PluginDescriptor descriptor) {
        if (!supports(descriptor)) {
            return Mono.error(new IllegalArgumentException(
                    "Descriptor source type must be 'jar', got: " + descriptor.getSourceType()));
        }
        
        return Mono.fromCallable(() -> {
            String jarPath = descriptor.getSourceUri();
            String className = descriptor.getClassName();
            
            if (jarPath == null || jarPath.isEmpty()) {
                throw new IllegalArgumentException("JAR path is required");
            }
            
            File jarFile = new File(jarPath);
            if (!jarFile.exists()) {
                throw new IllegalArgumentException("JAR file not found: " + jarPath);
            }
            
            URLClassLoader classLoader = getOrCreateClassLoader(jarPath);
            
            if (className != null && !className.isEmpty()) {
                // Load specific class
                return loadPluginClass(classLoader, className, jarPath);
            } else {
                // Scan JAR for plugins
                List<ProcessPlugin> plugins = scanJarFile(jarFile, classLoader);
                if (plugins.isEmpty()) {
                    throw new IllegalArgumentException("No plugins found in JAR: " + jarPath);
                }
                return plugins.get(0);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Mono<Void> unloadPlugin(String processId) {
        return Mono.fromRunnable(() -> {
            String jarPath = processToJar.remove(processId);
            if (jarPath != null) {
                // Check if any other plugins are still using this JAR
                List<ProcessPlugin> plugins = jarToPlugins.get(jarPath);
                if (plugins != null) {
                    plugins.removeIf(p -> processId.equals(p.getProcessId()));
                    
                    if (plugins.isEmpty()) {
                        // No more plugins from this JAR, close the classloader
                        jarToPlugins.remove(jarPath);
                        closeClassLoader(jarPath);
                    }
                }
                
                log.info("Unloaded JAR plugin: {}", processId);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
    
    @Override
    public Mono<Void> shutdown() {
        return Mono.fromRunnable(() -> {
            log.info("Shutting down JAR plugin loader");
            
            // Stop file watcher
            watching = false;
            if (watchService != null) {
                try {
                    watchService.close();
                } catch (IOException e) {
                    log.warn("Error closing watch service", e);
                }
            }
            
            // Close all classloaders
            for (String jarPath : new ArrayList<>(classLoaders.keySet())) {
                closeClassLoader(jarPath);
            }
            
            jarToPlugins.clear();
            processToJar.clear();
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
    
    @PreDestroy
    public void destroy() {
        shutdown().block();
    }
    
    /**
     * Scans a directory for JAR files containing plugins.
     */
    private Flux<ProcessPlugin> scanDirectory(String directoryPath) {
        return Flux.defer(() -> {
            File directory = new File(directoryPath);
            if (!directory.exists() || !directory.isDirectory()) {
                log.debug("Plugin directory does not exist: {}", directoryPath);
                return Flux.empty();
            }
            
            File[] jarFiles = directory.listFiles((dir, name) -> name.endsWith(".jar"));
            if (jarFiles == null || jarFiles.length == 0) {
                log.debug("No JAR files found in: {}", directoryPath);
                return Flux.empty();
            }
            
            log.debug("Found {} JAR files in {}", jarFiles.length, directoryPath);
            
            return Flux.fromArray(jarFiles)
                    .flatMap(this::loadJarFile);
        });
    }
    
    /**
     * Loads all plugins from a JAR file.
     */
    private Flux<ProcessPlugin> loadJarFile(File jarFile) {
        return Flux.defer(() -> {
            String jarPath = jarFile.getAbsolutePath();
            
            try {
                URLClassLoader classLoader = getOrCreateClassLoader(jarPath);
                List<ProcessPlugin> plugins = scanJarFile(jarFile, classLoader);
                
                if (!plugins.isEmpty()) {
                    jarToPlugins.put(jarPath, new ArrayList<>(plugins));
                    for (ProcessPlugin plugin : plugins) {
                        processToJar.put(plugin.getProcessId(), jarPath);
                    }
                    log.info("Loaded {} plugins from JAR: {}", plugins.size(), jarFile.getName());
                }
                
                return Flux.fromIterable(plugins);
            } catch (Exception e) {
                log.error("Failed to load JAR file: {}", jarPath, e);
                return Flux.empty();
            }
        });
    }
    
    /**
     * Scans a JAR file for plugin classes.
     */
    private List<ProcessPlugin> scanJarFile(File jarFile, URLClassLoader classLoader) {
        List<ProcessPlugin> plugins = new ArrayList<>();
        
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                
                if (name.endsWith(".class") && !name.contains("$")) {
                    String className = name.replace('/', '.').replace(".class", "");
                    
                    try {
                        Class<?> clazz = classLoader.loadClass(className);
                        
                        if (ProcessPlugin.class.isAssignableFrom(clazz) && 
                            clazz.isAnnotationPresent(FireflyProcess.class)) {
                            
                            ProcessPlugin plugin = loadPluginClass(classLoader, className, jarFile.getAbsolutePath());
                            if (plugin != null) {
                                plugins.add(plugin);
                            }
                        }
                    } catch (ClassNotFoundException | NoClassDefFoundError e) {
                        // Skip classes that can't be loaded
                        log.trace("Skipping class {}: {}", className, e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error scanning JAR file: {}", jarFile.getAbsolutePath(), e);
        }
        
        return plugins;
    }
    
    /**
     * Loads a specific plugin class from a classloader.
     */
    private ProcessPlugin loadPluginClass(URLClassLoader classLoader, String className, String jarPath) {
        try {
            // Validate dependencies before loading the plugin class
            if (!validateDependencies(classLoader, jarPath)) {
                log.error("Dependency validation failed for JAR: {}", jarPath);
                return null;
            }
            
            Class<?> clazz = classLoader.loadClass(className);
            
            if (!ProcessPlugin.class.isAssignableFrom(clazz)) {
                log.warn("Class {} does not implement ProcessPlugin", className);
                return null;
            }
            
            FireflyProcess annotation = clazz.getAnnotation(FireflyProcess.class);
            if (annotation == null) {
                log.warn("Class {} is not annotated with @FireflyProcess", className);
                return null;
            }
            
            // Instantiate the plugin
            ProcessPlugin plugin = (ProcessPlugin) clazz.getDeclaredConstructor().newInstance();
            
            // Wrap with metadata if needed
            ProcessMetadata existingMetadata = plugin.getMetadata();
            if (existingMetadata == null || existingMetadata.getProcessId() == null) {
                plugin = wrapWithMetadata(plugin, annotation, jarPath);
            }
            
            log.debug("Loaded plugin {} from JAR: {}", plugin.getProcessId(), jarPath);
            return plugin;
            
        } catch (Exception e) {
            log.error("Failed to load plugin class {}: {}", className, e.getMessage());
            return null;
        }
    }
    
    /**
     * Validates that all required dependency classes can be loaded by the classloader.
     */
    private boolean validateDependencies(URLClassLoader classLoader, String jarPath) {
        List<String> missingClasses = new ArrayList<>();
        
        for (String requiredClass : REQUIRED_DEPENDENCY_CLASSES) {
            try {
                classLoader.loadClass(requiredClass);
            } catch (ClassNotFoundException e) {
                missingClasses.add(requiredClass);
            }
        }
        
        if (!missingClasses.isEmpty()) {
            log.error("JAR {} is missing required dependencies: {}", jarPath, missingClasses);
            return false;
        }
        
        return true;
    }
    
    /**
     * Wraps a plugin with metadata from the annotation.
     */
    private ProcessPlugin wrapWithMetadata(ProcessPlugin plugin, FireflyProcess annotation, String jarPath) {
        ProcessMetadata metadata = ProcessMetadata.builder()
                .processId(annotation.id())
                .name(annotation.name().isEmpty() ? annotation.id() : annotation.name())
                .version(annotation.version())
                .description(annotation.description())
                .category(annotation.category())
                .capabilities(new HashSet<>(Arrays.asList(annotation.capabilities())))
                .requiredPermissions(new HashSet<>(Arrays.asList(annotation.requiredPermissions())))
                .requiredRoles(new HashSet<>(Arrays.asList(annotation.requiredRoles())))
                .requiredFeatures(new HashSet<>(Arrays.asList(annotation.requiredFeatures())))
                .tags(new HashSet<>(Arrays.asList(annotation.tags())))
                .sourceType(LOADER_TYPE)
                .sourceUri(jarPath)
                .vanilla(annotation.vanilla())
                .deprecated(annotation.deprecated())
                .replacedBy(annotation.replacedBy().isEmpty() ? null : annotation.replacedBy())
                .build();
        
        return DelegatingProcessPlugin.wrap(plugin, metadata);
    }
    
    /**
     * Gets or creates a classloader for a JAR file.
     */
    private URLClassLoader getOrCreateClassLoader(String jarPath) throws Exception {
        return classLoaders.computeIfAbsent(jarPath, path -> {
            try {
                URL jarUrl = new File(path).toURI().toURL();
                
                if (properties.getLoaders().getJar().isClassloaderIsolation()) {
                    // Isolated classloader - parent is system classloader
                    return new URLClassLoader(
                            new URL[]{jarUrl},
                            ClassLoader.getSystemClassLoader()
                    );
                } else {
                    // Shared classloader - parent is application classloader
                    return new URLClassLoader(
                            new URL[]{jarUrl},
                            getClass().getClassLoader()
                    );
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to create classloader for: " + path, e);
            }
        });
    }
    
    /**
     * Closes and removes a classloader for a JAR.
     */
    private void closeClassLoader(String jarPath) {
        URLClassLoader classLoader = classLoaders.remove(jarPath);
        if (classLoader != null) {
            try {
                classLoader.close();
                log.debug("Closed classloader for: {}", jarPath);
            } catch (IOException e) {
                log.warn("Error closing classloader for: {}", jarPath, e);
            }
        }
    }
    
    /**
     * Sets up file system watcher for hot-reload.
     */
    private void setupFileWatcher() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            
            for (String dir : properties.getLoaders().getJar().getScanDirectories()) {
                Path path = Paths.get(dir);
                if (Files.exists(path)) {
                    path.register(watchService, 
                            StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_MODIFY,
                            StandardWatchEventKinds.ENTRY_DELETE);
                    log.debug("Watching directory for changes: {}", dir);
                }
            }
            
            // Start watcher thread
            watching = true;
            Thread watcherThread = new Thread(this::watchForChanges, "jar-plugin-watcher");
            watcherThread.setDaemon(true);
            watcherThread.start();
            
            log.info("Hot-reload watcher started");
            
        } catch (IOException e) {
            log.error("Failed to setup file watcher for hot-reload", e);
        }
    }
    
    /**
     * Watches for file system changes and reloads plugins.
     */
    private void watchForChanges() {
        while (watching) {
            try {
                WatchKey key = watchService.take();
                
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    
                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path filename = ev.context();
                    
                    if (filename.toString().endsWith(".jar")) {
                        Path directory = (Path) key.watchable();
                        Path fullPath = directory.resolve(filename);
                        
                        log.info("Detected change in JAR: {} ({})", filename, kind.name());
                        
                        if (kind == StandardWatchEventKinds.ENTRY_CREATE || 
                            kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                            // Reload the JAR
                            reloadJar(fullPath.toFile());
                        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                            // Unload plugins from this JAR
                            unloadJar(fullPath.toString());
                        }
                    }
                }
                
                key.reset();
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ClosedWatchServiceException e) {
                break;
            }
        }
    }
    
    /**
     * Reloads a JAR file (unload old, load new) with debouncing.
     */
    private void reloadJar(File jarFile) {
        String jarPath = jarFile.getAbsolutePath();
        
        // Debounce: skip if we recently attempted to reload this JAR
        Instant now = Instant.now();
        Instant lastAttempt = lastReloadAttempt.get(jarPath);
        if (lastAttempt != null && Duration.between(lastAttempt, now).compareTo(RELOAD_DEBOUNCE) < 0) {
            log.debug("Skipping reload for {} - debounce in effect", jarFile.getName());
            return;
        }
        lastReloadAttempt.put(jarPath, now);
        
        // Use lock to prevent concurrent reloads of the same JAR
        if (!reloadLock.tryLock()) {
            log.debug("Skipping reload for {} - another reload in progress", jarFile.getName());
            return;
        }
        
        try {
            // Wait for file to be fully written using file stability check
            if (!waitForFileStable(jarFile, Duration.ofMillis(500), 5)) {
                log.warn("File {} did not stabilize, skipping reload", jarFile.getName());
                return;
            }
            
            // Unload existing plugins from this JAR
            unloadJar(jarPath);
            
            // Load new version
            loadJarFile(jarFile)
                    .flatMap(plugin -> plugin.onInit().then(registry.register(plugin)))
                    .subscribe(
                            v -> {},
                            error -> log.error("Error reloading JAR: {}", jarPath, error),
                            () -> log.info("Reloaded JAR: {}", jarFile.getName())
                    );
        } finally {
            reloadLock.unlock();
        }
    }
    
    /**
     * Waits for a file to become stable (size not changing) before proceeding.
     * This is better than a fixed Thread.sleep() as it adapts to actual file write completion.
     */
    private boolean waitForFileStable(File file, Duration checkInterval, int maxChecks) {
        long lastSize = -1;
        int stableCount = 0;
        
        for (int i = 0; i < maxChecks; i++) {
            long currentSize = file.length();
            if (currentSize == lastSize && currentSize > 0) {
                stableCount++;
                if (stableCount >= 2) {
                    return true; // File size stable for 2 consecutive checks
                }
            } else {
                stableCount = 0;
            }
            lastSize = currentSize;
            
            try {
                Thread.sleep(checkInterval.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        return file.exists() && file.length() > 0;
    }
    
    /**
     * Unloads all plugins from a JAR.
     */
    private void unloadJar(String jarPath) {
        List<ProcessPlugin> plugins = jarToPlugins.remove(jarPath);
        if (plugins != null) {
            for (ProcessPlugin plugin : plugins) {
                String processId = plugin.getProcessId();
                processToJar.remove(processId);
                
                plugin.onDestroy()
                        .then(registry.unregister(processId, plugin.getVersion()))
                        .subscribe();
            }
        }
        
        closeClassLoader(jarPath);
    }
    
    // JarProcessPlugin inner class removed - using DelegatingProcessPlugin.wrap() instead
}
