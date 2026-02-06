# Firefly Plugin Development Guide

This guide walks you through creating process plugins for the Firefly Framework's Plugin Architecture.

## Overview

The Plugin Architecture enables Firefly application microservices to act as **"containers"** exposing standard Banking as a Service (BaaS) APIs, while the underlying business logic is **pluggable**, **composable**, and **dynamically resolved** at runtime.

### Key Concepts

| Concept | Description |
|---------|-------------|
| **ProcessPlugin** | Core interface for pluggable business logic |
| **@FireflyProcess** | Annotation to mark Spring beans as plugins |
| **ProcessPluginRegistry** | Thread-safe registry of loaded plugins |
| **ProcessPluginExecutor** | Orchestration service for executing plugins |
| **ProcessMappingService** | Resolves operations to processes via config |
| **PluginEventPublisher** | Publishes plugin lifecycle and execution events |
| **PluginMetricsService** | Collects execution metrics via Micrometer |
| **PluginHealthIndicator** | Spring Boot Actuator health endpoint |

## Quick Start: Creating Your First Plugin

### Step 1: Add Dependencies

Your plugin project only needs the `lib-common-application` dependency:

```xml
<dependency>
    <groupId>org.fireflyframework</groupId>
    <artifactId>lib-common-application</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Step 2: Create a Plugin Class

The simplest way is to extend `AbstractProcessPlugin`:

```java
package com.mybank.process.account;

import org.fireflyframework.application.plugin.AbstractProcessPlugin;
import org.fireflyframework.application.plugin.ProcessExecutionContext;
import org.fireflyframework.application.plugin.ValidationResult;
import org.fireflyframework.application.plugin.annotation.FireflyProcess;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@FireflyProcess(
    id = "mybank-account-creation",
    name = "MyBank Account Creation",
    version = "1.0.0",
    description = "Custom account creation with MyBank-specific validations",
    capabilities = {"ACCOUNT_CREATION"},
    requiredPermissions = {"accounts:create"}
)
@Component
public class MyBankAccountCreationProcess 
        extends AbstractProcessPlugin<AccountCreationRequest, AccountResponse> {
    
    private final AccountDomainService accountService;
    private final ComplianceService complianceService;
    
    public MyBankAccountCreationProcess(
            AccountDomainService accountService,
            ComplianceService complianceService) {
        this.accountService = accountService;
        this.complianceService = complianceService;
    }
    
    @Override
    protected Mono<AccountResponse> doExecute(
            ProcessExecutionContext context,
            AccountCreationRequest request) {
        
        // Access context information
        UUID tenantId = context.getTenantId();
        UUID partyId = context.getPartyId();
        
        // Your business logic
        return complianceService.runChecks(partyId, request)
            .flatMap(result -> {
                if (!result.isPassed()) {
                    return businessError("COMPLIANCE_FAILED", result.getMessage());
                }
                return accountService.createAccount(request);
            })
            .map(account -> AccountResponse.from(account));
    }
    
    @Override
    protected ValidationResult doValidate(
            ProcessExecutionContext context,
            AccountCreationRequest request) {
        
        // Input validation
        if (request.getInitialDeposit().compareTo(BigDecimal.ZERO) < 0) {
            return ValidationResult.error("initialDeposit", "Initial deposit cannot be negative");
        }
        
        if (request.getAccountType() == null) {
            return ValidationResult.error("accountType", "Account type is required");
        }
        
        return ValidationResult.valid();
    }
}
```

### Step 3: Define Request/Response DTOs

```java
@Data
@Builder
public class AccountCreationRequest {
    private String accountType;
    private String currency;
    private BigDecimal initialDeposit;
    private UUID customerId;
}

@Data
@Builder
public class AccountResponse {
    private UUID accountId;
    private String accountNumber;
    private String status;
    private BigDecimal balance;
    
