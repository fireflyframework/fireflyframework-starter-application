# Application Layer Microservice - Complete Architecture Example

## Table of Contents

1. [Overview](#overview)
2. [Example: Customer Onboarding Service](#example-customer-onboarding-service)
3. [Project Structure](#project-structure)
4. [Module Organization](#module-organization)
5. [Complete Implementation Example](#complete-implementation-example)
6. [Package Structure Deep Dive](#package-structure-deep-dive)
7. [Integration with Domain Services](#integration-with-domain-services)
8. [Security Configuration](#security-configuration)
9. [Testing Strategy](#testing-strategy)
10. [Deployment & Configuration](#deployment--configuration)

---

## Overview

This document provides a **complete, production-ready architecture example** for building an Application Layer microservice using `lib-common-application`.

**Application Layer microservices follow the SAME multi-module structure** as Domain and Platform microservices, ensuring consistency across the entire Firefly architecture.

### Standard Multi-Module Structure

**Application Layer and Domain Layer** microservices use this structure:

- **`-web`**: REST controllers, web configuration, main application class
- **`-core`**: Business logic, service implementations, orchestration
- **`-interfaces`**: DTOs, enums, API contracts
- **`-sdk`**: Client SDK for other services to consume this API

**Platform/Infrastructure Layer** microservices add:
- **`-models`**: Entities, repositories (only for services with persistence)

> ⚠️ **Important**: Application Layer microservices **DO NOT have persistence**, so they **DO NOT need `-models` module**. They orchestrate domain services but don't persist data themselves.

### Key Differences from Domain Layer

| Aspect | Domain Layer | Application Layer |
|--------|--------------|-------------------|
| **Structure** | Multi-module (-web, -core, -interfaces, -models, -sdk) | **Same** Multi-module structure |
| **Purpose** | Implement business logic for ONE domain | Orchestrate MULTIPLE domains |
| **API Exposure** | Optional (internal APIs) | **Required** (external channel APIs) |
| **Context** | Receives simplified AppContext | Manages full ApplicationExecutionContext |
| **Dependencies** | Uses platform SDKs | Uses domain SDKs + platform SDKs |
| **Complexity** | Domain-specific logic | Cross-domain orchestration |

---

## Example: Customer Onboarding Service

We'll use **`customer-application-onboarding`** as our example - a microservice that orchestrates the complete customer onboarding process.

### Business Process

1. **Identity Verification** (KYC)
2. **Document Upload & Validation**
3. **Credit Check** (for certain products)
4. **Contract Creation**
5. **Account Setup**
6. **Notification** (email/SMS)

### Domains Involved

- **customer-domain-people** - Customer identity management
- **contract-domain-agreement** - Contract creation
- **payment-domain-account** - Account creation
- **notification-domain-messaging** - Email/SMS notifications

### Platform Services Used

- **common-platform-customer-mgmt** - Customer master data
- **common-platform-contract-mgmt** - Contract master data
- **common-platform-config-mgmt** - Tenant configuration
- **common-platform-document-mgmt** - Document storage

---

## Project Structure

### Multi-Module Application Layer Microservice

```
customer-application-onboarding/
├── pom.xml                                              # Parent POM
├── README.md
├── customer-application-onboarding-web/                 # WEB MODULE
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── com/
│       │   │       └── firefly/
│       │   │           └── application/
│       │   │               └── onboarding/
│       │   │                   └── web/
│       │   │                       ├── CustomerOnboardingApplication.java  # Main @SpringBootApplication
│       │   │                       └── controllers/
│       │   │                           ├── OnboardingController.java
│       │   │                           └── OnboardingStatusController.java
│       │   └── resources/
│       │       └── application.yaml
│       └── test/
│           └── java/
│               └── com/
│                   └── firefly/
│                       └── application/
│                           └── onboarding/
│                               └── web/
│                                   └── controllers/
│                                       └── OnboardingControllerTest.java
│
├── customer-application-onboarding-core/                # CORE MODULE
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   └── java/
│       │       └── com/
│       │           └── firefly/
│       │               └── application/
│       │                   └── onboarding/
│       │                       └── core/
│       │                           ├── mappers/
│       │                           │   ├── OnboardingMapper.java
│       │                           │   └── KycVerificationMapper.java
│       │                           ├── services/
│       │                           │   ├── OnboardingApplicationService.java
│       │                           │   ├── KycVerificationService.java
│       │                           │   ├── DocumentProcessingService.java
│       │                           │   └── impl/
│       │                           │       ├── OnboardingApplicationServiceImpl.java
│       │                           │       ├── KycVerificationServiceImpl.java
│       │                           │       └── DocumentProcessingServiceImpl.java
│       │                           ├── clients/
│       │                           │   ├── CustomerDomainClient.java
│       │                           │   ├── ContractDomainClient.java
│       │                           │   ├── AccountDomainClient.java
│       │                           │   └── NotificationDomainClient.java
│       │                           ├── resolvers/
│       │                           │   ├── OnboardingContextResolver.java
│       │                           │   └── OnboardingConfigResolver.java
│       │                           ├── security/
│       │                           │   ├── OnboardingSecurityConfiguration.java
│       │                           │   └── OnboardingAuthorizationService.java
│       │                           └── exceptions/
│       │                               ├── OnboardingException.java
│       │                               ├── KycVerificationFailedException.java
│       │                               └── DocumentValidationException.java
│       └── test/
│           └── java/
│               └── com/
│                   └── firefly/
│                       └── application/
│                           └── onboarding/
│                               └── core/
│                                   └── services/
│                                       └── OnboardingApplicationServiceTest.java
│
├── customer-application-onboarding-interfaces/          # INTERFACES MODULE
│   ├── pom.xml
│   └── src/
│       └── main/
│           └── java/
│               └── com/
│                   └── firefly/
│                       └── application/
│                           └── onboarding/
│                               └── interfaces/
│                                   ├── dtos/
│                                   │   ├── StartOnboardingRequestDTO.java
│                                   │   ├── CompleteOnboardingRequestDTO.java
│                                   │   ├── OnboardingResponseDTO.java
│                                   │   ├── OnboardingStatusResponseDTO.java
│                                   │   └── OnboardingStepResponseDTO.java
│                                   └── enums/
│                                       ├── OnboardingStatus.java
│                                       ├── OnboardingStep.java
│                                       └── KycStatus.java
│
└── customer-application-onboarding-sdk/                 # SDK MODULE
    ├── pom.xml
    └── src/
        └── main/
            └── resources/
                └── openapi/
                    └── onboarding-api.yaml             # OpenAPI spec (SDK may have no Java code)
```

---

## Module Organization

### Module Responsibilities

#### 1. **`-web` Module**
**Purpose**: HTTP layer - REST controllers, web configuration, main application

**Contains**:
- `@SpringBootApplication` main class with `@FireflyApplication` annotation
- REST Controllers (`@RestController`) with `@Secure` annotations
- Web configuration (`WebFluxConfigurer`, CORS, etc.)
- OpenAPI/Swagger configuration
- `application.yml` and environment-specific configs
- Controller integration tests

**Dependencies**: `-core`, `-interfaces`, `lib-common-application`

---

#### 2. **`-core` Module**
**Purpose**: Business logic - orchestration, workflows, domain coordination

**Contains**:
- **Application Services**: Extend `AbstractApplicationService`
  - Orchestrate multiple domain services
  - Manage multi-step workflows
  - Handle compensating transactions
- **Domain Clients**: WebClient-based clients to call domain services
- **Context Resolvers**: Implementations of `AbstractContextResolver` and `AbstractConfigResolver`
- **Security Configuration**: `AbstractSecurityConfiguration` implementations
- **Workflow Logic**: Business process orchestration
- **Exception Handling**: Custom application exceptions
- Service unit tests

**Dependencies**: `-interfaces`, domain SDKs, platform SDKs, `lib-common-application`

---

#### 3. **`-interfaces` Module**  
**Purpose**: API contracts - DTOs, enums, shared interfaces

**Contains**:
- **Request DTOs**: Input models for API endpoints
- **Response DTOs**: Output models for API responses
- **Enums**: Status codes, types, categories
- **API Interfaces** (if needed): Shared contracts

**Dependencies**: None (or minimal like `lombok`, `jakarta.validation`)

> ⚠️ **Important**: This module should have NO dependencies on other modules. It's pure data contracts.

---

#### 4. **`-sdk` Module**
**Purpose**: Client SDK for other services to consume this API

**Contains**:
- **API Client**: WebClient-based client (e.g., `OnboardingApiClient`)
- **Client Configuration**: Spring configuration for the client
- **OpenAPI Spec**: `openapi.yaml` for API documentation
- **Usage Examples**: Javadoc with examples

**Dependencies**: `-interfaces`, `spring-webflux`, `reactor-core`

---

### Module Dependency Flow

```
┌─────────────────┐
│   -sdk Module   │  ← Other services use this to call our API
└────────┬────────┘
         │ depends on
         ▼
┌─────────────────┐
│ -interfaces     │  ← Pure data contracts (DTOs, enums)
└────────┬────────┘
         │ used by
         ▼
┌─────────────────┐       ┌──────────────────┐
│  -core Module   │────── │ Domain SDKs      │
│                 │       │ Platform SDKs    │
│  Business Logic │       │ lib-common-app   │
└────────┬────────┘       └──────────────────┘
         │ used by
         ▼
┌─────────────────┐
│  -web Module    │  ← Entry point, REST controllers
│  Main App       │
└─────────────────┘
```

### Why This Structure?

1. **Clear Separation**: HTTP layer separate from business logic
2. **Reusable Contracts**: `-interfaces` can be shared without pulling business logic
3. **SDK Generation**: `-sdk` provides easy consumption for other services
4. **Testability**: Each module can be tested independently
5. **Consistency**: Same pattern across all Firefly microservices

---

## Complete Implementation Example

### 1. Main Application Class

```java
package org.fireflyframework.application.onboarding.web;

import org.fireflyframework.application.annotation.FireflyApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Customer Onboarding Application Service.
 * 
 * <p>Orchestrates the complete customer onboarding process including:
 * - Identity verification (KYC)
 * - Document upload and validation
 * - Credit check (if required)
 * - Contract creation
 * - Account setup
 * - Welcome notifications
 * </p>
 */
@FireflyApplication(
    name = "customer-onboarding",
    displayName = "Customer Onboarding Service",
    description = "Orchestrates complete customer onboarding: KYC, documents, contracts, and accounts",
    domain = "customer",
    team = "customer-experience",
    owners = {"john.doe@getfirefly.io", "jane.smith@getfirefly.io"},
    apiBasePath = "/api/v1/onboarding",
    usesServices = {
        "customer-domain-people",
        "contract-domain-agreement",
        "payment-domain-account",
        "notification-domain-messaging",
        "common-platform-customer-mgmt",
        "common-platform-contract-mgmt",
        "common-platform-document-mgmt",
        "common-platform-config-mgmt"
    },
    capabilities = {
        "Customer Identity Verification",
        "Document Management",
        "Contract Creation",
        "Account Setup",
        "Multi-step Workflow Orchestration"
    }
)
@SpringBootApplication
public class CustomerOnboardingApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(CustomerOnboardingApplication.class, args);
    }
}
```

---

### 2. REST Controller

```java
package org.fireflyframework.application.onboarding.web.controllers;

import org.fireflyframework.application.onboarding.interfaces.dtos.StartOnboardingRequestDTO;
import org.fireflyframework.application.onboarding.interfaces.dtos.CompleteOnboardingRequestDTO;
import org.fireflyframework.application.onboarding.interfaces.dtos.OnboardingResponseDTO;
import org.fireflyframework.application.onboarding.interfaces.dtos.OnboardingStatusResponseDTO;
import org.fireflyframework.application.onboarding.core.services.OnboardingApplicationService;
import org.fireflyframework.application.controller.AbstractContractController;
import org.fireflyframework.application.security.annotation.Secure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import java.util.UUID;

/**
 * REST Controller for customer onboarding operations.
 * 
 * <p>Exposes API endpoints for:
 * - Starting onboarding process
 * - Checking onboarding status
 * - Completing onboarding
 * </p>
 */
@RestController
@RequestMapping("/api/v1/contracts/{contractId}/onboarding")
@RequiredArgsConstructor
@Slf4j
public class OnboardingController extends AbstractContractController {
    
    private final OnboardingApplicationService onboardingService;
    
    /**
     * Start a new customer onboarding process.
     * 
     * @param contractId the contract ID
     * @param request onboarding details
     * @param exchange server web exchange
     * @return onboarding response with session ID
     */
    @PostMapping("/start")
    @ResponseStatus(HttpStatus.CREATED)
    @Secure(roles = "ACCOUNT_APPLICANT", permissions = "START_ONBOARDING")
    public Mono<OnboardingResponseDTO> startOnboarding(
            @PathVariable UUID contractId,
            @Valid @RequestBody StartOnboardingRequestDTO request,
            ServerWebExchange exchange) {
        
        requireContractId(contractId);
        logOperation(contractId, "startOnboarding");
        
        return onboardingService.startOnboarding(exchange, contractId, request);
    }
    
    /**
     * Get onboarding status.
     * 
     * @param contractId the contract ID
     * @param onboardingId the onboarding session ID
     * @param exchange server web exchange
     * @return current onboarding status
     */
    @GetMapping("/{onboardingId}/status")
    @Secure(roles = {"ACCOUNT_APPLICANT", "ACCOUNT_ADMIN"})
    public Mono<OnboardingStatusResponseDTO> getOnboardingStatus(
            @PathVariable UUID contractId,
            @PathVariable UUID onboardingId,
            ServerWebExchange exchange) {
        
        requireContractId(contractId);
        logOperation(contractId, "getOnboardingStatus");
        
        return onboardingService.getOnboardingStatus(exchange, contractId, onboardingId);
    }
    
    /**
     * Complete the onboarding process.
     * 
     * @param contractId the contract ID
     * @param onboardingId the onboarding session ID
     * @param request completion details
     * @param exchange server web exchange
     * @return final onboarding response
     */
    @PostMapping("/{onboardingId}/complete")
    @Secure(roles = "ACCOUNT_APPLICANT", permissions = {"COMPLETE_ONBOARDING", "CREATE_ACCOUNT"})
    public Mono<OnboardingResponseDTO> completeOnboarding(
            @PathVariable UUID contractId,
            @PathVariable UUID onboardingId,
            @Valid @RequestBody CompleteOnboardingRequestDTO request,
            ServerWebExchange exchange) {
        
        requireContractId(contractId);
        logOperation(contractId, "completeOnboarding");
        
        return onboardingService.completeOnboarding(exchange, contractId, onboardingId, request);
    }
}
```

---

### 3. Application Service (Orchestration)

```java
package org.fireflyframework.application.onboarding.core.services;

import org.fireflyframework.application.onboarding.core.clients.*;
import org.fireflyframework.application.onboarding.interfaces.dtos.StartOnboardingRequestDTO;
import org.fireflyframework.application.onboarding.interfaces.dtos.CompleteOnboardingRequestDTO;
import org.fireflyframework.application.onboarding.interfaces.dtos.OnboardingResponseDTO;
import org.fireflyframework.application.onboarding.interfaces.dtos.OnboardingStatusResponseDTO;
import org.fireflyframework.application.service.AbstractApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Main application service for customer onboarding orchestration.
 * 
 * <p>Orchestrates the complete onboarding workflow across multiple domains:
 * 1. Customer identity verification (customer-domain-people)
 * 2. Document validation (common-platform-document-mgmt)
 * 3. Credit check (credit-check-service)
 * 4. Contract creation (contract-domain-agreement)
 * 5. Account setup (payment-domain-account)
 * 6. Notifications (notification-domain-messaging)
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingApplicationService extends AbstractApplicationService {
    
    private final CustomerDomainClient customerClient;
    private final ContractDomainClient contractClient;
    private final AccountDomainClient accountClient;
    private final NotificationDomainClient notificationClient;
    private final KycVerificationService kycService;
    
    /**
     * Start a new onboarding process.
     * 
     * <p>Steps:
     * 1. Resolve execution context
     * 2. Validate user has permission to start onboarding
     * 3. Create customer profile (if not exists)
     * 4. Initiate KYC verification
     * 5. Create onboarding session
     * </p>
     */
    public Mono<OnboardingResponseDTO> startOnboarding(
            ServerWebExchange exchange,
            UUID contractId,
            StartOnboardingRequestDTO request) {
        
        return resolveExecutionContext(exchange)
            .flatMap(context -> validateContext(context, true, false))
            .flatMap(context -> requirePermission(context, "START_ONBOARDING")
                .thenReturn(context))
            .flatMap(context -> {
                log.info("Starting onboarding for contract: {}, party: {}", 
                        contractId, context.getContext().getPartyId());
                
                // Step 1: Create or verify customer profile
                return customerClient.getOrCreateCustomerProfile(context, request.getPersonalInfo())
                    .flatMap(customerProfile -> {
                        
                        // Step 2: Start KYC verification
                        return kycService.startKycVerification(context, customerProfile)
                            .flatMap(kycSession -> {
                                
                                // Step 3: Create onboarding session
                                OnboardingState state = OnboardingState.builder()
                                    .contractId(contractId)
                                    .partyId(context.getContext().getPartyId())
                                    .customerProfileId(customerProfile.getId())
                                    .kycSessionId(kycSession.getId())
                                    .currentStep("KYC_IN_PROGRESS")
                                    .build();
                                
                                // TODO: Save state to database or cache
                                
                                return Mono.just(OnboardingResponseDTO.builder()
                                    .onboardingId(UUID.randomUUID())
                                    .status("KYC_IN_PROGRESS")
                                    .nextStep("DOCUMENT_UPLOAD")
                                    .kycVerificationUrl(kycSession.getVerificationUrl())
                                    .build());
                            });
                    });
            })
            .doOnSuccess(response -> log.info("Onboarding started successfully: {}", response.getOnboardingId()))
            .doOnError(error -> log.error("Failed to start onboarding", error));
    }
    
    /**
     * Get onboarding status.
     */
    public Mono<OnboardingStatusResponseDTO> getOnboardingStatus(
            ServerWebExchange exchange,
            UUID contractId,
            UUID onboardingId) {
        
        return resolveExecutionContext(exchange)
            .flatMap(context -> validateContext(context, true, false))
            .flatMap(context -> {
                log.info("Fetching onboarding status: {}", onboardingId);
                
                // TODO: Fetch onboarding state from database/cache
                
                return Mono.just(OnboardingStatusResponseDTO.builder()
                    .onboardingId(onboardingId)
                    .status("IN_PROGRESS")
                    .completedSteps(List.of("KYC_VERIFICATION", "DOCUMENT_UPLOAD"))
                    .currentStep("CREDIT_CHECK")
                    .progress(60)
                    .build());
            });
    }
    
    /**
     * Complete the onboarding process.
     * 
     * <p>Final steps:
     * 1. Verify all previous steps are complete
     * 2. Create contract
     * 3. Set up account
     * 4. Send welcome notification
     * </p>
     */
    public Mono<OnboardingResponseDTO> completeOnboarding(
            ServerWebExchange exchange,
            UUID contractId,
            UUID onboardingId,
            CompleteOnboardingRequestDTO request) {
        
        return resolveExecutionContext(exchange)
            .flatMap(context -> validateContext(context, true, false))
            .flatMap(context -> requirePermissions(context, "COMPLETE_ONBOARDING", "CREATE_ACCOUNT")
                .thenReturn(context))
            .flatMap(context -> {
                log.info("Completing onboarding: {}", onboardingId);
                
                // Step 1: Create contract
                return contractClient.createContract(context, contractId, request.getContractDetails())
                    .flatMap(contract -> {
                        
                        // Step 2: Create account
                        return accountClient.createAccount(context, contract.getId(), request.getAccountDetails())
                            .flatMap(account -> {
                                
                                // Step 3: Send welcome notification
                                return notificationClient.sendWelcomeEmail(context, account.getId())
                                    .then(Mono.just(OnboardingResponseDTO.builder()
                                        .onboardingId(onboardingId)
                                        .status("COMPLETED")
                                        .contractId(contract.getId())
                                        .accountId(account.getId())
                                        .build()));
                            });
                    });
            })
            .doOnSuccess(response -> log.info("Onboarding completed successfully: {}", onboardingId))
            .doOnError(error -> log.error("Failed to complete onboarding", error));
    }
}
```

---

### 4. Domain Service Client

```java
package org.fireflyframework.application.onboarding.core.clients;

import org.fireflyframework.application.context.ApplicationExecutionContext;
import org.fireflyframework.domain.people.sdk.CustomerProfileDto;
import org.fireflyframework.domain.people.sdk.PersonalInfoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Client for interacting with customer-domain-people microservice.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerDomainClient {
    
    private final WebClient customerDomainWebClient;
    
    /**
     * Get or create customer profile.
     * 
     * @param context execution context
     * @param personalInfo personal information
     * @return customer profile
     */
    public Mono<CustomerProfileDto> getOrCreateCustomerProfile(
            ApplicationExecutionContext context,
            PersonalInfoDto personalInfo) {
        
        return customerDomainWebClient
            .post()
            .uri("/api/v1/customers")
            .header("X-Party-Id", context.getContext().getPartyId().toString())
            .header("X-Tenant-Id", context.getContext().getTenantId().toString())
            .bodyValue(personalInfo)
            .retrieve()
            .bodyToMono(CustomerProfileDto.class)
            .doOnSuccess(profile -> log.info("Customer profile retrieved/created: {}", profile.getId()))
            .doOnError(error -> log.error("Failed to get/create customer profile", error));
    }
}
```

---

### 5. Context Resolver

```java
package org.fireflyframework.application.onboarding.core.resolvers;

import org.fireflyframework.application.resolver.AbstractContextResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Context resolver for onboarding application.
 * 
 * <p>Extracts context information from incoming requests:
 * - partyId from JWT token
 * - contractId from path parameter
 * - tenantId from JWT or subdomain
 * </p>
 */
@Component
@Slf4j
public class OnboardingContextResolver extends AbstractContextResolver {
    
    @Override
    public Mono<UUID> resolvePartyId(ServerWebExchange exchange) {
        // Extract partyId from JWT token
        return extractFromJwtClaim(exchange, "sub")
            .map(UUID::fromString)
            .doOnSuccess(partyId -> log.debug("Resolved partyId: {}", partyId));
    }
    
    @Override
    public Mono<UUID> resolveContractId(ServerWebExchange exchange) {
        // Extract contractId from path parameter
        return extractFromPathVariable(exchange, "contractId")
            .doOnSuccess(contractId -> log.debug("Resolved contractId: {}", contractId));
    }
    
    @Override
    public Mono<UUID> resolveProductId(ServerWebExchange exchange) {
        // Product ID may not be present in onboarding
        return Mono.empty();
    }
    
    @Override
    public Mono<UUID> resolveTenantId(ServerWebExchange exchange) {
        // Extract tenantId from JWT or subdomain
        return extractFromJwtClaim(exchange, "tenantId")
            .map(UUID::fromString)
            .switchIfEmpty(extractFromSubdomain(exchange))
            .doOnSuccess(tenantId -> log.debug("Resolved tenantId: {}", tenantId));
    }
}
```

---

### 6. Security Configuration (Using AbstractSecurityConfiguration)

```java
package org.fireflyframework.application.onboarding.core.security;

import org.fireflyframework.application.security.AbstractSecurityConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Security configuration for onboarding endpoints.
 * 
 * <p>Configures security rules explicitly for dynamic control based on environment.</p>
 */
@Configuration
public class OnboardingSecurityConfiguration extends AbstractSecurityConfiguration {
    
    @Value("${onboarding.security.strict-mode:false}")
    private boolean strictMode;
    
    @Override
    protected void configureEndpointSecurity() {
        
        // Start onboarding - basic permission
        protect("/api/v1/contracts/{contractId}/onboarding/start")
            .onMethod("POST")
            .requireRoles("ACCOUNT_APPLICANT")
            .requirePermissions("START_ONBOARDING")
            .register();
        
        // Get status - allow applicant or admin
        protect("/api/v1/contracts/{contractId}/onboarding/{onboardingId}/status")
            .onMethod("GET")
            .requireRoles("ACCOUNT_APPLICANT", "ACCOUNT_ADMIN")
            .register();
        
        // Complete onboarding - strict mode requires additional approval
        if (strictMode) {
            protect("/api/v1/contracts/{contractId}/onboarding/{onboardingId}/complete")
                .onMethod("POST")
                .requireAllRoles("ACCOUNT_APPLICANT", "APPROVAL_GRANTED")
                .requireAllPermissions("COMPLETE_ONBOARDING", "CREATE_ACCOUNT")
                .register();
        } else {
            protect("/api/v1/contracts/{contractId}/onboarding/{onboardingId}/complete")
                .onMethod("POST")
                .requireRoles("ACCOUNT_APPLICANT")
                .requireAllPermissions("COMPLETE_ONBOARDING", "CREATE_ACCOUNT")
                .register();
        }
    }
}
```

---

## Package Structure Deep Dive

### Module: `-web` 

#### Package: `org.fireflyframework.application.{service}.web.controllers`
**Purpose**: REST API endpoints exposed to channels

**Contains**:
- `@RestController` annotated classes
- **Optional**: Extend `AbstractContractController` or `AbstractProductController`
- **Required**: Use `@Secure` for declarative security
- Pass `ServerWebExchange` to service layer

**Example**:
```java path=null start=null
package org.fireflyframework.application.onboarding.web.controllers;

@RestController
@RequestMapping("/api/v1/contracts/{contractId}/onboarding")
public class OnboardingController extends AbstractContractController {
    
    private final OnboardingApplicationService service;
    
    @PostMapping("/start")
    @Secure(roles = "ACCOUNT_APPLICANT")
    public Mono<OnboardingResponse> startOnboarding(
            @PathVariable UUID contractId,
            @RequestBody StartOnboardingRequest request,
            ServerWebExchange exchange) {
        requireContractId(contractId);
        return service.startOnboarding(exchange, contractId, request);
    }
}
```

---

### Module: `-core`

#### Package: `org.fireflyframework.application.{service}.core.services`
**Purpose**: Business process orchestration

**Contains**:
- Service interfaces and implementations (`services/` and `services/impl/`)
- Application services extending `AbstractApplicationService`
- Orchestrate multiple domain services
- Manage multi-step workflows
- Handle compensating transactions

**Example**:
```java path=null start=null
package org.fireflyframework.application.onboarding.core.services;

@Service
public class OnboardingApplicationService extends AbstractApplicationService {
    
    private final CustomerDomainClient customerClient;
    
    public Mono<OnboardingResponse> startOnboarding(
            ServerWebExchange exchange,
            UUID contractId,
            StartOnboardingRequest request) {
        
        return resolveExecutionContext(exchange)
            .flatMap(context -> validateContext(context, true, false))
            .flatMap(context -> customerClient.createProfile(context, request));
    }
}
```

---

#### Package: `org.fireflyframework.application.{service}.core.clients`
**Purpose**: Clients for calling domain services

**Contains**:
- `WebClient` based clients for HTTP calls
- One client per domain service
- Uses `ApplicationExecutionContext` for passing context

**Example**:
```java path=null start=null
package org.fireflyframework.application.onboarding.core.clients;

@Component
public class CustomerDomainClient {
    
    private final WebClient customerDomainWebClient;
    
    public Mono<CustomerProfile> createProfile(
            ApplicationExecutionContext context,
            StartOnboardingRequest request) {
        
        return customerDomainWebClient
            .post()
            .uri("/api/v1/customers")
            .header("X-Party-Id", context.getContext().getPartyId().toString())
            .bodyValue(request)
            .retrieve()
            .bodyToMono(CustomerProfile.class);
    }
}
```

---

#### Package: `org.fireflyframework.application.{service}.core.resolvers`
**Purpose**: Extract context and configuration from requests

**Contains**:
- Implementations of `AbstractContextResolver` (partyId, contractId, tenantId)
- Implementations of `AbstractConfigResolver` (tenant configuration)

**Example**:
```java path=null start=null
package org.fireflyframework.application.onboarding.core.resolvers;

@Component
public class OnboardingContextResolver extends AbstractContextResolver {
    
    @Override
    public Mono<UUID> resolvePartyId(ServerWebExchange exchange) {
        return extractFromJwtClaim(exchange, "sub").map(UUID::fromString);
    }
    
    @Override
    public Mono<UUID> resolveContractId(ServerWebExchange exchange) {
        return extractFromPathVariable(exchange, "contractId");
    }
}
```

---

#### Package: `org.fireflyframework.application.{service}.core.security`
**Purpose**: Security configuration and authorization

**Contains**:
- Implementations of `AbstractSecurityConfiguration`
- Authorization service implementations

**Example**:
```java
package org.fireflyframework.application.onboarding.core.security;

@Configuration
public class OnboardingSecurityConfiguration extends AbstractSecurityConfiguration {
    
    @Override
    protected void configureEndpointSecurity() {
        protect("/api/v1/contracts/{contractId}/onboarding/start")
            .onMethod("POST")
            .requireRoles("ACCOUNT_APPLICANT")
            .register();
    }
}
```

---

#### Package: `org.fireflyframework.application.{service}.core.exceptions`
**Purpose**: Custom exceptions for the application

**Contains**: Domain-specific exception classes

**Example**:
```java path=null start=null
package org.fireflyframework.application.onboarding.core.exceptions;

public class KycVerificationFailedException extends RuntimeException {
    public KycVerificationFailedException(String message) {
        super(message);
    }
}
```

---

### Module: `-interfaces`

#### Package: `org.fireflyframework.application.{service}.interfaces.dtos`
**Purpose**: Data Transfer Objects

**Structure**:
- `dtos/` - All DTOs (request and response, typically with DTO suffix)

**Example**:
```java path=null start=null
package org.fireflyframework.application.onboarding.interfaces.dtos;

@Data
public class StartOnboardingRequestDTO {
    @NotNull
    private String firstName;
    
    @NotNull
    private String lastName;
    
    @Email
    private String email;
}
```

---

#### Package: `org.fireflyframework.application.{service}.interfaces.enums`
**Purpose**: Enumerations

**Example**:
```java
package org.fireflyframework.application.onboarding.interfaces.enums;

public enum OnboardingStatus {
    PENDING,
    KYC_IN_PROGRESS,
    DOCUMENT_UPLOAD,
    COMPLETED,
    FAILED
}
```

---

### Module: `-sdk`

#### Package: `org.fireflyframework.application.{service}.sdk`
**Purpose**: Client SDK for other services

**Contains**:
- API client class
- Client configuration
- Usage examples in Javadoc

**Example**:
```java
package org.fireflyframework.application.onboarding.sdk;

public class OnboardingApiClient {
    
    private final WebClient webClient;
    
    public Mono<OnboardingResponse> startOnboarding(
            UUID contractId,
            StartOnboardingRequest request) {
        
        return webClient
            .post()
            .uri("/api/v1/contracts/{contractId}/onboarding/start", contractId)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(OnboardingResponse.class);
    }
}
```

---

## Integration with Domain Services

### Pattern for Calling Domain Services

```java
// 1. Resolve execution context
return resolveExecutionContext(exchange)
    
    // 2. Validate context
    .flatMap(context -> validateContext(context, true, false))
    
    // 3. Check permissions
    .flatMap(context -> requirePermission(context, "REQUIRED_PERMISSION")
        .thenReturn(context))
    
    // 4. Call domain service with context
    .flatMap(context -> domainClient.performOperation(context, request))
    
    // 5. Handle response
    .map(result -> mapToResponse(result));
```

### Passing Context to Domain Services

Application services receive `ApplicationExecutionContext`, but domain services typically only need `AppContext`:

```java
// Extract AppContext for domain calls
AppContext domainContext = executionContext.getContext();

// Pass to domain service
return customerDomainService.createCustomer(domainContext, customerData);
```

---

## Security Configuration

### Two Approaches

#### 1. Declarative (Annotations)
```java
@GetMapping
@Secure(roles = "ACCOUNT_VIEWER")
public Mono<List<Account>> getAccounts(...) {
    // Implementation
}
```

#### 2. Explicit (AbstractSecurityConfiguration)
```java
@Configuration
public class SecurityConfig extends AbstractSecurityConfiguration {
    @Override
    protected void configureEndpointSecurity() {
        protect("/api/v1/accounts")
            .onMethod("GET")
            .requireRoles("ACCOUNT_VIEWER")
            .register();
    }
}
```

**Priority**: Explicit configuration ALWAYS overrides annotations.

---

## Testing Strategy

### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class OnboardingApplicationServiceTest {
    
    @Mock
    private CustomerDomainClient customerClient;
    
    @Mock
    private KycVerificationService kycService;
    
    @InjectMocks
    private OnboardingApplicationService service;
    
    @Test
    void shouldStartOnboardingSuccessfully() {
        // Given
        ServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.post("/api/v1/onboarding/start").build()
        );
        
        // When & Then
        StepVerifier.create(service.startOnboarding(exchange, contractId, request))
            .expectNextMatches(response -> 
                response.getStatus().equals("KYC_IN_PROGRESS"))
            .verifyComplete();
    }
}
```

### Integration Tests

```java
@SpringBootTest
@AutoConfigureWebTestClient
class OnboardingIntegrationTest {
    
    @Autowired
    private WebTestClient webClient;
    
    @Test
    @WithMockJwt(partyId = "...", roles = {"ACCOUNT_APPLICANT"})
    void shouldCompleteOnboardingFlow() {
        // Start onboarding
        webClient.post()
            .uri("/api/v1/contracts/{contractId}/onboarding/start", contractId)
            .bodyValue(startRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(OnboardingResponse.class)
            .value(response -> {
                assertThat(response.getOnboardingId()).isNotNull();
            });
    }
}
```

---

## Deployment & Configuration

### application.yml

```yaml
spring:
  application:
    name: customer-application-onboarding

firefly:
  application:
    security:
      enabled: true
      use-security-center: true
    context:
      cache-enabled: true
      cache-ttl: 300

onboarding:
  security:
    strict-mode: false
  kyc:
    provider: jumio
    timeout-seconds: 30
  domain-services:
    customer-domain:
      url: http://customer-domain-people:8080
    contract-domain:
      url: http://contract-domain-agreement:8080
    account-domain:
      url: http://payment-domain-account:8080
```

---

## Summary

### Application Layer Microservices are:

✅ **Multi-module projects** (same structure as Domain and Platform layers)  
✅ **API-first** - Always expose REST/GraphQL APIs to channels  
✅ **Orchestrators** - Coordinate multiple domain services  
✅ **Context managers** - Handle full ApplicationExecutionContext  
✅ **NO persistence** - No `-models` module, delegate data to platform services  

### Module Structure:

```
{service}-application-{name}/
├── {name}-web/          # REST controllers, main app, configs
├── {name}-core/         # Business logic, orchestration, clients
├── {name}-interfaces/   # DTOs, enums, contracts
└── {name}-sdk/          # Client SDK for other services
```

### Key Components from `lib-common-application`:

| Component | Purpose | Module |
|-----------|---------|--------|
| `@FireflyApplication` | Application metadata | `-web` |
| `@Secure` | Declarative security | `-web` controllers |
| `AbstractApplicationService` | Orchestration base class | `-core` services |
| `AbstractContextResolver` | Extract context from requests | `-core` resolvers |
| `AbstractConfigResolver` | Fetch tenant configuration | `-core` resolvers |
| `AbstractSecurityConfiguration` | Explicit security config | `-core` security |
| `AbstractContractController` | Contract-scoped endpoints | `-web` controllers |
| `AbstractProductController` | Product-scoped endpoints | `-web` controllers |
| `EndpointSecurityRegistry` | Runtime security registry | `-core` security |

### Best Practices:

1. **Module Dependencies**: `-web` → `-core` → `-interfaces`
2. **No Circular Dependencies**: `-sdk` only depends on `-interfaces`
3. **Pure Contracts**: `-interfaces` has minimal dependencies (lombok, validation only)
4. **Context Flow**: Always pass `ServerWebExchange` from controllers to services
5. **Security First**: Use `@Secure` on all endpoints or `AbstractSecurityConfiguration`
6. **Path Variables**: Always use `{contractId}` and `{productId}` in paths
7. **Client Pattern**: One WebClient-based client per domain service
8. **Testing**: Test each module independently

---

**Ready to build your Application Layer microservice?**

1. 🎯 Follow this multi-module structure
2. 📖 Start with `-interfaces` (DTOs, enums)
3. 🛠️ Build `-core` (services, clients, resolvers)
4. 🌐 Add `-web` (controllers, main app)
5. 📦 Create `-sdk` for other services to consume your API
6. ✅ Test, document, deploy!

**Consistency is key - all Firefly microservices follow this pattern!**
