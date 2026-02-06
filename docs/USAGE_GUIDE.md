# 🚀 Usage Guide

> **Practical step-by-step guide for building Application Layer microservices**

This guide provides hands-on examples and detailed instructions for using `lib-common-application` to build production-ready microservices.

---

## 📖 Table of Contents

### 🏁 Getting Started
1. [📦 Getting Started](#getting-started)
   - Add Dependency
   - Configure Properties
   - Project Structure

### 🔨 Basic Implementation
2. [🛠️ Basic Implementation](#basic-implementation)
   - Step 1: Implement ContextResolver
   - Step 2: Implement ConfigResolver
   - Step 3: Implement AuthorizationService
   - Step 4: Create Application Service
   - Step 5: Create REST Controller
   - Step 6: Configure Security

### 🎯 Advanced Patterns
3. [🎨 Advanced Patterns](#advanced-patterns)
   - Multi-Step Workflows
   - Compensating Transactions
   - Feature Flags
   - Circuit Breakers
   - Event Publishing

### ✅ Testing
4. [🧪 Testing](#testing)
   - Unit Testing
   - Integration Testing
   - Security Testing
   - Mock Strategies

### 🔧 Troubleshooting
5. [🔍 Troubleshooting](#troubleshooting)
   - Common Issues
   - Debug Tips
   - Performance Tuning
   - FAQ

---

## Getting Started

### 1. Add Dependency

Add the library to your `pom.xml`:

```xml
<dependency>
    <groupId>org.fireflyframework</groupId>
    <artifactId>lib-common-application</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. Configure Properties

In your `application.yml`:

```yaml
firefly:
  application:
    security:
      enabled: true
      use-security-center: true
      default-roles: []
    context:
      cache-enabled: true
      cache-ttl: 300  # seconds
      cache-max-size: 1000
    config:
      cache-enabled: true
      cache-ttl: 600  # seconds
```

## Basic Implementation

### Step 1: Implement ContextResolver

Create a context resolver for your microservice:

```java
package com.mycompany.myservice.application.resolver;

import org.fireflyframework.application.context.AppContext;
import org.fireflyframework.application.resolver.AbstractContextResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Custom context resolver that extracts context from JWT tokens.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtContextResolver extends AbstractContextResolver {
    
    @Override
    public Mono<UUID> resolvePartyId(ServerWebExchange exchange) {
        return ReactiveSecurityContextHolder.getContext()
            .map(securityContext -> securityContext.getAuthentication())
            .filter(auth -> auth.getPrincipal() instanceof Jwt)
            .map(auth -> (Jwt) auth.getPrincipal())
            .map(jwt -> jwt.getClaimAsString("partyId"))
            .map(UUID::fromString)
            .switchIfEmpty(extractUUID(exchange, "partyId", "X-Party-Id"));
    }
    
    @Override
    public Mono<UUID> resolveTenantId(ServerWebExchange exchange) {
        return ReactiveSecurityContextHolder.getContext()
            .map(securityContext -> securityContext.getAuthentication())
            .filter(auth -> auth.getPrincipal() instanceof Jwt)
            .map(auth -> (Jwt) auth.getPrincipal())
            .map(jwt -> jwt.getClaimAsString("tenantId"))
            .map(UUID::fromString)
            .switchIfEmpty(extractUUID(exchange, "tenantId", "X-Tenant-Id"));
    }
    
    @Override
    public Mono<UUID> resolveContractId(ServerWebExchange exchange) {
        // First try path variable, then header
        return extractUUIDFromPath(exchange, "contractId")
            .switchIfEmpty(extractUUID(exchange, "contractId", "X-Contract-Id"));
    }
    
    @Override
    public Mono<UUID> resolveProductId(ServerWebExchange exchange) {
        // First try path variable, then header
        return extractUUIDFromPath(exchange, "productId")
            .switchIfEmpty(extractUUID(exchange, "productId", "X-Product-Id"));
    }
    
    @Override
    public boolean supports(ServerWebExchange exchange) {
        // This resolver supports requests with JWT authentication
        return exchange.getRequest().getHeaders().containsKey("Authorization");
    }
    
    @Override
    public int getPriority() {
        return 100; // Higher priority than default
    }
}
```

### Step 2: Implement ConfigResolver

Create a config resolver that integrates with your config management service:

```java
package com.mycompany.myservice.application.resolver;

import org.fireflyframework.application.context.AppConfig;
import org.fireflyframework.application.resolver.AbstractConfigResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Custom config resolver that fetches tenant configuration.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TenantConfigResolver extends AbstractConfigResolver {
    
    // TODO: Inject config-mgmt SDK client
    // private final ConfigManagementClient configClient;
    
    @Override
    protected Mono<AppConfig> fetchConfigFromPlatform(UUID tenantId) {
        // TODO: Implement using common-platform-config-mgmt-sdk
        /*
        return configClient.getTenantConfig(tenantId)
            .map(response -> AppConfig.builder()
                .tenantId(response.getTenantId())
                .tenantName(response.getName())
                .providers(convertProviders(response.getProviders()))
                .featureFlags(response.getFeatureFlags())
                .settings(response.getSettings())
                .environment(response.getEnvironment())
                .active(response.isActive())
                .build());
        */
        
        log.info("Fetching configuration for tenant: {}", tenantId);
        
        // Temporary implementation for development
        Map<String, AppConfig.ProviderConfig> providers = new HashMap<>();
        providers.put("PAYMENT_GATEWAY", AppConfig.ProviderConfig.builder()
            .providerType("PAYMENT_GATEWAY")
            .implementation("StripeProvider")
            .enabled(true)
            .properties(Map.of(
                "apiKey", "sk_test_...",
                "webhookSecret", "whsec_..."
            ))
            .build());
        
        return Mono.just(AppConfig.builder()
            .tenantId(tenantId)
            .tenantName("Default Tenant")
            .providers(providers)
            .featureFlags(Map.of("NEW_TRANSFER_FLOW", true))
            .settings(Map.of("currency", "USD"))
            .environment("development")
            .active(true)
            .build());
    }
}
```

### Step 3: Implement AuthorizationService

Create an authorization service:

```java
package com.mycompany.myservice.application.security;

import org.fireflyframework.application.security.AbstractSecurityAuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Custom authorization service with SecurityCenter integration.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CustomAuthorizationService extends AbstractSecurityAuthorizationService {
    
    // TODO: Inject SecurityCenter client when available
    // private final SecurityCenterClient securityCenterClient;
    
    // The base class provides default implementation
    // Override authorizeWithSecurityCenter() when SecurityCenter is ready
}
```

### Step 4: Create Application Service

Create an application service that extends `AbstractApplicationService`:

```java
package com.mycompany.myservice.application.service;

import org.fireflyframework.application.context.ApplicationExecutionContext;
import org.fireflyframework.application.resolver.ConfigResolver;
import org.fireflyframework.application.resolver.ContextResolver;
import org.fireflyframework.application.security.SecurityAuthorizationService;
import org.fireflyframework.application.service.AbstractApplicationService;
import com.mycompany.myservice.domain.model.Transfer;
import com.mycompany.myservice.domain.service.AccountDomainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Application service for account operations.
 * Orchestrates business processes and manages context.
 */
@Service
@Slf4j
public class AccountApplicationService extends AbstractApplicationService {
    
    private final AccountDomainService accountDomainService;
    
    public AccountApplicationService(
        ContextResolver contextResolver,
        ConfigResolver configResolver,
        SecurityAuthorizationService authorizationService,
        AccountDomainService accountDomainService
    ) {
        super(contextResolver, configResolver, authorizationService);
        this.accountDomainService = accountDomainService;
    }
    
    /**
     * Transfer funds between accounts.
     * This method orchestrates the entire transfer process including:
     * - Context resolution
     * - Security validation
     * - Business logic execution
     * 
     * @param exchange the server web exchange
     * @param request the transfer request
     * @return Mono of Transfer result
     */
    public Mono<Transfer> transferFunds(ServerWebExchange exchange, TransferRequest request) {
        return resolveExecutionContext(exchange)
            // Validate that we have contract and product context
            .flatMap(context -> validateContext(context, true, true))
            // Check permissions
            .flatMap(context -> requirePermission(context, "TRANSFER_FUNDS")
                .thenReturn(context))
            // Check feature flag
            .flatMap(context -> isFeatureEnabled(context, "NEW_TRANSFER_FLOW")
                .flatMap(enabled -> enabled
                    ? executeNewTransferFlow(context, request)
                    : executeLegacyTransferFlow(context, request)));
    }
    
    private Mono<Transfer> executeNewTransferFlow(
        ApplicationExecutionContext context,
        TransferRequest request
    ) {
        log.info("Executing new transfer flow for party: {}", context.getPartyId());
        
        // Get payment provider configuration
        return getProviderConfig(context, "PAYMENT_GATEWAY")
            .flatMap(providerConfig -> {
                log.debug("Using payment provider: {}", providerConfig.getImplementation());
                
                // Orchestrate the transfer
                return accountDomainService.transfer(
                    context.getContext(),
                    request.getFromAccount(),
                    request.getToAccount(),
                    request.getAmount()
                );
            });
    }
    
    private Mono<Transfer> executeLegacyTransferFlow(
        ApplicationExecutionContext context,
        TransferRequest request
    ) {
        log.info("Executing legacy transfer flow for party: {}", context.getPartyId());
        
        return accountDomainService.legacyTransfer(
            context.getContext(),
            request.getFromAccount(),
            request.getToAccount(),
            request.getAmount()
        );
    }
}
```

### Step 5: Create Controller with Security

Create a controller that uses the `@Secure` annotation:

```java
package com.mycompany.myservice.presentation.controller;

import org.fireflyframework.application.security.annotation.Secure;
import com.mycompany.myservice.application.service.AccountApplicationService;
import com.mycompany.myservice.domain.model.Transfer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;

/**
 * REST controller for account operations.
 */
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Slf4j
@Secure(roles = {"ACCOUNT_HOLDER", "ACCOUNT_ADMIN"})
public class AccountController {
    
    private final AccountApplicationService applicationService;
    
    /**
     * Transfer funds between accounts.
     * Requires ACCOUNT_HOLDER role and TRANSFER_FUNDS permission.
     */
    @PostMapping("/{accountId}/transfer")
    @Secure(
        roles = "ACCOUNT_HOLDER",
        permissions = "TRANSFER_FUNDS",
        description = "Transfer funds between accounts"
    )
    public Mono<Transfer> transferFunds(
        @PathVariable UUID accountId,
        @Valid @RequestBody TransferRequest request,
        ServerWebExchange exchange
    ) {
        log.info("Transfer request received for account: {}", accountId);
        return applicationService.transferFunds(exchange, request);
    }
    
    /**
     * Get account balance.
     * Requires any account-related role.
     */
    @GetMapping("/{accountId}/balance")
    @Secure(
        roles = {"ACCOUNT_HOLDER", "ACCOUNT_VIEWER"},
        requireAllRoles = false  // Any of the roles is sufficient
    )
    public Mono<BalanceResponse> getBalance(
        @PathVariable UUID accountId,
        ServerWebExchange exchange
    ) {
        return applicationService.getBalance(exchange, accountId);
    }
    
    /**
     * Public endpoint that doesn't require authentication.
     */
    @GetMapping("/public/rates")
    @Secure(allowAnonymous = true)
    public Mono<RatesResponse> getExchangeRates() {
        return applicationService.getPublicRates();
    }
}
```

## Advanced Patterns

### Pattern 1: Programmatic Security Registration

Register security rules programmatically:

```java
package com.mycompany.myservice.config;

import org.fireflyframework.application.security.EndpointSecurityRegistry;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class SecurityConfiguration {
    
    private final EndpointSecurityRegistry registry;
    
    public SecurityConfiguration(EndpointSecurityRegistry registry) {
        this.registry = registry;
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void registerSecurityRules() {
        // Register endpoint security
        registry.registerEndpoint(
            "/api/v1/admin/users",
            "POST",
            EndpointSecurityRegistry.EndpointSecurity.builder()
                .roles(Set.of("ADMIN", "USER_MANAGER"))
                .requireAllRoles(false)
                .build()
        );
        
        registry.registerEndpoint(
            "/api/v1/accounts/{id}/close",
            "DELETE",
            EndpointSecurityRegistry.EndpointSecurity.builder()
                .roles(Set.of("ACCOUNT_OWNER"))
                .permissions(Set.of("CLOSE_ACCOUNT"))
                .requireAllRoles(true)
                .requireAllPermissions(true)
                .build()
        );
    }
}
```

### Pattern 2: Custom Context Enrichment

Override enrichment logic for special cases:

```java
@Component
public class EnrichedContextResolver extends AbstractContextResolver {
    
    @Override
    protected Mono<AppContext> enrichContext(
        AppContext basicContext,
        ServerWebExchange exchange,
        AppMetadata metadata
    ) {
        // Call parent enrichment first
        return super.enrichContext(basicContext, exchange, metadata)
            .flatMap(context -> {
                // Add custom enrichment
                Map<String, Object> attributes = new HashMap<>();
                attributes.put("ipAddress", metadata.getClientIp());
                attributes.put("userAgent", metadata.getUserAgent());
                
                return Mono.just(context.toBuilder()
                    .attributes(attributes)
                    .build());
            });
    }
}
```

### Pattern 3: Multi-Step Business Process

Orchestrate complex multi-step processes:

```java
@Service
public class LoanApplicationService extends AbstractApplicationService {
    
    private final CreditCheckService creditCheckService;
    private final DocumentService documentService;
    private final ApprovalService approvalService;
    
    public Mono<LoanApplication> processLoanApplication(
        ServerWebExchange exchange,
        LoanApplicationRequest request
    ) {
        return resolveExecutionContext(exchange)
            // Step 1: Validate context
            .flatMap(ctx -> validateContext(ctx, true, true))
            
            // Step 2: Check credit
            .flatMap(ctx -> creditCheckService.performCreditCheck(ctx, request)
                .map(creditResult -> new ProcessState(ctx, request, creditResult)))
            
            // Step 3: Verify documents
            .flatMap(state -> documentService.verifyDocuments(state.context, state.request)
                .map(docResult -> state.withDocumentResult(docResult)))
            
            // Step 4: Get approval
            .flatMap(state -> approvalService.getApproval(state.context, state)
                .map(approval -> state.withApproval(approval)))
            
            // Step 5: Create loan if approved
            .flatMap(state -> state.approval.isApproved()
                ? createLoan(state)
                : Mono.error(new LoanRejectedException(state.approval.getReason())));
    }
    
    @lombok.Value
    @lombok.With
    private static class ProcessState {
        ApplicationExecutionContext context;
        LoanApplicationRequest request;
        CreditCheckResult creditResult;
        DocumentVerificationResult documentResult;
        ApprovalResult approval;
        
        ProcessState(ApplicationExecutionContext context, 
                    LoanApplicationRequest request,
                    CreditCheckResult creditResult) {
            this.context = context;
            this.request = request;
            this.creditResult = creditResult;
            this.documentResult = null;
            this.approval = null;
        }
    }
}
```

### Pattern 4: Compensating Transactions

Handle failures with compensating transactions:

```java
public Mono<TransferResult> transferWithCompensation(
    ApplicationExecutionContext context,
    TransferRequest request
) {
    return accountDomainService.debit(context.getContext(), request.getFromAccount(), request.getAmount())
        .flatMap(debitResult -> 
            accountDomainService.credit(context.getContext(), request.getToAccount(), request.getAmount())
                .onErrorResume(creditError -> {
                    // Compensate: reverse the debit
                    log.error("Credit failed, compensating debit", creditError);
                    return accountDomainService.credit(
                        context.getContext(),
                        request.getFromAccount(),
                        request.getAmount()
                    ).then(Mono.error(creditError));
                })
        );
}
```

## Testing

### Unit Testing Context Resolution

```java
@ExtendWith(MockitoExtension.class)
class JwtContextResolverTest {
    
    @Mock
    private ServerWebExchange exchange;
    
    @Mock
    private ServerHttpRequest request;
    
    @InjectMocks
    private JwtContextResolver resolver;
    
    @Test
    void shouldResolvePartyIdFromJwt() {
        // Given
        UUID expectedPartyId = UUID.randomUUID();
        Jwt jwt = createJwtWithPartyId(expectedPartyId);
        
        when(exchange.getRequest()).thenReturn(request);
        mockSecurityContext(jwt);
        
        // When
        StepVerifier.create(resolver.resolvePartyId(exchange))
            // Then
            .expectNext(expectedPartyId)
            .verifyComplete();
    }
}
```

### Integration Testing Application Service

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class AccountApplicationServiceIntegrationTest {
    
    @Autowired
    private WebTestClient webClient;
    
    @Test
    @WithMockJwtAuth(partyId = "123e4567-e89b-12d3-a456-426614174000", roles = {"ACCOUNT_HOLDER"})
    void shouldTransferFunds() {
        TransferRequest request = new TransferRequest(
            fromAccount,
            toAccount,
            BigDecimal.valueOf(100.00)
        );
        
        webClient.post()
            .uri("/api/v1/accounts/{id}/transfer", accountId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Transfer.class)
            .value(transfer -> {
                assertThat(transfer.getAmount()).isEqualTo(request.getAmount());
                assertThat(transfer.getStatus()).isEqualTo(TransferStatus.COMPLETED);
            });
    }
}
```

## Troubleshooting

### Common Issues

#### 1. Context Resolution Fails

**Problem**: `ApplicationExecutionContext` cannot be resolved.

**Solution**:
- Verify that your `ContextResolver` is properly registered as a Spring bean
- Check that the `supports()` method returns true for your requests
- Verify authentication headers/tokens are present
- Enable debug logging: `logging.level.org.fireflyframework.application=DEBUG`

#### 2. Security Checks Fail

**Problem**: Receiving 403 Forbidden despite having correct roles.

**Solution**:
- Verify roles are being resolved correctly in `resolveRoles()`
- Check that permissions are being resolved in `resolvePermissions()`
- Ensure `@Secure` annotation is being processed (AOP is enabled)
- Check security configuration in application.yml

#### 3. Configuration Not Loading

**Problem**: `AppConfig` is empty or missing provider configurations.

**Solution**:
- Verify `ConfigResolver` is properly implemented
- Check connection to config-mgmt service
- Verify cache settings in application.yml
- Call `refreshConfig()` to force reload

#### 4. AOP Not Working

**Problem**: `@Secure` annotation is ignored.

**Solution**:
- Verify `@EnableAspectJAutoProxy` is present
- Check that `SecurityAspect` is registered as a bean
- Ensure methods are being called from outside the class (AOP proxy)
- Verify AspectJ dependencies are on classpath

### Debug Tips

Enable verbose logging:

```yaml
logging:
  level:
    org.fireflyframework.application: DEBUG
    org.fireflyframework.application.aop: TRACE
    org.fireflyframework.application.resolver: DEBUG
    org.fireflyframework.application.security: DEBUG
```

Use Actuator for introspection:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,beans,mappings
```

Monitor metrics:

```java
@Component
public class ApplicationMetrics {
    
    private final MeterRegistry meterRegistry;
    
    public void recordContextResolution(String resolver, long duration) {
        meterRegistry.timer("app.context.resolution",
            "resolver", resolver)
            .record(Duration.ofMillis(duration));
    }
}
```

## Next Steps

- Review [ARCHITECTURE.md](ARCHITECTURE.md) for architectural patterns
- Read [API_REFERENCE.md](API_REFERENCE.md) for detailed API documentation
- Check [EXAMPLES.md](EXAMPLES.md) for more code examples
- See [MIGRATION_GUIDE.md](MIGRATION_GUIDE.md) for upgrading existing applications