    public static AccountResponse from(Account account) {
        return AccountResponse.builder()
            .accountId(account.getId())
            .accountNumber(account.getAccountNumber())
            .status(account.getStatus().name())
            .balance(account.getBalance())
            .build();
    }
}
```

### Step 4: Configure Process Mapping

In the config-mgmt database, create a mapping:

```sql
INSERT INTO api_process_mappings (
    tenant_id, operation_id, process_id, process_version, priority
) VALUES (
    'your-tenant-uuid', 'createAccount', 'mybank-account-creation', '1.0.0', 100
);
```

Or via the config-mgmt API:

```bash
curl -X POST http://config-mgmt:8080/api/v1/api-process-mappings \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "your-tenant-uuid",
    "operationId": "createAccount",
    "processId": "mybank-account-creation",
    "processVersion": "1.0.0",
    "priority": 100
  }'
```

## Plugin Development Patterns

### Pattern 1: Simple Plugin (Implementing Interface Directly)

For maximum flexibility, implement `ProcessPlugin` directly:

```java
@FireflyProcess(id = "simple-process", version = "1.0.0")
@Component
public class SimpleProcess implements ProcessPlugin {
    
    @Override
    public String getProcessId() {
        return "simple-process";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public ProcessMetadata getMetadata() {
        return ProcessMetadata.builder()
            .processId("simple-process")
            .version("1.0.0")
            .name("Simple Process")
            .build();
    }
    
    @Override
    public Mono<ProcessResult> execute(ProcessExecutionContext context) {
        Map<String, Object> input = context.getInputs();
        // Process logic
        return Mono.just(ProcessResult.success(Map.of("result", "done")));
    }
}
```

### Pattern 2: Extended Plugin (Recommended)

Use `AbstractProcessPlugin` for type safety and less boilerplate:

```java
@FireflyProcess(
    id = "typed-process",
    name = "Typed Process",
    version = "1.0.0"
)
@Component
public class TypedProcess 
        extends AbstractProcessPlugin<MyRequest, MyResponse> {
    
    @Override
    protected Mono<MyResponse> doExecute(
            ProcessExecutionContext context,
            MyRequest request) {
        // Type-safe input, automatic result wrapping
        return Mono.just(new MyResponse(request.getValue() * 2));
    }
}
```

### Pattern 3: Vanilla (Default) Process

Mark processes as vanilla to serve as fallbacks:

```java
@FireflyProcess(
    id = "vanilla-transfer",
    name = "Standard Transfer",
    version = "1.0.0",
    vanilla = true,  // This is the default implementation
    capabilities = {"TRANSFER"}
)
@Component
public class VanillaTransferProcess 
        extends AbstractProcessPlugin<TransferRequest, TransferResponse> {
    
    @Override
    protected Mono<TransferResponse> doExecute(
            ProcessExecutionContext context,
            TransferRequest request) {
        // Standard transfer logic
        return transferService.execute(request);
    }
}
```

### Pattern 4: Process with Compensation (Saga Support)

Implement rollback logic for distributed transactions:

```java
@FireflyProcess(
    id = "compensatable-process",
    name = "Compensatable Process",
    version = "1.0.0"
)
@Component
public class CompensatableProcess 
        extends AbstractProcessPlugin<PaymentRequest, PaymentResponse> {
    
    @Override
    protected Mono<PaymentResponse> doExecute(
            ProcessExecutionContext context,
            PaymentRequest request) {
        return paymentService.processPayment(request)
            .doOnSuccess(response -> {
                // Store reference for potential compensation
                context.getProperties().put("paymentId", response.getPaymentId());
            });
    }
    
    @Override
    protected Mono<Void> doCompensate(
            ProcessExecutionContext context,
            PaymentRequest request) {
        // Rollback the payment
        String paymentId = context.getProperty("paymentId", String.class);
        return paymentService.reversePayment(paymentId);
    }
}
```

## Accessing Context Information

The `ProcessExecutionContext` provides everything you need:

```java
@Override
protected Mono<MyResponse> doExecute(
        ProcessExecutionContext context,
        MyRequest request) {
    
    // Identity
    UUID tenantId = context.getTenantId();
    UUID partyId = context.getPartyId();
    UUID contractId = context.getContractId();
    UUID productId = context.getProductId();
    
    // Full application context
    ApplicationExecutionContext appContext = context.getAppContext();
    
    // Security
    boolean hasRole = context.hasRole("ADMIN");
    boolean featureEnabled = context.isFeatureEnabled("new-feature");
    
    // Headers
    String idempotencyKey = context.getHeader("X-Idempotency-Key");
    
    // Correlation for tracing
    String correlationId = context.getCorrelationId();
    
    // Custom properties
    context.getProperties().put("myKey", "myValue");
    
    return Mono.just(new MyResponse());
}
```

## Validation

Implement `doValidate` to validate inputs before execution:

```java
@Override
protected ValidationResult doValidate(
        ProcessExecutionContext context,
        AccountCreationRequest request) {
    
    ValidationResult.ValidationResultBuilder result = ValidationResult.invalid();
    boolean hasErrors = false;
    
    // Field validation
    if (request.getAmount() == null) {
        result.error(ValidationResult.ValidationError.of("amount", "Amount is required"));
        hasErrors = true;
    } else if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
        result.error(ValidationResult.ValidationError.of("amount", "Amount must be positive"));
        hasErrors = true;
    }
    
