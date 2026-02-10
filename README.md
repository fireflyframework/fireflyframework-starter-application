# Firefly Common Application Library
    
[![CI](https://github.com/fireflyframework/fireflyframework-application/actions/workflows/ci.yml/badge.svg)](https://github.com/fireflyframework/fireflyframework-application/actions/workflows/ci.yml)
A comprehensive Spring Boot library for building Application Layer microservices
Business process orchestration • Multi-domain coordination • Context management • Security & Authorization

---

## Table of Contents

- [Overview](#overview)
- [Understanding the Three-Layer Architecture](#understanding-the-three-layer-architecture)
- [When to Use This Library](#when-to-use-this-library)
- [Key Features](#key-features)
- [Quick Start](#quick-start)
- [Core Components](#core-components)
- [Architecture Patterns](#architecture-patterns)
- [Complete Documentation](#-complete-documentation)
- [Examples](#examples)
- [Configuration](#configuration)
- [Caching](#caching)
- [Plugin Architecture](#plugin-architecture)
- [Testing](#testing)
- [Performance & Monitoring](#performance--monitoring)
- [Contributing](#contributing)
- [License](#license)

---

A comprehensive Spring Boot library that enables application layer architecture for business process-oriented microservices with context management, security, and authorization support.

## Overview

**The Application Layer is where business process orchestration microservices live.** This layer exposes REST/GraphQL APIs to channels (web, mobile, third-party applications) and orchestrates multi-domain business processes.

### Understanding Firefly's Three Layers

Firefly implements a clean, layered microservice architecture:

```
Channels (Web/Mobile/Apps)
         ↓
┌─────────────────────────────────────────┐
│   APPLICATION LAYER (THIS LIBRARY)      │  ← Exposes APIs to Channels
│   Business Process Orchestration        │  ← Orchestrates Multiple Domains
│   - customer-application-onboarding     │  ← Uses lib-common-application
│   - lending-application-loan-origin     │
└─────────────────┬───────────────────────┘
                  │ Uses ApplicationExecutionContext
                  ↓
┌─────────────────────────────────────────┐
│   DOMAIN LAYER                          │  ← Domain-Driven Design
│   Business Logic & Rules                │  ← Single-Domain Focus
│   - customer-domain-people              │  ← Uses lib-common-domain
│   - lending-domain-loan                 │
└─────────────────┬───────────────────────┘
                  │ Uses Repositories & SDKs
                  ↓
┌─────────────────────────────────────────┐
│   INFRASTRUCTURE/PLATFORM LAYER         │  ← Master Data Management
│   Data Persistence & Platform Services  │  ← External Integrations
│   - common-platform-customer-mgmt       │  ← Uses lib-common-core
│   - common-platform-contract-mgmt       │
└─────────────────────────────────────────┘
```

**This library (lib-common-application) is for Application Layer microservices that:**
- **Expose REST/GraphQL APIs** to channels (web, mobile, third-party)
- **Orchestrate business processes** across multiple domains
- **Coordinate multi-domain operations** (customer + contract + account)
- **Manage complete application context** (party, contract, product, tenant, security)
- **Handle security & authorization** at the API level

**NOT for:**
- Domain layer microservices (use `lib-common-domain`)
- Platform/infrastructure services (use `lib-common-core`)
- Single-domain operations (use `lib-common-domain`)

### Key Responsibilities

- **Context Management**: Resolving and managing party, contract, product, and tenant information
- **Security & Authorization**: Declarative security with @Secure annotation and SecurityCenter integration  
- **Configuration**: Multi-tenant configuration management with provider settings
- **Business Process Orchestration**: Coordinating domain services to fulfill business operations

### Architecture Complete – Controller-Based Context Resolution + Security Center Integration! 🎉

This library provides a **fully integrated, controller-based** application layer with **complete Security Center integration**:

#### Security Center Integration - COMPLETE
-  **FireflySessionManager Integration** – Fully integrated for session management, roles, and permissions
-  **Automatic Role Resolution** – Roles extracted from party contracts via Security Center
-  **Automatic Permission Resolution** – Permissions derived from role scopes (action + resource)
-  **Product Access Validation** – Validates party has access to requested products/contracts
-  **SessionContextMapper Utility** – Maps session data to AppContext roles/permissions
-  **Graceful Degradation** – Works even if Security Center is temporarily unavailable

#### Core Features
-  **@FireflyApplication** annotation for application metadata and service discovery
-  **Context Architecture** (AppContext, AppConfig, AppSecurityContext, ApplicationExecutionContext)
-  **@Secure Annotation** system for declarative security
-  **Two Base Controllers** – `AbstractApplicationController`, `AbstractResourceController`
-  **Automatic Context Resolution** – Party/Tenant from Istio headers + Contract/Product from path variables
-  **Default Config Resolver** – Fetches tenant configuration automatically
-  **Default Security Authorization** – Validates roles/permissions automatically
-  **Abstract Application Service** base class for business orchestration
-  **AOP Interceptors** for annotation processing
-  **Spring Boot Auto-configuration**
-  **Actuator Integration** with application metadata exposed in /actuator/info

** Extend the appropriate controller base class and call `resolveExecutionContext()` – full context resolution with Security Center integration is automatic!**

### Infrastructure Components Included

-  **Banner** - Firefly Application Layer banner (banner.txt)
-  **Health Checks** - ApplicationLayerHealthIndicator for /actuator/health
-  **Structured Logging** - JSON logging via logback-spring.xml with MDC support
-  **Caching** - Integration with lib-common-cache (Caffeine/Redis)
-  **CQRS Support** - Command/Query separation via lib-common-cqrs
-  **Event-Driven Architecture** - Event publishing via lib-common-eda
-  **Actuator Integration** - Full Spring Boot Actuator support

### Application Metadata with @FireflyApplication

Declare metadata about your microservice for service discovery, monitoring, and governance:

```java
@FireflyApplication(
    name = "customer-onboarding",
    displayName = "Customer Onboarding Service",
    description = "Orchestrates customer onboarding: KYC verification, document upload, and account setup",
    domain = "customer",
    team = "customer-experience",
    owners = {"john.doe@getfirefly.io", "jane.smith@getfirefly.io"},
    apiBasePath = "/api/v1/onboarding",
    usesServices = {"customer-domain-people", "common-platform-customer-mgmt", "kyc-provider-api"},
    capabilities = {"Customer Identity Verification", "Document Management", "Account Creation"}
)
@SpringBootApplication
public class CustomerOnboardingApplication {
    public static void main(String[] args) {
        SpringApplication.run(CustomerOnboardingApplication.class, args);
    }
}
```

**Benefits:**
- 📊 **Service Discovery** - Automatic catalog of all microservices
- 👥 **Ownership Tracking** - Know who owns what
- 🔗 **Dependency Mapping** - Visualize service dependencies
- 📈 **Monitoring** - Metadata exposed via `/actuator/info`
- 📖 **Self-Documentation** - Services document themselves

### Context Architecture

The library provides a comprehensive context model for application requests:

```java
ApplicationExecutionContext {
    AppContext context;          // Business context (partyId, contractId, productId, roles)
    AppConfig config;            // Tenant configuration (providers, feature flags)
    AppSecurityContext security; // Security requirements and authorization
}
```

**AppContext** - Business domain context (party, contract, product) from platform services
**AppConfig** - Multi-tenant configuration and provider settings
**AppSecurityContext** - Security requirements and authorization results

All TODO placeholders are marked for SDK integration with:
- `common-platform-config-mgmt-sdk` - Tenant resolution (partyId → tenantId), configuration, providers, feature flags
- `FireflySessionManager` (Security Center) - Party sessions, contract access, roles, permissions, role scopes
- `common-platform-product-mgmt-sdk` - Product information, product-specific config (optional)

See the comprehensive architecture documentation below.

## Quick Start

### 1. Add Dependency

```xml
<dependency>
    <groupId>org.fireflyframework</groupId>
    <artifactId>lib-common-application</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. Declare Application Metadata

```java
@FireflyApplication(
    name = "my-service",
    displayName = "My Service",
    description = "Description of what this service does",
    domain = "my-domain",
    team = "my-team",
    owners = {"dev@company.com"},
    apiBasePath = "/api/v1/my-service",
    usesServices = {"domain-service-1", "platform-service-2"}
)
@SpringBootApplication
public class MyServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyServiceApplication.class, args);
    }
}
```

### 3. Choose Your Controller Base Class

**The library provides two base controller classes** that automatically resolve context based on your endpoint's scope:

####Architecture: How Context Resolution Works

```
┌──────────────────────────────────────────────────┐
│        Istio Gateway (Authentication)            │
│  - Validates JWT                                 │
│  - Injects X-Party-Id header (from JWT sub)      │
└──────────────────────┬───────────────────────────┘
                         │
                         ↓
┌──────────────────────────────────────────────────┐
│        Your Controller (Extracts Path Variables) │
│  - Extends AbstractApplicationController         │
│    OR AbstractResourceController                 │
│  - Extracts contractId from @PathVariable        │
│  - Extracts productId from @PathVariable         │
│  - Calls resolveExecutionContext(exchange, ...)  │
└──────────────────────┬───────────────────────────┘
                         │
                         ↓
┌──────────────────────────────────────────────────┐
│        DefaultContextResolver (Library)          │
│  1. Extracts partyId from X-Party-Id header      │
│  2. Calls config-mgmt to get tenantId(by partyId)│
│  3. Uses contractId/productId from controller    │
│  4. Calls FireflySessionManager (Security Center)│
│     - Get party session (contracts,roles,scopes) │
│     - Validate contract access                   │
│     - Get roles for contract/product             │
│     - Derive permissions from roles              │
└──────────────────────────────────────────────────┘
```

**Key Points:**
- **Istio handles authentication** → Injects `X-Party-Id` header (from JWT)
- **Config-mgmt resolves tenant** → `GET /api/v1/parties/{partyId}/tenant`
- **Controllers extract path variables** → `@PathVariable UUID contractId/productId`
- **FireflySessionManager (Security Center)** → Provides party session: contracts, roles, permissions, scopes
- **Library resolves full context** → Party + Tenant + Contract + Product + Roles + Permissions + Config
- **@Secure / EndpointSecurityRegistry** → Validates authorization using resolved context

#### 🎯 Option 1: Application-Layer Endpoints (Onboarding, Product Catalog)

Use `AbstractApplicationController` for endpoints that don't require contract or product context:

```java
@RestController
@RequestMapping("/api/v1/onboarding")
public class OnboardingController extends AbstractApplicationController {
    
    @Autowired
    private OnboardingApplicationService onboardingService;
    
    @PostMapping("/start")
    @Secure(requireParty = true, requireRole = "customer:onboard")
    public Mono<OnboardingResponse> startOnboarding(
            @RequestBody OnboardingRequest request,
            ServerWebExchange exchange) {
        
        // Automatically resolves: party + tenant (no contract/product)
        return resolveExecutionContext(exchange)
            .flatMap(context -> onboardingService.startOnboarding(context, request));
    }
}
```

**Context resolved:** Party ID, Tenant ID, Roles, Permissions, Config

---

#### Option 2: Resource Endpoints (Accounts, Transactions, Cards)

Use `AbstractResourceController` for endpoints that require BOTH contract and product context:

```java
@RestController
@RequestMapping("/api/v1/contracts/{contractId}/products/{productId}/transactions")
public class TransactionController extends AbstractResourceController {
    
    @Autowired
    private TransactionApplicationService transactionService;
    
    @GetMapping
    @Secure(requireParty = true, requireContract = true, requireProduct = true, requireRole = "transaction:viewer")
    public Mono<List<TransactionDTO>> listTransactions(
            @PathVariable UUID contractId,
            @PathVariable UUID productId,
            ServerWebExchange exchange) {
        
        // Automatically resolves: party + tenant + contract + product
        return resolveExecutionContext(exchange, contractId, productId)
            .flatMap(context -> transactionService.listTransactions(context));
    }
}
```

**Context resolved:** Party ID, Tenant ID, Contract ID, Product ID, Roles (party+contract+product), Permissions, Config

---

**What the library handles automatically:**
- Extracts `partyId` from Istio header (`X-Party-Id`)
- Resolves `tenantId` by calling `common-platform-config-mgmt` with the partyId
- Uses `contractId` and `productId` from your `@PathVariable` annotations
- Enriches context with roles and permissions from platform SDKs
- Loads tenant configuration (providers, feature flags, settings)
- Validates security requirements from `@Secure` annotations
- Returns 401/403 for unauthorized requests

---

#### Advanced: Custom Context Resolution (Optional)

If you need custom context resolution logic (e.g., non-Istio environments), you can provide your own implementations:

**Custom Context Resolver:**
```java
@Component
@Primary // Override the default
public class CustomContextResolver extends AbstractContextResolver {
    
    @Override
    public Mono<UUID> resolvePartyId(ServerWebExchange exchange) {
        // Custom logic: extract from JWT, session, etc.
        return extractFromJWT(exchange, "sub");
    }
    
    @Override
    public Mono<UUID> resolveTenantId(ServerWebExchange exchange) {
        // Custom logic: extract from subdomain, header, etc.
        return extractFromSubdomain(exchange);
    }
}
```

**Custom Config Resolver:**
```java
@Component
@Primary // Override the default
public class CustomConfigResolver extends AbstractConfigResolver {
    
    private final ConfigManagementClient configClient;
    
    @Override
    protected Mono<AppConfig> fetchConfigFromPlatform(UUID tenantId) {
        return configClient.getTenantConfig(tenantId)
            .map(response -> AppConfig.builder()
                .tenantId(response.getTenantId())
                .tenantName(response.getName())
                .providers(response.getProviders())
                .featureFlags(response.getFeatureFlags())
                .settings(response.getSettings())
                .build());
    }
}
```


#### AbstractApplicationService - Business Process Orchestration

**Purpose:** Base class for application services that orchestrate business processes.

**Provides helper methods:**
- `resolveExecutionContext(ServerWebExchange)` - Resolves full ApplicationExecutionContext
- `validateContext(context, requireContract, requireProduct)` - Validates required IDs  
- `requireRole(context, role)` - Throws AccessDeniedException if role missing
- `requirePermission(context, permission)` - Throws AccessDeniedException if permission missing
- `getProviderConfig(context, providerType)` - Gets provider configuration (KYC, payment gateway, etc.)
- `isFeatureEnabled(context, feature)` - Checks if feature flag is enabled

```java
@Service
public class CustomerOnboardingService extends AbstractApplicationService {
    
    private final CustomerDomainService customerDomain;
    private final KycProviderService kycProvider;
    
    // Constructor with required AbstractApplicationService dependencies
    public CustomerOnboardingService(
            ContextResolver contextResolver,
            ConfigResolver configResolver,
            SecurityAuthorizationService authorizationService,
            CustomerDomainService customerDomain,
            KycProviderService kycProvider) {
        super(contextResolver, configResolver, authorizationService);
        this.customerDomain = customerDomain;
        this.kycProvider = kycProvider;
    }
    
    public Mono<Customer> onboardCustomer(ServerWebExchange exchange, OnboardingRequest request) {
        return resolveExecutionContext(exchange)
            // Validate we have partyId and tenantId (no contract/product required)
            .flatMap(ctx -> validateContext(ctx, false, false))
            
            // Check permission
            .flatMap(ctx -> requirePermission(ctx, "CREATE_CUSTOMER")
                .thenReturn(ctx))
            
            // Orchestrate business process: KYC → Domain Service
            .flatMap(ctx -> 
                kycProvider.verify(ctx, request)
                    .flatMap(kycResult -> customerDomain.createCustomer(ctx, request, kycResult))
            );
    }
}
```

---

#### Optional: AbstractResourceController Helper Methods

**Purpose:** `AbstractResourceController` provides helper methods for resource endpoints.

**What it provides:**
- `requireContext(UUID, UUID)` - Validates both contractId and productId (automatically called by `resolveExecutionContext`)
- `logOperation(UUID, UUID, String)` - Logs operation with full resource context

**Example:**

```java
@RestController
@RequestMapping("/api/v1/contracts/{contractId}/products/{productId}/transactions")
public class TransactionController extends AbstractResourceController {
    
    @Autowired
    private TransactionApplicationService transactionService;
    
    @GetMapping
    @Secure(roles = "TRANSACTION_VIEWER")
    public Mono<List<TransactionDto>> listTransactions(
            @PathVariable UUID contractId,
            @PathVariable UUID productId,
            ServerWebExchange exchange) {
        
        // Log the operation (optional)
        logOperation(contractId, productId, "listTransactions");
        
        // Context resolution automatically validates both IDs
        return resolveExecutionContext(exchange, contractId, productId)
            .flatMap(context -> transactionService.listTransactions(context));
    }
}
```

**Note:** These helpers are **completely optional** – use them only if they add value.

---

## Key Features

### Context Management (Controller-Based)
- **Istio Integration**: `X-Party-Id` extracted from header (Istio-injected after JWT validation)
- **Tenant Resolution**: `tenantId` resolved by calling `common-platform-config-mgmt` with partyId
- **Path Variable Extraction**: `contractId` and `productId` extracted from `@PathVariable` in controllers
- **Automatic Enrichment**: Roles and permissions fetched from platform SDKs based on party+contract+product
- **Two Controller Types**: `AbstractApplicationController`, `AbstractResourceController`
- **Clear Scoping**: Application-layer (no contract/product) or Resource-layer (contract + product both required)
- **Caching**: Built-in caching for performance optimization
- **Immutability**: Thread-safe context objects

### Security & Authorization (Controller-Based)
- **Declarative**: `@Secure` annotation with `requireParty`, `requireContract`, `requireProduct`, `requireRole`, `requirePermission`
- **Context-Aware**: Security validation based on fully resolved ApplicationExecutionContext
- **Default Authorization**: `DefaultSecurityAuthorizationService` validates roles and permissions automatically
- **SecurityCenter Ready**: Integration points for complex authorization policies
- **Role & Permission Based**: Fine-grained access control based on party role in contract/product
- **Flexible**: Works with both controller types (application, resource)

### Configuration Management
- **Multi-tenant**: Per-tenant configuration and provider settings
- **Feature Flags**: A/B testing and gradual rollouts
- **Provider Configs**: Payment gateways, KYC providers, etc.
- **Caching**: Configurable TTL for performance

### Business Process Orchestration
- **Cross-domain Coordination**: Orchestrate multiple domain services
- **Transaction Management**: Clear transaction boundaries
- **Error Handling**: Compensating transactions support
- **Reactive**: Fully non-blocking with Project Reactor

## Architecture Patterns

This library implements several industry-standard patterns:

1. **Application Service Pattern** - Business process orchestration
2. **Context Object Pattern** - Immutable request context
3. **Strategy Pattern** - Pluggable context resolution
4. **Decorator Pattern** - AOP-based security
5. **Registry Pattern** - Endpoint security configuration
6. **Template Method Pattern** - Consistent resolver structure
7. **Facade Pattern** - Simplified domain access

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for detailed explanations.

## Complete Documentation

### Essential Reading

| Document | Description | When to Read |
|----------|-------------|-------------|
| **[ARCHITECTURE.md](docs/ARCHITECTURE.md)** | Complete architectural overview, three-layer explanation, patterns, ADRs | ⭐ **START HERE** - Understanding the architecture |
| **[EXAMPLE_MICROSERVICE_ARCHITECTURE.md](docs/EXAMPLE_MICROSERVICE_ARCHITECTURE.md)** | Complete production-ready microservice example with code | 🔨 **Building your first service** |
| **[SECURITY_GUIDE.md](docs/SECURITY_GUIDE.md)** | Complete security guide: annotations, registry, SecurityCenter integration | 🔒 Implementing authentication & authorization |
| **[USAGE_GUIDE.md](docs/USAGE_GUIDE.md)** | Step-by-step implementation guide with examples | 🚀 Getting started guide |
| **[API_REFERENCE.md](docs/API_REFERENCE.md)** | Complete API documentation for all classes and methods | 📑 Reference when developing |

### Architecture Documentation

The [ARCHITECTURE.md](docs/ARCHITECTURE.md) document provides:
- Complete architectural overview with layer responsibilities
- 7 design patterns explained with examples
- Detailed data flow diagrams for request processing
- Integration points with platform services
- 10 Architecture Decision Records (ADRs)
- Best practices and guidelines

### Usage Guide

The [USAGE_GUIDE.md](docs/USAGE_GUIDE.md) includes:
- Getting started instructions
- Step-by-step basic implementation
- Advanced patterns (compensating transactions, multi-step processes)
- Testing strategies (unit and integration tests)
- Troubleshooting common issues
- Debug tips and monitoring

### API Reference

The [API_REFERENCE.md](docs/API_REFERENCE.md) provides:
- Complete class and method documentation
- All annotation attributes explained
- Usage examples for every component
- Configuration properties reference
- Error handling patterns
- Reactive programming patterns

## Integration Points

The library provides clear integration points (marked with TODO) for:

### 1. Configuration Management (`common-platform-config-mgmt-sdk`) ⭐
**Purpose:** Tenant resolution and multi-tenant configuration
- **Tenant Resolution:** `GET /api/v1/parties/{partyId}/tenant` → Returns tenantId for a party
- Fetch tenant configuration (providers, settings)
- Get provider configurations (KYC, payment gateways, etc.)
- Retrieve feature flags
- Manage tenant-specific settings

### 2. FireflySessionManager (Security Center) **INTEGRATED**
**Purpose:** Authorization, session management, role/permission resolution

**Status:** **Fully integrated and operational**

**Integration Points:**
- `DefaultContextResolver` - Automatically resolves roles and permissions from session
- `DefaultSecurityAuthorizationService` - Validates product access and permissions
- `SessionContextMapper` - Utility for extracting roles/permissions from session data
- Graceful degradation when Security Center is unavailable

**Key Features:**
- **Party Session Management:** Tracks which contracts a party has access to
- **Contract Access Validation:** Validates if party can access specific contract/product
- **Role Resolution:** Gets party roles in contract (owner, viewer, etc.)
- **Role Scopes:** Supports party-level, contract-level, product-level roles
- **Permission Derivation:** Converts roles to permissions using role scopes
- **Session Caching:** Caches party sessions for performance (via Redis/Caffeine)

**How It Works:**
```java
// 1. FireflySessionManager called automatically by DefaultContextResolver
sessionManager.createOrGetSession(exchange)
    .map(session -> {
        // 2. Extract roles based on context (party/contract/product)
        Set<String> roles = SessionContextMapper.extractRoles(
            session, contractId, productId
        );
        
        // 3. Extract permissions from role scopes
        Set<String> permissions = SessionContextMapper.extractPermissions(
            session, contractId, productId
        );
        
        return AppContext with roles and permissions;
    });

// 4. Authorization checks product access automatically
sessionManager.hasAccessToProduct(partyId, productId);
sessionManager.hasPermission(partyId, productId, "READ", "BALANCE");
```

**Permission Format:** `{roleCode}:{actionType}:{resourceType}`
- Example: `owner:READ:BALANCE`, `account_viewer:READ:TRANSACTION`

### 3. Product Management (`common-platform-product-mgmt-sdk`) (Optional)
**Purpose:** Product-specific information and configuration
- Resolve product information
- Fetch product-specific configuration
- Validate product status and availability
- Get product features and limits

## Design Principles

### SOLID Principles
- **Single Responsibility**: Each class has one clear purpose
- **Open/Closed**: Extensible via abstract classes and interfaces
- **Liskov Substitution**: All abstractions can be safely substituted
- **Interface Segregation**: Focused, cohesive interfaces
- **Dependency Inversion**: Depend on abstractions, not concretions

### Best Practices
- **Immutability**: All context objects are immutable
- **Reactive First**: Non-blocking, composable operations
- **Fail Fast**: Early validation prevents cascading failures
- **Explicit Context**: No ThreadLocal magic, clear data flow
- **Separation of Concerns**: Security, business logic, infrastructure separated

## Configuration

```yaml
firefly:
  application:
    security:
      enabled: true                    # Enable security features
      use-security-center: true        # Delegate to SecurityCenter
      default-roles: []                # Default roles
      fail-on-missing: false           # Fail on missing config
    context:
      cache-enabled: true              # Enable context caching
      cache-ttl: 300                   # Cache TTL (seconds)
      cache-max-size: 1000            # Maximum cache size
    config:
      cache-enabled: true              # Enable config caching
      cache-ttl: 600                   # Cache TTL (seconds)
      refresh-on-startup: false       # Refresh on startup
```

## Caching

The library integrates with `lib-common-cache` (FireflyCacheManager) to provide efficient tenant configuration caching.

### What's Cached?

- **Tenant Configurations**: Cached with 1-hour TTL (configurable)
  - Tenant settings
  - Provider configurations (KYC, payment gateways)
  - Feature flags
  - Multi-tenant settings

**Cache Key Format**: `firefly:application:config:{tenantId}`
- Example: `firefly:application:config:123e4567-e89b-12d3-a456-426614174000`
- Follows Firefly naming conventions for proper namespace organization

### Cache Configuration

Caching is configured via `application.yml` and auto-configured by `lib-common-cache`:

```yaml
firefly:
  cache:
    enabled: true
    default-cache-type: CAFFEINE  # Use Caffeine (in-memory) by default
    metrics-enabled: true
    health-enabled: true
    
    caffeine:
      cache-name: application-layer
      enabled: true
      key-prefix: "firefly:application"  # Prefix for all cache keys (Firefly naming convention)
      maximum-size: 1000                 # Maximum cached configurations
      expire-after-write: PT1H           # Tenant configs expire after 1 hour
      record-stats: true                 # Enable cache statistics
```

### 🔄 Cache Behavior

**With FireflyCacheManager Available:**
- Tenant configs cached with TTL
- Automatic eviction policies
- Cache statistics and monitoring
- Support for distributed caching (Redis)

**Without FireflyCacheManager (Graceful Degradation):**
- Caching disabled - fetches from platform every time
- Service continues to function normally
- Higher latency and platform load

### Cache Operations

**Manual Cache Control:**

```java
@Service
public class ConfigManagementService {
    
    @Autowired
    private ConfigResolver configResolver;
    
    // Refresh config for specific tenant
    public Mono<AppConfig> refreshTenantConfig(UUID tenantId) {
        return configResolver.refreshConfig(tenantId);
    }
    
    // Check if tenant config is cached
    public Mono<Boolean> isConfigCached(UUID tenantId) {
        return configResolver.isCached(tenantId);
    }
}
```

### Cache Monitoring

Cache metrics are exposed via Spring Boot Actuator:

```bash
# View cache health
GET /actuator/health/cache

# View cache statistics
GET /actuator/caches

# View cache metrics
GET /actuator/metrics/cache.gets
GET /actuator/metrics/cache.evictions
```

### Custom TTL

You can customize cache TTL per resolver:

```java
public class CustomConfigResolver extends AbstractConfigResolver {
    
    @Override
    protected Duration getConfigTTL() {
        // Custom TTL: 30 minutes instead of default 1 hour
        return Duration.ofMinutes(30);
    }
    
    @Override
    protected Mono<AppConfig> fetchConfigFromPlatform(UUID tenantId) {
        // Your platform integration
        return configClient.getTenantConfig(tenantId);
    }
}
```

### Best Practices

1. **Use Caffeine for Single-Instance Apps**: Fast in-memory caching
2. **Use Redis for Distributed Apps**: Shared cache across instances
3. **Monitor Cache Hit Rates**: Optimize TTL based on hit/miss ratios
4. **Set Appropriate TTL**: Balance freshness vs performance
5. **Enable Cache Statistics**: Track cache effectiveness

For more details, see [lib-common-cache documentation](../lib-common-cache/README.md).

## Plugin Architecture

The Plugin Architecture enables Firefly application microservices to act as **"containers"** exposing standard Banking as a Service (BaaS) APIs, while the underlying business logic is **pluggable**, **composable**, and **dynamically resolved** at runtime through configuration.

### Key Features

- **Pluggable Business Logic**: Swap vanilla implementations with custom ones without code changes
- **Runtime Resolution**: Process plugins resolved based on tenant, product, and channel configuration
- **Multiple Loading Strategies**: Spring beans, external JARs, or remote repositories
- **Hot-Reload Support**: Update plugins without application restart (JAR/Remote loaders)
- **Security Sandbox**: Isolated execution for external plugins
- **Observability**: Built-in health checks, metrics (Micrometer), and event publishing
- **Circuit Breaker**: Resilience4j integration for remote plugin loading
- **Execution Phase Tracking**: Detailed error context with execution phase information

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    Banking as a Service APIs                    │
│     (Standard REST Endpoints - /api/v1/accounts, etc.)          │
└───────────────────────────────┬─────────────────────────────────┘
                                │
┌───────────────────────────────▼─────────────────────────────────┐
│              EXISTING Controller Layer (UNCHANGED)              │
│   AbstractApplicationController / AbstractResourceController    │
│   • ContextResolver → partyId, tenantId, contractId             │
│   • @Secure + SecurityAspect → FireflySessionManager            │
└───────────────────────────────┬─────────────────────────────────┘
                                │
┌───────────────────────────────▼─────────────────────────────────┐
│               ENHANCED Service Layer (NEW)                      │
│    AbstractApplicationService + ProcessPluginExecutor           │
│    • executeProcess(context, operationId, input)                │
│    • Process resolved based on tenant/product config            │
└───────────────────────────────┬─────────────────────────────────┘
                                │
┌───────────────────────────────▼─────────────────────────────────┐
│                    Process Plugin Layer                         │
│   ┌────────────────┐  ┌────────────────┐  ┌────────────────┐    │
│   │ Vanilla Process│  │ Custom Process │  │ Workflow-Based │    │
│   │ (Default impl) │  │(Tenant-specif.)│  │    Process     │    │
│   └────────────────┘  └────────────────┘  └────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

### Quick Start

#### 1. Create a Process Plugin

```java
@FireflyProcess(
    id = "vanilla-account-creation",
    name = "Standard Account Creation",
    version = "1.0.0",
    capabilities = {"ACCOUNT_CREATION"},
    requiredPermissions = {"accounts:create"}
)
public class VanillaAccountCreationProcess implements ProcessPlugin {
    
    private final AccountDomainService accountService;
    
    @Override
    public Mono<ProcessResult> execute(ProcessExecutionContext context) {
        AccountCreationRequest request = context.getInput(AccountCreationRequest.class);
        return accountService.createAccount(context.getAppContext(), request)
            .map(ProcessResult::success);
    }
}
```

#### 2. Use ProcessPluginExecutor in Your Service

```java
@Service
public class AccountApplicationService extends AbstractApplicationService {
    
    private final ProcessPluginExecutor processExecutor;
    
    public Mono<AccountResponse> createAccount(
            ApplicationExecutionContext context,
            AccountCreationRequest request) {
        
        // Delegate to plugin system - process resolved based on tenant/product config
        return processExecutor.executeProcess(context, "createAccount", toMap(request))
            .map(result -> result.getOutput(AccountResponse.class));
    }
}
```

#### 3. Configure API-to-Process Mapping

In `common-platform-config-mgmt`, configure which process handles each operation:

```json
{
  "tenantId": "acme-bank-uuid",
  "operationId": "createAccount",
  "processId": "acme-bank-account-creation",
  "processVersion": "1.0.0"
}
```

### Core Components

| Component | Purpose |
|-----------|----------|
| `ProcessPlugin` | Core interface for pluggable business logic |
| `@FireflyProcess` | Annotation to mark Spring beans as plugins |
| `ProcessPluginRegistry` | Thread-safe registry of loaded plugins |
| `ProcessPluginExecutor` | Orchestration service for executing plugins |
| `ProcessMappingService` | Resolves operations to processes via config |
| `SpringBeanPluginLoader` | Discovers plugins from Spring context |
| `JarPluginLoader` | Loads plugins from external JAR files |
| `RemoteRepositoryPluginLoader` | Downloads and loads plugins from Maven/HTTP |
| `PluginEventPublisher` | Publishes lifecycle and execution events |
| `PluginMetricsService` | Collects execution metrics via Micrometer |
| `PluginHealthIndicator` | Spring Boot Actuator health endpoint |

### Configuration

```yaml
firefly:
  application:
    plugin:
      enabled: true
      loaders:
        spring-bean:
          enabled: true
          priority: 0
        jar:
          enabled: true
          priority: 10
          scan-directories:
            - /opt/firefly/plugins
          hot-reload: true
        remote:
          enabled: true
          priority: 20
          cache-directory: /var/firefly/plugin-cache
          repositories:
            - type: maven
              name: firefly-plugins
              url: https://repo.firefly.io/plugins
            - type: http
              name: custom-plugins
              url: https://plugins.mybank.com
      security:
        sandbox-enabled: true
      cache:
        enabled: true
        ttl: PT1H
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
      # Circuit breaker for remote loading
      circuit-breaker:
        enabled: true
        failure-rate-threshold: 50
        wait-duration-in-open-state: PT30S
```

### Plugin Architecture Documentation

The plugin architecture is **self-contained** within `lib-common-application`. You do **NOT** need a separate library to create plugins - simply depend on this library.

For detailed documentation, see:
- **[Plugin Development Guide](docs/PLUGIN_DEVELOPMENT_GUIDE.md)** - Comprehensive tutorial for creating plugins
- [AbstractProcessPlugin](src/main/java/org/fireflyframework/common/application/plugin/AbstractProcessPlugin.java) - Type-safe base class for plugins
- [Plugin Loading Strategies](docs/PLUGIN_LOADERS.md) - JAR, Maven, HTTP loading options

## Examples

### Simple Transfer Operation

```java
@Service
public class AccountApplicationService extends AbstractApplicationService {
    
    public Mono<Transfer> transferFunds(ServerWebExchange exchange, TransferRequest request) {
        return resolveExecutionContext(exchange)
            .flatMap(context -> validateContext(context, true, true))
            .flatMap(context -> requirePermission(context, "TRANSFER_FUNDS")
                .thenReturn(context))
            .flatMap(context -> accountDomainService.transfer(
                context.getContext(),
                request.getFromAccount(),
                request.getToAccount(),
                request.getAmount()
            ));
    }
}
```

### Multi-Step Business Process

```java
public Mono<LoanApplication> processLoan(ServerWebExchange exchange, LoanRequest request) {
    return resolveExecutionContext(exchange)
        .flatMap(ctx -> validateContext(ctx, true, true))
        .flatMap(ctx -> creditCheckService.performCheck(ctx, request)
            .zipWith(documentService.verifyDocuments(ctx, request))
            .flatMap(results -> approvalService.evaluate(ctx, results))
            .filter(ApprovalResult::isApproved)
            .flatMap(approval -> createLoan(ctx, request, approval))
        );
}
```

### Feature Flag Usage

```java
return isFeatureEnabled(context, "NEW_PAYMENT_FLOW")
    .flatMap(enabled -> enabled
        ? newPaymentProcessor.process(context, payment)
        : legacyPaymentProcessor.process(context, payment)
    );
```

## Testing

### Comprehensive Test Suite

**250+ tests - 100% passing**

The library includes extensive test coverage:
- **Context Management**: 63 tests covering AppContext, AppConfig, AppMetadata, ApplicationExecutionContext
- **Security Components**: 30 tests for AppSecurityContext, SecurityEvaluationResult, EndpointSecurityRegistry, Authorization
- **Infrastructure**: 37 tests for Configuration, Resolvers, Controllers, Services
- **Integration Tests**: 25 tests for AOP Security and Metadata Provider
- **Monitoring**: 11 tests for Health and Actuator
- **Configuration**: 14 tests for ApplicationLayerProperties and Banner
- **Plugin Architecture**: 65+ tests for ProcessPluginRegistry, ProcessPluginExecutor, AbstractProcessPlugin

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=AppContextTest

# With coverage report
mvn clean test jacoco:report
```

**Test Metrics:**
- Total Tests: 250+
- Success Rate: 100%
- Execution Time: < 5s
- Core Coverage: ~95%+

See [TESTING.md](docs/TESTING.md) for detailed documentation.

### Unit Testing Examples

```java
@ExtendWith(MockitoExtension.class)
class MyContextResolverTest {
    @Mock
    private ServerWebExchange exchange;
    
    @InjectMocks
    private MyContextResolver resolver;
    
    @Test
    void shouldResolvePartyId() {
        // Given
        UUID expectedPartyId = UUID.randomUUID();
        mockJwtToken(expectedPartyId);
        
        // When
        StepVerifier.create(resolver.resolvePartyId(exchange))
            // Then
            .expectNext(expectedPartyId)
            .verifyComplete();
    }
}
```

### Integration Testing

```java
@SpringBootTest
@AutoConfigureWebTestClient
class AccountControllerIntegrationTest {
    
    @Autowired
    private WebTestClient webClient;
    
    @Test
    @WithMockJwt(partyId = "...", roles = {"ACCOUNT_HOLDER"})
    void shouldTransferFunds() {
        webClient.post()
            .uri("/api/v1/accounts/{id}/transfer", accountId)
            .bodyValue(transferRequest)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Transfer.class)
            .value(transfer -> {
                assertThat(transfer.getStatus()).isEqualTo(COMPLETED);
            });
    }
}
```

## Performance Considerations

### Caching Strategy
- **Context Caching**: 5-minute TTL for roles/permissions
- **Config Caching**: 10-minute TTL for tenant configuration
- **LRU Eviction**: Configurable max cache size

### Circuit Breakers
```java
@CircuitBreaker(name = "customer-mgmt", fallbackMethod = "fallbackRoles")
protected Mono<Set<String>> resolveRoles(AppContext context) {
    return customerManagementClient.getPartyRoles(...);
}
```

### Timeouts
```java
return resolveExecutionContext(exchange)
    .timeout(Duration.ofSeconds(5))
    .onErrorMap(TimeoutException.class, 
        e -> new ApplicationException("Context resolution timeout"));
```

## Monitoring & Observability

### Metrics
- Context resolution duration
- Authorization success/failure rates
- Cache hit/miss ratios
- SecurityCenter call latencies

### Logging
Enable debug logging for troubleshooting:
```yaml
logging:
  level:
    org.fireflyframework.application: DEBUG
    org.fireflyframework.application.aop: TRACE
```

### Tracing
Automatic correlation ID propagation for distributed tracing:
- Request ID generation
- Correlation ID from headers or auto-generated
- Trace ID and Span ID support
- MDC integration for structured logging

## Contributing

This library follows strict architectural principles:

1. **No implementation logic** - Provide abstractions with TODO placeholders
2. **Immutability first** - Use @Value and @With from Lombok
3. **Reactive patterns** - All operations return Mono/Flux
4. **Complete JavaDoc** - Every public API must be documented
5. **Architecture decisions** - Document significant decisions in ADRs

## License

Copyright 2024-2026 Firefly Software Solutions Inc

Licensed under the Apache License, Version 2.0

## Support

For questions or issues:
- Review the [documentation](docs/)
- Check [troubleshooting guide](docs/USAGE_GUIDE.md#troubleshooting)
- Enable debug logging for diagnostics

---

**Built with ❤️ by the Firefly Development Team**