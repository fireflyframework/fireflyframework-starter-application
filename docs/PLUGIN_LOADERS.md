# Plugin Loading Strategies

This document covers the various strategies for loading process plugins in the Firefly Plugin Architecture.

## Overview

The Plugin Architecture supports three loading strategies, each suited for different deployment scenarios:

| Loader | Priority | Use Case |
|--------|----------|----------|
| **Spring Bean** | 0 (highest) | Plugins bundled with the application |
| **JAR File** | 10 | External plugins deployed as JAR files |
| **Remote Repository** | 20 (lowest) | Plugins downloaded from Maven/HTTP repositories |

Plugins are loaded in priority order. If the same process ID exists in multiple sources, the higher-priority loader wins.

## 1. Spring Bean Loader

The default and simplest approach - plugins are Spring beans within your application.

### How It Works

1. Scans the Spring application context for beans annotated with `@FireflyProcess`
2. Validates that each bean implements `ProcessPlugin`
3. Registers discovered plugins with the registry

### Configuration

```yaml
firefly:
  application:
    plugin:
      loaders:
        spring-bean:
          enabled: true    # Default: true
          priority: 0      # Highest priority
```

### Usage

Simply annotate your plugin class with `@FireflyProcess` and `@Component`:

```java
@FireflyProcess(
    id = "vanilla-account-creation",
    name = "Standard Account Creation",
    version = "1.0.0"
)
@Component
public class VanillaAccountCreationProcess extends AbstractProcessPlugin<Request, Response> {
    
    @Override
    protected Mono<Response> doExecute(ProcessExecutionContext context, Request input) {
        // Business logic
    }
}
```

### Advantages

- Zero configuration for discovery
- Full Spring dependency injection support
- IDE support for debugging and navigation
- Type-safe compile-time checking

### Limitations

- Plugins must be in the application classpath at compile time
- Requires application restart to update plugins

---

## 2. JAR File Loader

Load plugins from external JAR files at runtime, enabling hot-reload without application restart.

### How It Works

1. Scans configured directories for JAR files
2. Creates isolated classloaders for each JAR (optional)
3. Discovers `@FireflyProcess` annotated classes within JARs
4. Watches directories for changes (hot-reload)

### Configuration

```yaml
firefly:
  application:
    plugin:
      loaders:
        jar:
          enabled: true
          priority: 10
          scan-directories:
            - /opt/firefly/plugins
            - /var/firefly/custom-plugins
          hot-reload: true
          hot-reload-interval: PT30S
          classloader-isolation: true  # Isolate each JAR's dependencies
```

### Directory Structure

```
/opt/firefly/plugins/
├── acme-bank-plugins-1.0.0.jar
├── premium-features-2.1.0.jar
└── custom-compliance-1.5.0.jar
```

### Creating a Plugin JAR

1. **Create a Maven/Gradle project** with `lib-common-application` as a dependency:

```xml
<dependency>
    <groupId>org.fireflyframework</groupId>
    <artifactId>lib-common-application</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <scope>provided</scope> <!-- Provided by the host application -->
</dependency>
```

2. **Implement your plugins**:

```java
@FireflyProcess(
    id = "acme-premium-account-creation",
    version = "1.0.0"
)
public class AcmePremiumAccountProcess extends AbstractProcessPlugin<Request, Response> {
    // Implementation
}
```

3. **Build the JAR**:

```bash
mvn clean package
```

4. **Deploy to plugin directory**:

```bash
cp target/acme-plugins-1.0.0.jar /opt/firefly/plugins/
```

The plugin will be automatically discovered and loaded (with hot-reload, no restart needed).

### Hot-Reload Behavior

| Event | Action |
|-------|--------|
| New JAR added | Plugins discovered and registered |
| JAR modified | Old plugins unloaded, new versions loaded (with debouncing) |
| JAR removed | Plugins unregistered |

**Debouncing:** The loader uses a 2-second debounce window to prevent multiple reloads when a file is being written. It also performs file stability checks (verifying file size stops changing) before reloading.

**Thread Safety:** Hot-reload operations are protected by a lock to prevent concurrent reload attempts for the same JAR.

**Dependency Validation:** Before loading a plugin, the loader validates that all required classes (ProcessPlugin, ProcessExecutionContext, ProcessResult, Mono) can be loaded by the classloader.

### Classloader Isolation

When `classloader-isolation: true`:

- Each JAR gets its own URLClassLoader
- Plugins can have different versions of the same dependency
- Prevents classpath conflicts

