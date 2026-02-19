# Remove Plugin System — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Remove the entire plugin system (37 files) and all references to it from the starter application.

**Architecture:** The plugin system is self-contained in `src/main/java/.../plugin/` with minimal integration points (2 bean definitions in `ApplicationLayerAutoConfiguration`, 2 entries in auto-config imports). Removal is delete-first, then clean references, then verify.

**Tech Stack:** Java 21, Spring Boot 3.x, Maven

---

### Task 1: Delete plugin source and test directories

**Files:**
- Delete: `src/main/java/org/fireflyframework/common/application/plugin/` (29 files)
- Delete: `src/test/java/org/fireflyframework/common/application/plugin/` (8 files)

**Step 1: Delete main plugin directory**

```bash
rm -rf src/main/java/org/fireflyframework/common/application/plugin/
```

**Step 2: Delete plugin test directory**

```bash
rm -rf src/test/java/org/fireflyframework/common/application/plugin/
```

**Step 3: Verify directories are gone**

```bash
ls src/main/java/org/fireflyframework/common/application/plugin/ 2>&1
# Expected: "No such file or directory"
ls src/test/java/org/fireflyframework/common/application/plugin/ 2>&1
# Expected: "No such file or directory"
```

**Step 4: Commit**

```bash
git add -A && git commit -m "refactor: remove plugin system source and test files"
```

---

### Task 2: Remove plugin references from ApplicationLayerAutoConfiguration

**Files:**
- Modify: `src/main/java/org/fireflyframework/common/application/config/ApplicationLayerAutoConfiguration.java`

**Step 1: Remove the 3 plugin imports (lines 23-25)**

Remove:
```java
import org.fireflyframework.common.application.plugin.ProcessPluginRegistry;
import org.fireflyframework.common.application.plugin.config.PluginProperties;
import org.fireflyframework.common.application.plugin.metrics.PluginMetricsService;
```

**Step 2: Remove the processPluginRegistry bean (lines 213-223)**

Remove:
```java
    /**
     * Creates the process plugin registry bean.
     *
     * @return ProcessPluginRegistry instance
     */
    @Bean
    @ConditionalOnMissingBean
    public ProcessPluginRegistry processPluginRegistry() {
        log.info("Creating ProcessPluginRegistry bean");
        return new ProcessPluginRegistry();
    }
```

**Step 3: Remove the pluginMetricsService bean (lines 225-240)**

Remove:
```java
    /**
     * Creates the plugin metrics service bean.
     * Only created when Micrometer's {@link MeterRegistry} is on the classpath.
     *
     * @param meterRegistry the meter registry
     * @param pluginProperties the plugin properties
     * @return PluginMetricsService instance
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(MeterRegistry.class)
    public PluginMetricsService pluginMetricsService(MeterRegistry meterRegistry,
                                                     PluginProperties pluginProperties) {
        log.info("Creating PluginMetricsService bean");
        return new PluginMetricsService(meterRegistry, pluginProperties);
    }
```

**Step 4: Remove unused MeterRegistry import if no other beans use it**

Check if `MeterRegistry` is still used in the file. If not, remove:
```java
import io.micrometer.core.instrument.MeterRegistry;
```
And the corresponding `@ConditionalOnClass` import if unused.

**Step 5: Commit**

```bash
git add src/main/java/org/fireflyframework/common/application/config/ApplicationLayerAutoConfiguration.java
git commit -m "refactor: remove plugin bean definitions from auto-configuration"
```

---

### Task 3: Remove plugin auto-config entries

