# Remove Plugin System from Starter Application

**Date:** 2026-02-19
**Status:** Approved

## Context

The architecture has shifted away from the dynamic plugin pattern. The plugin system (30+ files) is no longer needed and should be removed to simplify the starter.

No replacement is being introduced at this time.

## Scope

### Delete entirely

| Target | File count |
|--------|-----------|
| `src/main/java/.../plugin/` | 29 Java files |
| `src/test/java/.../plugin/` | 5 test files |
| `docs/PLUGIN_DEVELOPMENT_GUIDE.md` | 1 file |
| `docs/PLUGIN_LOADERS.md` | 1 file |

### Edit

| File | Change |
|------|--------|
| `ApplicationLayerAutoConfiguration.java` | Remove 3 plugin imports + 2 bean definitions (`processPluginRegistry`, `pluginMetricsService`) |
| `AutoConfiguration.imports` | Remove 2 plugin auto-config entries |
| `README.md` | Remove plugin references from description, features list, configuration example, and documentation links |
| `pom.xml` | Remove "plugin support" from `<description>` |

### Untouched

Everything else: controllers, services, security, context, AOP, health, metadata, SPI, util, CQRS, EDA, cache, observability, and their tests.

## Risks

- **Low risk:** The plugin system is almost entirely self-contained. Only 2 bean definitions and 2 auto-config entries reference it from outside.
- **Compilation:** After deletion, `ApplicationLayerAutoConfiguration` must still compile cleanly (it will — the removed beans have no dependents).
- **Tests:** No non-plugin tests reference the plugin system.