When `classloader-isolation: false`:

- All JARs share the application classloader
- Faster loading, but potential version conflicts

### Shutdown Hook

The JAR loader registers a JVM shutdown hook to ensure classloaders are properly closed even during forced shutdown (SIGKILL), preventing memory leaks.

### Advantages

- Hot-reload without restart
- Deploy tenant-specific plugins dynamically
- Classloader isolation for dependency management
- Automatic dependency validation
- Memory leak prevention with shutdown hooks

### Limitations

- More complex deployment pipeline
- Potential classloader issues with shared dependencies
- Debugging is more challenging

---

## 3. Remote Repository Loader

Download and load plugins from remote Maven repositories or HTTP endpoints.

### How It Works

1. Resolves artifact coordinates (groupId:artifactId:version)
2. Downloads JAR from configured repositories
3. Verifies checksum (SHA-256)
4. Caches locally and delegates to JAR loader

### Configuration

```yaml
firefly:
  application:
    plugin:
      loaders:
        remote:
          enabled: true
          priority: 20
          cache-directory: /var/firefly/plugin-cache
          verify-checksums: true
          repositories:
            # Maven repository
            - type: maven
              name: firefly-plugins
              url: https://repo.firefly.io/plugins
              enabled: true
              # Optional authentication
              username: ${MAVEN_USER}
              password: ${MAVEN_PASSWORD}
            
            # Direct HTTP downloads
            - type: http
              name: custom-plugins
              url: https://plugins.mybank.com
              enabled: true
              # HTTP basic auth if needed
              username: ${HTTP_USER}
              password: ${HTTP_PASSWORD}
```

### Maven Repository Format

Artifacts are resolved using standard Maven coordinates:

```
groupId:artifactId:version
```

Example: `com.acme.bank:premium-plugins:1.2.0`

The loader constructs the download URL:
```
{repository-url}/com/acme/bank/premium-plugins/1.2.0/premium-plugins-1.2.0.jar
```

### HTTP Repository Format

For HTTP repositories, provide the full path in the plugin descriptor:

```json
{
  "sourceType": "remote",
  "sourceUri": "https://plugins.mybank.com/premium-plugins-1.2.0.jar",
  "processId": "premium-account-creation"
}
```

### Circuit Breaker

The remote loader includes a Resilience4j circuit breaker to handle download failures gracefully:

```yaml
firefly:
  application:
    plugin:
      circuit-breaker:
        enabled: true
        failure-rate-threshold: 50      # Open circuit at 50% failure rate
        slow-call-rate-threshold: 100   # Consider all slow calls
        slow-call-duration-threshold: PT10S
        sliding-window-size: 10
        minimum-number-of-calls: 5
        wait-duration-in-open-state: PT30S
        permitted-calls-in-half-open-state: 3
      remote-timeout: PT30S  # Timeout for remote downloads
```

Circuit breaker states:
- **CLOSED**: Normal operation, downloads proceed
- **OPEN**: Too many failures, downloads rejected immediately
- **HALF_OPEN**: Testing if service recovered, limited calls allowed

### On-Demand Loading

Unlike other loaders, the remote loader loads plugins **on-demand**:

1. A request comes for operation `createPremiumAccount`
2. Config-mgmt returns mapping: `processId: "acme-premium-account"`
3. Process not in registry, descriptor specifies `sourceType: remote`
4. Circuit breaker checked - if OPEN, fail fast with error
5. Remote loader downloads from repository (with timeout)
6. JAR cached locally, plugin loaded
7. Subsequent requests use cached version

### Cache Management

```yaml
firefly:
  application:
    plugin:
      loaders:
        remote:
          cache-directory: /var/firefly/plugin-cache
          cache-ttl: P7D  # Cache JARs for 7 days
          max-cache-size: 500MB
```

Cache structure:
```
/var/firefly/plugin-cache/
├── com/
│   └── acme/
│       └── bank/
│           └── premium-plugins/
│               ├── 1.2.0/
│               │   ├── premium-plugins-1.2.0.jar
│               │   └── premium-plugins-1.2.0.jar.sha256
│               └── 1.3.0/
│                   └── ...
└── http/
    └── plugins.mybank.com/
        └── custom-plugin-1.0.0.jar
```

### Checksum Verification

When `verify-checksums: true`:

1. Downloads `{artifact}.sha256` alongside the JAR
2. Computes SHA-256 of downloaded JAR
3. Compares checksums
4. Rejects on mismatch

### Advantages