    if (request.getCurrency() == null || request.getCurrency().length() != 3) {
        result.error(ValidationResult.ValidationError.of("currency", "Valid 3-letter currency code required"));
        hasErrors = true;
    }
    
    // Business rule validation
    if (!context.isFeatureEnabled("international-transfers") 
            && !"USD".equals(request.getCurrency())) {
        result.error(ValidationResult.ValidationError.of(
            "currency", "FEATURE_DISABLED", "International transfers not enabled"));
        hasErrors = true;
    }
    
    return hasErrors ? result.build() : ValidationResult.valid();
}
```

## Error Handling

### Business Errors

Use business errors for expected failures:

```java
@Override
protected Mono<TransferResponse> doExecute(
        ProcessExecutionContext context,
        TransferRequest request) {
    
    return accountService.getBalance(request.getSourceAccount())
        .flatMap(balance -> {
            if (balance.compareTo(request.getAmount()) < 0) {
                // Business error - expected condition
                return businessError("INSUFFICIENT_FUNDS", 
                    "Account has insufficient funds for this transfer");
            }
            return transferService.execute(request);
        });
}
```

### Technical Errors

Technical errors are handled automatically with execution phase tracking:

```java
@Override
protected Mono<ProcessResult> handleError(Throwable error) {
    // Custom error handling
    if (error instanceof TimeoutException) {
        return Mono.just(ProcessResult.failed("TIMEOUT", 
            "Operation timed out, please retry"));
    }
    
    // Fall back to default handling - errors are wrapped in PluginExecutionException
    // with phase information (INITIALIZATION, INPUT_CONVERSION, VALIDATION, 
    // EXECUTION, COMPENSATION, OUTPUT_CONVERSION)
    return super.handleError(error);
}
```

### Execution Phases

The plugin system tracks which phase an error occurred in:

| Phase | Description |
|-------|-------------|
| `INITIALIZATION` | Plugin initialization failed |
| `INPUT_CONVERSION` | Failed to convert input to expected type |
| `VALIDATION` | Input validation failed |
| `EXECUTION` | Business logic execution failed |
| `COMPENSATION` | Compensation/rollback failed |
| `OUTPUT_CONVERSION` | Failed to convert output |

## Health Checks

Plugins can implement health checks for monitoring:

```java
@FireflyProcess(id = "my-process", version = "1.0.0")
@Component
public class MyProcess extends AbstractProcessPlugin<Request, Response> {
    
    private final ExternalService externalService;
    