**Files:**
- Modify: `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

**Step 1: Remove the 2 plugin auto-configuration entries**

Remove these lines:
```
org.fireflyframework.common.application.plugin.config.PluginAutoConfiguration
org.fireflyframework.common.application.plugin.resolver.ProcessMappingResolverAutoConfiguration
```

The file should contain only:
```
org.fireflyframework.common.application.config.ApplicationLayerAutoConfiguration
org.fireflyframework.common.application.config.ConfigCacheAutoConfiguration
org.fireflyframework.common.application.config.DomainPassthroughAutoConfiguration
```

**Step 2: Commit**

```bash
git add src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
git commit -m "refactor: remove plugin auto-configuration entries"
```

---

### Task 4: Delete plugin documentation

**Files:**
- Delete: `docs/PLUGIN_DEVELOPMENT_GUIDE.md`
- Delete: `docs/PLUGIN_LOADERS.md`

**Step 1: Delete the files**

```bash
rm docs/PLUGIN_DEVELOPMENT_GUIDE.md docs/PLUGIN_LOADERS.md
```

**Step 2: Commit**

```bash
git add -A && git commit -m "docs: remove plugin documentation"
```

---

### Task 5: Update README.md

**Files:**
- Modify: `README.md`

**Step 1: Update project description (line 8)**

Change:
```
> Opinionated starter for application-layer microservices providing business process orchestration, context management, security, authorization, plugin system, and session management.
```
To:
```
> Opinionated starter for application-layer microservices providing business process orchestration, context management, security, authorization, and session management.
```

**Step 2: Update Overview paragraph (lines 26-32)**

Remove all plugin references. Replace the overview paragraphs (lines 26-32) with:
```
Firefly Framework Starter Application is an **opinionated starter for application-layer microservices** that orchestrate business processes across domain services. It provides a complete foundation for building application-tier microservices with enterprise-grade context management, security authorization, and session handling.

This starter supplies context management (application config, security context, session context) and security authorization with annotation-driven endpoint protection. It's designed for the **application tier** in a multi-tier architecture — the orchestration layer that coordinates domain services and implements business workflows.

The starter also includes abstract controllers for standardized REST endpoints, configuration caching, domain passthrough for cross-service data propagation, and SPI-based session management.
```

**Step 3: Update Features list (lines 39-48)**

Remove these lines:
```
- Plugin system with `ProcessPlugin`, `ProcessPluginRegistry`, and `@FireflyProcess`
- Plugin loaders: Spring Bean, JAR file, remote Maven repository
- Process mapping resolution with caching support
```
and:
```
- Plugin event publishing and metrics collection
- Health indicators for application layer and plugins
```

Replace the health line with:
```
- Health indicators for application layer
```

**Step 4: Update Configuration example (lines 87-103)**

Remove the plugin section from the YAML. New config:
```yaml
firefly:
  application:
    security:
      enabled: true
    config-cache:
      enabled: true
      ttl: 5m
    domain-passthrough:
      enabled: true
```

**Step 5: Update Documentation links (lines 107-118)**

Remove:
```
- [Plugin Development Guide](docs/PLUGIN_DEVELOPMENT_GUIDE.md)
- [Plugin Loaders](docs/PLUGIN_LOADERS.md)
```

**Step 6: Commit**

```bash
git add README.md
git commit -m "docs: remove plugin references from README"
```

---

### Task 6: Update pom.xml description

**Files:**
- Modify: `pom.xml`

**Step 1: Update description (line 19)**

Change:
```xml
<description>Starter for application-layer microservices. Enables business process orchestration with context management, security, authorization, and plugin support.</description>
```
To:
```xml
<description>Starter for application-layer microservices. Enables business process orchestration with context management, security, and authorization.</description>
```

**Step 2: Commit**

```bash
git add pom.xml
git commit -m "build: remove plugin mention from pom.xml description"
```

---

### Task 7: Verify — compile and run tests

**Step 1: Clean compile**

```bash
mvn clean compile -q
```
Expected: BUILD SUCCESS

**Step 2: Run all tests**

```bash
mvn test
```
Expected: BUILD SUCCESS, all remaining tests pass

**Step 3: Final grep to ensure no dangling plugin references**

```bash
grep -r "plugin" src/ --include="*.java" -l
grep -r "ProcessPlugin\|PluginProperties\|PluginMetrics\|FireflyProcess\|PluginLoader\|PluginAuto\|PluginEvent\|PluginHealth" src/ -l
```
Expected: No matches

**Step 4: Commit any fixes if needed, then tag**

If clean:
```bash
git log --oneline -7
```
Verify the commit history tells a clear story.