- Centralized plugin distribution
- Version management via standard Maven workflows
- Minimal local storage
- Easy rollback to previous versions
- Circuit breaker prevents cascading failures
- Configurable timeouts for reliability

### Limitations

- Network dependency for initial load
- Latency on first load of new plugins
- Requires repository infrastructure

---

## Configuration Reference

### Complete Configuration Example

```yaml
firefly:
  application:
    plugin:
      enabled: true
      
      loaders:
        # Spring Bean Loader (default, highest priority)
        spring-bean:
          enabled: true
          priority: 0
        
        # JAR File Loader
        jar:
          enabled: true
          priority: 10
          scan-directories:
            - /opt/firefly/plugins
          hot-reload: true
          hot-reload-interval: PT30S
          classloader-isolation: true
        
        # Remote Repository Loader
        remote:
          enabled: false  # Enable when needed
          priority: 20
          cache-directory: /var/firefly/plugin-cache
          verify-checksums: true
          repositories: []
      
      # Security settings
      security:
        sandbox-enabled: true
        max-execution-time: PT30S
        allowed-packages:
          - org.fireflyframework
          - com.acme.bank
      
      # Cache settings
      cache:
        enabled: true
        ttl: PT1H
      
      # Remote timeout
      remote-timeout: PT30S
      
      # Circuit breaker for remote loading
      circuit-breaker:
        enabled: true
        failure-rate-threshold: 50
        sliding-window-size: 10
        wait-duration-in-open-state: PT30S
      
      # Observability
      events:
        enabled: true
        publish-execution-events: true
      
      metrics:
        enabled: true
        detailed-per-process: true
      
      health:
        enabled: true
        check-individual-plugins: false
        timeout: PT10S
```

### Environment-Specific Overrides

**Development** (only Spring beans):
```yaml
firefly:
  application:
    plugin:
      loaders:
        spring-bean:
          enabled: true
        jar:
          enabled: false
        remote:
          enabled: false
```

**Production** (all loaders):
```yaml
firefly:
  application:
    plugin:
      loaders:
        spring-bean:
          enabled: true
        jar:
          enabled: true
          scan-directories:
            - /opt/firefly/plugins
        remote:
          enabled: true
          repositories:
            - type: maven
              name: firefly-plugins
              url: https://repo.firefly.io/plugins
```

---

## Plugin Descriptor

When loading plugins programmatically or configuring remote plugins:

```java
PluginDescriptor descriptor = PluginDescriptor.builder()
    .processId("premium-account-creation")
    .processVersion("1.0.0")
    .sourceType("remote")          // "spring-bean", "jar", or "remote"
    .sourceUri("com.acme:plugins:1.0.0")  // Maven coords or JAR path
    .className("com.acme.PremiumAccountProcess")  // Optional
    .forceReload(false)            // Force reload even if cached
    .build();
```

---

## Troubleshooting

### Plugin Not Discovered

1. **Check loader is enabled**: Verify configuration
2. **Check annotations**: Ensure `@FireflyProcess` is present
3. **Check interface**: Class must implement `ProcessPlugin`
4. **Check logs**: Look for discovery messages at DEBUG level

```bash
logging:
  level:
    org.fireflyframework.application.plugin.loader: DEBUG
```

### ClassNotFoundException in JAR Plugins

1. **Check dependencies**: Ensure required libraries are in the JAR or host application
2. **Check classloader isolation**: Try toggling `classloader-isolation`
3. **Use shade/fat JAR**: Include dependencies in the plugin JAR

### Remote Plugin Download Fails

1. **Check network**: Verify repository URL is accessible
2. **Check credentials**: Verify authentication if required
3. **Check artifact coordinates**: Verify groupId:artifactId:version format
4. **Check cache**: Clear cache directory and retry

---

## Best Practices

1. **Use Spring Bean loader for core plugins** - Fastest, simplest, best IDE support
2. **Use JAR loader for tenant customizations** - Hot-reload without restart
3. **Use Remote loader for centralized distribution** - Enterprise plugin management
4. **Always version your plugins** - Enable rollback and parallel versions
5. **Test plugins in isolation** - Unit test before deploying as JAR
6. **Monitor plugin loading** - Use actuator endpoints and logging

---

## Related Documentation

- [Plugin Development Guide](PLUGIN_DEVELOPMENT_GUIDE.md) - How to create plugins
- [Architecture Overview](ARCHITECTURE.md) - System architecture
- [API Reference](API_REFERENCE.md) - Detailed API documentation