    @Override
    public Mono<HealthStatus> healthCheck() {
        return externalService.ping()
            .map(ok -> HealthStatus.up()
                .detail("externalService", "connected")
                .build())
            .onErrorReturn(HealthStatus.down()
                .detail("externalService", "disconnected")
                .build());
    }
    
    @Override
    protected Mono<Response> doExecute(ProcessExecutionContext context, Request input) {
        // Business logic
    }
}
```

Health status is exposed via Spring Boot Actuator at `/actuator/health`.

## Observability

### Events

The plugin system publishes Spring events for monitoring:

```java
@Component
public class PluginEventListener {
    
    @EventListener
    public void onPluginRegistered(PluginEvent.PluginRegisteredEvent event) {
        log.info("Plugin registered: {} v{}", 
            event.getProcessId(), event.getProcessVersion());
    }
    
    @EventListener
    public void onExecutionCompleted(PluginEvent.PluginExecutionCompletedEvent event) {
        log.info("Plugin {} executed in {}ms with status {}",
            event.getProcessId(), event.getDurationMs(), event.getStatus());
    }
    
    @EventListener
    public void onExecutionFailed(PluginEvent.PluginExecutionFailedEvent event) {
        log.error("Plugin {} failed: {} - {}",
            event.getProcessId(), event.getErrorCode(), event.getErrorMessage());
    }
}
```

### Metrics

Metrics are automatically collected via Micrometer:

| Metric | Type | Description |
|--------|------|-------------|
| `plugin.executions.total` | Counter | Total executions by process and status |
| `plugin.executions.active` | Gauge | Currently running executions |
| `plugin.execution.duration` | Timer | Execution duration histogram |
| `plugin.errors.total` | Counter | Errors by process and error code |
| `plugin.registry.count` | Gauge | Number of registered plugins |

Configuration:

```yaml
firefly:
  application:
    plugin:
      metrics:
        enabled: true
        detailed-per-process: true  # Separate metrics per process ID
      events:
        enabled: true
        publish-execution-events: true  # Can be disabled for performance
      health:
        enabled: true
        check-individual-plugins: false  # Enable to call each plugin's healthCheck()
        timeout: PT10S
```

## Loading Plugins from External JARs

Plugins can be packaged as external JARs and loaded dynamically.

### Creating a Plugin JAR

1. Create a separate Maven project for your plugin:

```xml
<project>
    <groupId>com.mybank</groupId>
    <artifactId>mybank-account-plugin</artifactId>
    <version>1.0.0</version>
    
    <dependencies>
        <dependency>
            <groupId>org.fireflyframework</groupId>
            <artifactId>lib-common-application</artifactId>
            <version>1.0.0-SNAPSHOT</version>
            <scope>provided</scope> <!-- Provided by container -->
        </dependency>
    </dependencies>
</project>
```

2. Create your plugin class (same as above)

3. Add a plugin descriptor file `META-INF/firefly-plugin.properties`:

```properties
plugin.id=mybank-account-plugin
plugin.version=1.0.0
plugin.class=com.mybank.process.account.MyBankAccountCreationProcess
plugin.description=Custom account creation for MyBank
```

4. Build and deploy:

```bash
mvn clean package
cp target/mybank-account-plugin-1.0.0.jar /opt/firefly/plugins/
```

### Configuring JAR Loading

```yaml
firefly:
  application:
    plugin:
      loaders:
        jar:
          enabled: true
          scan-directories:
            - /opt/firefly/plugins
          hot-reload: true
          hot-reload-interval: PT30S
```

## Loading Plugins from Remote Repositories

### Maven Repository

```yaml
firefly:
  application:
    plugin:
      loaders:
        remote:
          enabled: true
          cache-directory: /var/firefly/plugin-cache
          repositories:
            - type: maven
              name: firefly-plugins
              url: https://repo.mybank.com/plugins
              credentials:
                username: ${MAVEN_USER}
                password: ${MAVEN_PASS}
