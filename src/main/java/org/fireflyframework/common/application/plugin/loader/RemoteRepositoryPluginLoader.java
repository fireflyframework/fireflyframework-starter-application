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
import org.fireflyframework.application.plugin.ProcessPluginRegistry;
import org.fireflyframework.application.plugin.config.PluginProperties;
import org.fireflyframework.application.plugin.config.PluginProperties.CircuitBreakerProperties;
import org.fireflyframework.application.plugin.config.PluginProperties.RepositoryConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

/**
 * Plugin loader that downloads and loads plugins from remote repositories.
 * 
 * <p>Supports multiple repository types:</p>
 * <ul>
 *   <li><b>Maven</b> - Downloads JARs from Maven repositories</li>
 *   <li><b>HTTP</b> - Downloads JARs from direct HTTP URLs</li>
 *   <li><b>Git</b> - Clones repositories and builds plugins (future)</li>
 * </ul>
 * 
 * <h3>Configuration</h3>
 * <pre>
 * firefly:
 *   application:
 *     plugin:
 *       loaders:
 *         remote:
 *           enabled: true
 *           priority: 20
 *           cache-directory: /var/firefly/plugin-cache
 *           repositories:
 *             - type: maven
 *               name: firefly-plugins
 *               url: https://repo.firefly.io/plugins
 *             - type: http
 *               name: custom-plugins
 *               url: https://plugins.mybank.com
 * </pre>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "firefly.application.plugin.loaders.remote.enabled", havingValue = "true")
public class RemoteRepositoryPluginLoader implements PluginLoader {
    
    public static final String LOADER_TYPE = "remote";
    
    private final PluginProperties properties;
    private final ProcessPluginRegistry registry;
    private final JarPluginLoader jarPluginLoader;
    private final WebClient webClient;
    private final CircuitBreaker downloadCircuitBreaker;
    private final Duration downloadTimeout;
    
    /**
     * Cache of downloaded artifacts.
     */
    private final Map<String, PluginArtifact> artifactCache = new ConcurrentHashMap<>();
    
    /**
     * Map of process ID to artifact that provides it.
     */
    private final Map<String, PluginArtifact> processToArtifact = new ConcurrentHashMap<>();
    
    public RemoteRepositoryPluginLoader(
            PluginProperties properties,
            ProcessPluginRegistry registry,
            JarPluginLoader jarPluginLoader) {
        this.properties = properties;
        this.registry = registry;
        this.jarPluginLoader = jarPluginLoader;
        this.downloadTimeout = properties.getRemoteTimeout();
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(50 * 1024 * 1024)) // 50MB
                .build();
        
        // Configure circuit breaker for remote downloads
        this.downloadCircuitBreaker = createCircuitBreaker(properties.getCircuitBreaker());
    }
    
    /**
     * Creates a circuit breaker with the configured properties.
     */
    private CircuitBreaker createCircuitBreaker(CircuitBreakerProperties cbProps) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(cbProps.getFailureRateThreshold())
                .slowCallRateThreshold(cbProps.getSlowCallRateThreshold())
                .slowCallDurationThreshold(cbProps.getSlowCallDurationThreshold())
                .waitDurationInOpenState(cbProps.getWaitDurationInOpenState())
                .permittedNumberOfCallsInHalfOpenState(cbProps.getPermittedCallsInHalfOpenState())
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(cbProps.getSlidingWindowSize())
                .minimumNumberOfCalls(cbProps.getMinimumNumberOfCalls())
                .recordExceptions(IOException.class, TimeoutException.class)
                .build();
        
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        CircuitBreaker cb = registry.circuitBreaker("remote-plugin-download");
        
        // Add event listeners for monitoring
        cb.getEventPublisher()
                .onStateTransition(event -> 
                        log.info("Circuit breaker '{}' state changed: {} -> {}",
                                event.getCircuitBreakerName(),
                                event.getStateTransition().getFromState(),
                                event.getStateTransition().getToState()))
                .onCallNotPermitted(event -> 
                        log.warn("Circuit breaker '{}' rejected call - circuit is OPEN",
                                event.getCircuitBreakerName()));
        
        return cb;
    }
    
    @Override
    public String getLoaderType() {
        return LOADER_TYPE;
    }
    
    @Override
    public int getPriority() {
        return properties.getLoaders().getRemote().getPriority();
    }
    
    @Override
    public boolean isEnabled() {
        return properties.isEnabled() && properties.getLoaders().getRemote().isEnabled();
    }
    
    @Override
    public boolean supports(PluginDescriptor descriptor) {
        return descriptor != null && descriptor.isRemote();
    }
    
    @Override
    public boolean supportsHotReload() {
        return true;
    }
    
    @Override
    public Mono<Void> initialize() {
        if (!isEnabled()) {
            return Mono.empty();
        }
        
        return Mono.fromRunnable(() -> {
            log.info("Initializing remote repository plugin loader");
            
            // Create cache directory
            String cacheDir = properties.getLoaders().getRemote().getCacheDirectory();
            File cachePath = new File(cacheDir);
            if (!cachePath.exists()) {
                log.info("Creating plugin cache directory: {}", cacheDir);
                cachePath.mkdirs();
            }
            
            // Log configured repositories
            List<RepositoryConfig> repos = properties.getLoaders().getRemote().getRepositories();
            log.info("Configured {} remote plugin repositories", repos.size());
            for (RepositoryConfig repo : repos) {
                if (repo.isEnabled()) {
                    log.debug("Repository: {} ({}) - {}", repo.getName(), repo.getType(), repo.getUrl());
                }
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
    
    @Override
    public Flux<ProcessPlugin> discoverPlugins() {
        // Remote loader doesn't auto-discover; plugins are loaded on-demand
        // based on configuration mappings
        log.debug("Remote plugin loader: discovery skipped (on-demand loading)");
        return Flux.empty();
    }
    
    @Override
    public Mono<ProcessPlugin> loadPlugin(PluginDescriptor descriptor) {
        if (!supports(descriptor)) {
            return Mono.error(new IllegalArgumentException(
                    "Descriptor source type must be remote, got: " + descriptor.getSourceType()));
        }
        
        return resolveAndDownload(descriptor)
                .transformDeferred(CircuitBreakerOperator.of(downloadCircuitBreaker))
                .timeout(downloadTimeout)
                .flatMap(artifact -> loadFromArtifact(artifact, descriptor))
                .doOnSuccess(plugin -> {
                    if (plugin != null) {
                        processToArtifact.put(plugin.getProcessId(), 
                                artifactCache.get(getCacheKey(descriptor)));
                    }
                })
                .onErrorResume(io.github.resilience4j.circuitbreaker.CallNotPermittedException.class, e -> {
                    log.error("Remote plugin download circuit breaker is OPEN for: {}", descriptor.getProcessId());
                    return Mono.error(new IllegalStateException(
                            "Remote plugin download temporarily unavailable (circuit breaker open): " + 
                            descriptor.getProcessId(), e));
                });
    }
    
    @Override
    public Mono<Void> unloadPlugin(String processId) {
        return Mono.fromRunnable(() -> {
            PluginArtifact artifact = processToArtifact.remove(processId);
            if (artifact != null) {
                log.info("Unloaded remote plugin: {}", processId);
                // Note: We keep the cached JAR for potential future reloads
            }
        }).then(jarPluginLoader.unloadPlugin(processId));
    }
    
    /**
     * Resolves and downloads the plugin artifact.
     */
    private Mono<PluginArtifact> resolveAndDownload(PluginDescriptor descriptor) {
        String cacheKey = getCacheKey(descriptor);
        
        // Check cache first
        PluginArtifact cached = artifactCache.get(cacheKey);
        if (cached != null && !descriptor.isForceReload()) {
            File cachedFile = new File(cached.getLocalPath());
            if (cachedFile.exists()) {
                log.debug("Using cached artifact: {}", cached.getLocalPath());
                return Mono.just(cached);
            }
        }
        
        // Resolve based on source type
        switch (descriptor.getSourceType()) {
            case "remote-maven":
                return downloadFromMaven(descriptor);
            case "remote-http":
                return downloadFromHttp(descriptor);
            case "remote-git":
                return downloadFromGit(descriptor);
            default:
                return Mono.error(new IllegalArgumentException(
                        "Unknown remote source type: " + descriptor.getSourceType()));
        }
    }
    
    /**
     * Downloads a plugin from a Maven repository.
     */
    private Mono<PluginArtifact> downloadFromMaven(PluginDescriptor descriptor) {
        return Mono.fromCallable(() -> {
            String groupId = descriptor.getGroupId();
            String artifactId = descriptor.getArtifactId();
            String version = descriptor.getVersion();
            
            if (groupId == null || artifactId == null || version == null) {
                throw new IllegalArgumentException(
                        "Maven descriptor requires groupId, artifactId, and version");
            }
            
            // Find configured Maven repository
            RepositoryConfig mavenRepo = findRepository("maven");
            if (mavenRepo == null) {
                throw new IllegalStateException("No Maven repository configured");
            }
            
            // Build Maven URL
            String groupPath = groupId.replace('.', '/');
            String jarName = artifactId + "-" + version + ".jar";
            String mavenUrl = String.format("%s/%s/%s/%s/%s",
                    mavenRepo.getUrl().replaceAll("/$", ""),
                    groupPath,
                    artifactId,
                    version,
                    jarName);
            
            log.info("Downloading Maven artifact: {}:{}:{}", groupId, artifactId, version);
            
            return downloadJar(mavenUrl, descriptor);
        }).subscribeOn(Schedulers.boundedElastic());
    }
    
    /**
     * Downloads a plugin from a direct HTTP URL.
     */
    private Mono<PluginArtifact> downloadFromHttp(PluginDescriptor descriptor) {
        return Mono.fromCallable(() -> {
            String url = descriptor.getSourceUri();
            if (url == null || url.isEmpty()) {
                throw new IllegalArgumentException("HTTP descriptor requires sourceUri");
            }
            
            log.info("Downloading plugin from HTTP: {}", url);
            
            return downloadJar(url, descriptor);
        }).subscribeOn(Schedulers.boundedElastic());
    }
    
    /**
     * Downloads/clones from a Git repository.
     * Note: This is a simplified implementation. Full Git support would require JGit.
     */
    private Mono<PluginArtifact> downloadFromGit(PluginDescriptor descriptor) {
        return Mono.error(new UnsupportedOperationException(
                "Git repository support requires JGit dependency. Use Maven or HTTP instead."));
    }
    
    /**
     * Downloads a JAR file from a URL.
     */
    private PluginArtifact downloadJar(String url, PluginDescriptor descriptor) throws Exception {
        String cacheDir = properties.getLoaders().getRemote().getCacheDirectory();
        String fileName = extractFileName(url, descriptor);
        Path localPath = Paths.get(cacheDir, fileName);
        
        // Download the file
        log.debug("Downloading {} to {}", url, localPath);
        
        try (InputStream in = new URL(url).openStream();
             OutputStream out = Files.newOutputStream(localPath)) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytes = 0;
            
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }
            
            log.info("Downloaded {} bytes to {}", totalBytes, localPath);
        }
        
        // Verify checksum if provided
        if (descriptor.getChecksum() != null && !descriptor.getChecksum().isEmpty()) {
            String actualChecksum = calculateChecksum(localPath.toFile());
            if (!descriptor.getChecksum().equalsIgnoreCase(actualChecksum)) {
                Files.delete(localPath);
                throw new SecurityException("Checksum mismatch for downloaded artifact");
            }
            log.debug("Checksum verified for {}", fileName);
        }
        
        // Create and cache artifact
        PluginArtifact artifact = PluginArtifact.builder()
                .processId(descriptor.getProcessId())
                .version(descriptor.getVersion())
                .sourceType(descriptor.getSourceType())
                .sourceUrl(url)
                .localPath(localPath.toString())
                .downloadedAt(System.currentTimeMillis())
                .build();
        
        artifactCache.put(getCacheKey(descriptor), artifact);
        
        return artifact;
    }
    
    /**
     * Loads a plugin from a downloaded artifact.
     */
    private Mono<ProcessPlugin> loadFromArtifact(PluginArtifact artifact, PluginDescriptor descriptor) {
        // Create a JAR descriptor and delegate to JarPluginLoader
        PluginDescriptor jarDescriptor = PluginDescriptor.builder()
                .processId(descriptor.getProcessId())
                .version(descriptor.getVersion())
                .sourceType("jar")
                .sourceUri(artifact.getLocalPath())
                .className(descriptor.getClassName())
                .build();
        
        return jarPluginLoader.loadPlugin(jarDescriptor);
    }
    
    /**
     * Finds a configured repository by type.
     */
    private RepositoryConfig findRepository(String type) {
        return properties.getLoaders().getRemote().getRepositories().stream()
                .filter(repo -> repo.isEnabled() && type.equals(repo.getType()))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Generates a cache key for a descriptor.
     */
    private String getCacheKey(PluginDescriptor descriptor) {
        if (descriptor.getGroupId() != null) {
            return String.format("%s:%s:%s",
                    descriptor.getGroupId(),
                    descriptor.getArtifactId(),
                    descriptor.getVersion());
        }
        return descriptor.getProcessId() + ":" + descriptor.getVersion();
    }
    
    /**
     * Extracts a file name from URL or descriptor.
     */
    private String extractFileName(String url, PluginDescriptor descriptor) {
        // Try to get from URL
        String urlPath = url.substring(url.lastIndexOf('/') + 1);
        if (urlPath.endsWith(".jar")) {
            return urlPath;
        }
        
        // Generate from descriptor
        if (descriptor.getArtifactId() != null) {
            return descriptor.getArtifactId() + "-" + descriptor.getVersion() + ".jar";
        }
        
        return descriptor.getProcessId() + "-" + descriptor.getVersion() + ".jar";
    }
    
    /**
     * Calculates SHA-256 checksum of a file.
     */
    private String calculateChecksum(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        
        byte[] hashBytes = digest.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
    
    /**
     * Represents a downloaded plugin artifact.
     */
    @Data
    @Builder
    public static class PluginArtifact {
        private String processId;
        private String version;
        private String sourceType;
        private String sourceUrl;
        private String localPath;
        private String checksum;
        private long downloadedAt;
        private long fileSize;
    }
}
