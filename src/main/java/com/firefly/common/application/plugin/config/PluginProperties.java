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

package com.firefly.common.application.plugin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Configuration properties for the Firefly Plugin Architecture.
 * 
 * <h3>Configuration Example</h3>
 * <pre>
 * firefly:
 *   application:
 *     plugin:
 *       enabled: true
 *       registry:
 *         scan-packages:
 *           - com.firefly
 *           - com.mybank.processes
 *       loaders:
 *         spring-bean:
 *           enabled: true
 *           priority: 0
 *         jar:
 *           enabled: true
 *           priority: 10
 *           scan-directories:
 *             - /opt/firefly/plugins
 *           hot-reload: true
 *           hot-reload-interval: PT30S
 *         remote:
 *           enabled: false
 *           repositories: []
 *       security:
 *         sandbox-enabled: true
 *         allowed-packages:
 *           - java.util
 *           - java.time
 *       cache:
 *         enabled: true
 *         ttl: PT1H
 * </pre>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "firefly.application.plugin")
public class PluginProperties {
    
    /**
     * Whether the plugin system is enabled.
     */
    private boolean enabled = true;
    
    /**
     * Registry configuration.
     */
    private RegistryProperties registry = new RegistryProperties();
    
    /**
     * Loader configurations.
     */
    private LoaderProperties loaders = new LoaderProperties();
    
    /**
     * Security configuration.
     */
    private SecurityProperties security = new SecurityProperties();
    
    /**
     * Cache configuration.
     */
    private CacheProperties cache = new CacheProperties();
    
    /**
     * Event publishing configuration.
     */
    private EventProperties events = new EventProperties();
    
    /**
     * Metrics configuration.
     */
    private MetricsProperties metrics = new MetricsProperties();
    
    /**
     * Health check configuration.
     */
    private HealthProperties health = new HealthProperties();
    
    /**
     * Circuit breaker configuration.
     */
    private CircuitBreakerProperties circuitBreaker = new CircuitBreakerProperties();
    
    /**
     * Default timeout for remote operations (config-mgmt calls, remote downloads, etc.).
     */
    private Duration remoteTimeout = Duration.ofSeconds(30);
    
    /**
     * Registry-specific configuration.
     */
    @Data
    public static class RegistryProperties {
        
        /**
         * Packages to scan for @FireflyProcess annotated classes.
         */
        private List<String> scanPackages = new ArrayList<>(List.of("com.firefly"));
        
        /**
         * Whether to fail startup if no plugins are found.
         */
        private boolean failOnEmpty = false;
        
        /**
         * Whether to validate plugin metadata on registration.
         */
        private boolean validateMetadata = true;
    }
    
    /**
     * Configuration for all plugin loaders.
     */
    @Data
    public static class LoaderProperties {
        
        private SpringBeanLoaderProperties springBean = new SpringBeanLoaderProperties();
        private JarLoaderProperties jar = new JarLoaderProperties();
        private RemoteLoaderProperties remote = new RemoteLoaderProperties();
    }
    
    /**
     * Spring bean loader configuration.
     */
    @Data
    public static class SpringBeanLoaderProperties {
        
        /**
         * Whether the Spring bean loader is enabled.
         */
        private boolean enabled = true;
        
        /**
         * Priority (lower = higher priority).
         */
        private int priority = 0;
    }
    
    /**
     * JAR plugin loader configuration.
     */
    @Data
    public static class JarLoaderProperties {
        
        /**
         * Whether the JAR loader is enabled.
         */
        private boolean enabled = true;
        
        /**
         * Priority (lower = higher priority).
         */
        private int priority = 10;
        
        /**
         * Directories to scan for plugin JARs.
         */
        private List<String> scanDirectories = new ArrayList<>();
        
        /**
         * Whether to enable hot-reload of JAR plugins.
         */
        private boolean hotReload = true;
        
        /**
         * Interval for checking for plugin changes.
         */
        private Duration hotReloadInterval = Duration.ofSeconds(30);
        
        /**
         * Whether to use isolated classloaders for plugins.
         */
        private boolean classloaderIsolation = true;
        
        /**
         * Whether to verify JAR signatures.
         */
        private boolean verifySignatures = false;
    }
    
    /**
     * Remote repository loader configuration.
     */
    @Data
    public static class RemoteLoaderProperties {
        
        /**
         * Whether the remote loader is enabled.
         */
        private boolean enabled = false;
        
        /**
         * Priority (lower = higher priority).
         */
        private int priority = 20;
        
        /**
         * List of remote plugin repositories.
         */
        private List<RepositoryConfig> repositories = new ArrayList<>();
        
        /**
         * Directory for caching downloaded plugins.
         */
        private String cacheDirectory = "/var/firefly/plugin-cache";
        
        /**
         * Connection timeout for remote operations.
         */
        private Duration connectionTimeout = Duration.ofSeconds(30);
        