```

Then reference in config:

```sql
INSERT INTO api_process_mappings (
    tenant_id, operation_id, process_id, 
    loader_type, source_uri, process_version
) VALUES (
    'tenant-uuid', 'createAccount', 'mybank-account-creation',
    'remote-maven', 'com.mybank:mybank-account-plugin', '1.0.0'
);
```

### HTTP Direct Download

```yaml
firefly:
  application:
    plugin:
      loaders:
        remote:
          enabled: true
          repositories:
            - type: http
              name: direct-download
              url: https://plugins.mybank.com
```

## Using the Plugin in Your Service

In your application service, use `ProcessPluginExecutor`:

```java
@Service
@RequiredArgsConstructor
public class AccountApplicationService extends AbstractApplicationService {
    
    private final ProcessPluginExecutor processExecutor;
    
    public Mono<AccountResponse> createAccount(
            ApplicationExecutionContext context,
            AccountCreationRequest request) {
        
        // Plugin system resolves the correct process based on config
        return processExecutor.executeProcess(
                context,
                "createAccount",  // operationId
                toMap(request)    // input
            )
            .map(result -> {
                if (result.isFailed()) {
                    throw new BusinessException(
                        result.getErrorCode(), 
                        result.getErrorMessage()
                    );
                }
                return result.getOutput(AccountResponse.class);
            });
    }
    
    private Map<String, Object> toMap(Object obj) {
        // Convert to Map for plugin input
        return objectMapper.convertValue(obj, Map.class);
    }
}
```

## Testing Plugins

### Unit Testing

```java
@ExtendWith(MockitoExtension.class)
class MyBankAccountCreationProcessTest {
    
    @Mock
    private AccountDomainService accountService;
    
    @Mock
    private ComplianceService complianceService;
    
    @InjectMocks
    private MyBankAccountCreationProcess process;
    
    @Test
    void shouldCreateAccountSuccessfully() {
        // Arrange
        AccountCreationRequest request = AccountCreationRequest.builder()
            .accountType("SAVINGS")
            .currency("USD")
            .initialDeposit(BigDecimal.valueOf(1000))
            .build();
        
        ProcessExecutionContext context = ProcessExecutionContext.builder()
            .operationId("createAccount")
            .inputs(Map.of(
                "accountType", "SAVINGS",
                "currency", "USD",
                "initialDeposit", 1000
            ))
            .build();
        
        when(complianceService.runChecks(any(), any()))
            .thenReturn(Mono.just(ComplianceResult.passed()));
        when(accountService.createAccount(any()))
            .thenReturn(Mono.just(Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("ACC123")
                .status(AccountStatus.ACTIVE)
                .build()));
        
        // Act
        ProcessResult result = process.execute(context).block();
        
        // Assert
        assertThat(result.isSuccess()).isTrue();
        AccountResponse response = result.getOutput(AccountResponse.class);
        assertThat(response.getAccountNumber()).isEqualTo("ACC123");
    }
    
    @Test
    void shouldFailValidationForNegativeDeposit() {
        // Arrange
        ProcessExecutionContext context = ProcessExecutionContext.builder()
            .inputs(Map.of("initialDeposit", -100))
            .build();
        
        // Act
        ValidationResult result = process.validate(context).block();
        
        // Assert
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getField()).isEqualTo("initialDeposit");
    }
}
```

### Integration Testing

```java
@SpringBootTest
@AutoConfigureWebTestClient
class AccountCreationIntegrationTest {
    
    @Autowired
    private WebTestClient webClient;
    
    @Test
    void shouldCreateAccountViaApi() {
        webClient.post()
            .uri("/api/v1/accounts")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                    "accountType": "SAVINGS",
                    "currency": "USD",
                    "initialDeposit": 1000
                }
                """)
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.accountNumber").isNotEmpty()
            .jsonPath("$.status").isEqualTo("ACTIVE");
    }
}
```

## Best Practices

### 1. Implement Health Checks for External Dependencies

```java
@Override
public Mono<HealthStatus> healthCheck() {
    return checkDatabaseConnection()
        .zipWith(checkExternalApi())
        .map(tuple -> {
            if (tuple.getT1() && tuple.getT2()) {
                return HealthStatus.up().build();
            }
            return HealthStatus.degraded()
                .detail("database", tuple.getT1() ? "up" : "down")
                .detail("externalApi", tuple.getT2() ? "up" : "down")
                .build();
        })
        .onErrorReturn(HealthStatus.down().build());
}
```

### 2. Keep Plugins Focused

Each plugin should do one thing well:

```java
// Good: Single responsibility
@FireflyProcess(id = "account-creation", ...)
public class AccountCreationProcess { ... }

@FireflyProcess(id = "account-closure", ...)
public class AccountClosureProcess { ... }

// Avoid: Multiple responsibilities
@FireflyProcess(id = "account-management", ...)
public class AccountManagementProcess { 
    // Creates, updates, deletes - too much
}
```

### 2. Use Semantic Versioning

```java
@FireflyProcess(
    id = "account-creation",
    version = "2.0.0",  // Breaking change
    // version = "1.1.0",  // New feature
    // version = "1.0.1",  // Bug fix
    ...
)
```

### 3. Define Clear Capabilities

```java
@FireflyProcess(
    id = "premium-account-creation",
    capabilities = {
        "ACCOUNT_CREATION",      // What it does
        "PREMIUM_FEATURES",      // What tier
        "INSTANT_ACTIVATION"     // Special capabilities
    },
    ...
)
```

### 4. Require Minimal Permissions

```java
@FireflyProcess(
    requiredPermissions = {
        "accounts:create"     // Just what's needed
    },
    // NOT: "accounts:*"     // Too broad
    ...
)
```

### 5. Handle All Error Cases

```java
@Override
protected Mono<MyResponse> doExecute(
        ProcessExecutionContext context,
        MyRequest request) {
    
    return service.process(request)
        .onErrorMap(NotFoundException.class, 
            e -> new BusinessException("NOT_FOUND", e.getMessage()))
        .onErrorMap(ValidationException.class,
            e -> new BusinessException("INVALID_INPUT", e.getMessage()))
        .timeout(Duration.ofSeconds(30))
        .onErrorMap(TimeoutException.class,
            e -> new BusinessException("TIMEOUT", "Operation timed out"));
}
```

## Architecture Summary

```
┌─────────────────────────────────────────────────────────────────┐
│                    Banking as a Service APIs                    │
│     (Standard REST Endpoints - /api/v1/accounts, etc.)          │
└───────────────────────────────┬─────────────────────────────────┘
                                │
┌───────────────────────────────▼─────────────────────────────────┐
│              EXISTING Controller Layer (UNCHANGED)              │
│   AbstractApplicationController / AbstractResourceController    │
└───────────────────────────────┬─────────────────────────────────┘
                                │
┌───────────────────────────────▼─────────────────────────────────┐
│              ProcessPluginExecutor (Service Layer)              │
│    • Resolves process from config-mgmt                          │
│    • Validates permissions                                      │
│    • Executes plugin                                            │
└───────────────────────────────┬─────────────────────────────────┘
                                │
┌───────────────────────────────▼─────────────────────────────────┐
│                    Process Plugin Layer                         │
│   ┌────────────────┐  ┌────────────────┐  ┌────────────────┐    │
│   │ Vanilla Process│  │ Custom Process │  │ Workflow-Based │    │
│   │ (Default impl) │  │(Tenant-specif.)│  │     Process    │    │
│   └────────────────┘  └────────────────┘  └────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

## Next Steps

- Review the [API Reference](API_REFERENCE.md) for detailed API documentation
- See [Plugin Loading Strategies](PLUGIN_LOADERS.md) for JAR, Maven, and HTTP loading options
- Check the `lib-workflow-engine` module for workflow-backed plugins integration