        /**
         * Read timeout for remote operations.
         */
        private Duration readTimeout = Duration.ofMinutes(5);
        
        /**
         * Whether to verify checksums.
         */
        private boolean verifyChecksums = true;
    }
    
    /**
     * Configuration for a single remote repository.
     */
    @Data
    public static class RepositoryConfig {
        
        /**
         * Repository type: "maven", "git", "http"
         */
        private String type;
        
        /**
         * Repository URL.
         */
        private String url;
        
        /**
         * Repository name/ID.
         */
        private String name;
        
        /**
         * Authentication credentials (if required).
         */
        private CredentialsConfig credentials;
        
        /**
         * Whether this repository is enabled.
         */
        private boolean enabled = true;
    }
    
    /**
     * Credentials configuration for authenticated repositories.
     */
    @Data
    public static class CredentialsConfig {
        
        /**
         * Username for authentication.
         */
        private String username;
        
        /**
         * Password or token for authentication.
         * Consider using a secret management solution.
         */
        private String password;
        
        /**
         * Path to SSH key (for Git).
         */
        private String sshKeyPath;
    }
    
    /**
     * Security configuration for plugin sandboxing.
     */
    @Data
    public static class SecurityProperties {
        
        /**
         * Whether to enable the security sandbox.
         */
        private boolean sandboxEnabled = true;
        
        /**
         * Packages that plugins are allowed to access.
         */
        private Set<String> allowedPackages = Set.of(
                "java.util",
                "java.time",
                "java.math",
                "reactor.core",
                "com.firefly.common.application.plugin",
                "com.firefly.common.application.context"
        );
        
        /**
         * Packages that plugins are denied access to.
         */
        private Set<String> deniedPackages = Set.of(
                "java.io",
                "java.net",
                "java.lang.reflect",
                "java.lang.invoke",
                "java.security",
                "sun",
                "com.sun"
        );
        
        /**
         * Maximum execution time for a process.
         */
        private Duration maxExecutionTime = Duration.ofMinutes(5);
        
        /**
         * Maximum memory per plugin (if enforcement is available).
         */
        private long maxMemoryMb = 256;
    }
    
    /**
     * Cache configuration for process mapping resolution.
     */
    @Data
    public static class CacheProperties {
        
        /**
         * Whether caching is enabled.
         */
        private boolean enabled = true;
        
        /**
         * Time-to-live for cached mappings.
         */
        private Duration ttl = Duration.ofHours(1);
        
        /**
         * Maximum cache entries.
         */
        private int maxEntries = 1000;
        
        /**
         * Whether to refresh cache entries in the background.
         */
        private boolean backgroundRefresh = true;
    }
    
    /**
     * Event publishing configuration.
     */
    @Data
    public static class EventProperties {
        
        /**
         * Whether event publishing is enabled.
         */
        private boolean enabled = true;
        
        /**
         * Whether to publish execution start/complete/failed events.
         * These can be high-volume, so disable for performance if not needed.
         */
        private boolean publishExecutionEvents = true;
    }
    
    /**
     * Metrics configuration.
     */
    @Data
    public static class MetricsProperties {
        
        /**
         * Whether metrics collection is enabled.
         */
        private boolean enabled = true;
        
        /**
         * Whether to collect detailed per-process metrics.
         * Enabling this creates separate timers/counters for each process ID.
         */
        private boolean detailedPerProcess = true;
    }
    
    /**
     * Health check configuration.
     */
    @Data
    public static class HealthProperties {
        
        /**
         * Whether health checks are enabled.
         */
        private boolean enabled = true;
        
        /**
         * Whether to check individual plugin health.
         * When enabled, each plugin's healthCheck() method is called.
         */
        private boolean checkIndividualPlugins = false;
        
        /**
         * Timeout for health check operations.
         */
        private Duration timeout = Duration.ofSeconds(10);
    }
    
    /**
     * Circuit breaker configuration for remote plugin loading.
     */
    @Data
    public static class CircuitBreakerProperties {
        
        /**
         * Whether circuit breaker is enabled.
         */
        private boolean enabled = true;
        
        /**
         * Failure rate threshold percentage to open the circuit.
         */
        private float failureRateThreshold = 50.0f;
        
        /**
         * Slow call rate threshold percentage.
         */
        private float slowCallRateThreshold = 100.0f;
        
        /**
         * Duration threshold for slow calls.
         */
        private Duration slowCallDurationThreshold = Duration.ofSeconds(10);
        
        /**
         * Number of calls in the sliding window.
         */
        private int slidingWindowSize = 10;
        
        /**
         * Minimum number of calls before the circuit breaker can calculate the error rate.
         */
        private int minimumNumberOfCalls = 5;
        
        /**
         * Time to wait in open state before transitioning to half-open.
         */
        private Duration waitDurationInOpenState = Duration.ofSeconds(30);
        
        /**
         * Number of permitted calls in half-open state.
         */
        private int permittedCallsInHalfOpenState = 3;
    }
}
