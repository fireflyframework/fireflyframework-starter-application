# Firefly Framework - Starter Application

[![CI](https://github.com/fireflyframework/fireflyframework-starter-application/actions/workflows/ci.yml/badge.svg)](https://github.com/fireflyframework/fireflyframework-starter-application/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://openjdk.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)

> Opinionated starter for application-layer microservices providing business process orchestration, context management, security, authorization, and session management.

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [Documentation](#documentation)
- [Contributing](#contributing)
- [License](#license)

## Overview

Firefly Framework Starter Application is an **opinionated starter for application-layer microservices** that orchestrate business processes across domain services. It provides a complete foundation for building application-tier microservices with enterprise-grade context management, security authorization, and session handling.

This starter supplies context management (application config, security context, session context) and security authorization with annotation-driven endpoint protection. It's designed for the **application tier** in a multi-tier architecture — the orchestration layer that coordinates domain services and implements business workflows.

The starter also includes abstract controllers for standardized REST endpoints, configuration caching, domain passthrough for cross-service data propagation, and SPI-based session management.

## Features

- `AppContext` and `AppSecurityContext` for application-wide context management
- `@Secure` and `@RequireContext` annotations for declarative endpoint security
- `SecurityAuthorizationService` with configurable endpoint security registry
- Abstract controllers: `AbstractApplicationController`, `AbstractResourceController`
- Configuration caching auto-configuration
- Domain passthrough for cross-service data propagation
- Session management SPI with `SessionManager` and `SessionContext`
- Application metadata with `@FireflyApplication` annotation
- Health indicators for application layer
- Actuator info contributor for Firefly application metadata
- Orchestration engine support (Saga, TCC, Workflow) via optional dependency

## Requirements

- Java 21+
- Spring Boot 3.x
- Maven 3.9+

## Installation

```xml
<dependency>
    <groupId>org.fireflyframework</groupId>
    <artifactId>fireflyframework-starter-application</artifactId>
    <version>26.02.07</version>
</dependency>
```

## Quick Start

```java
import org.fireflyframework.common.application.controller.AbstractApplicationController;
import org.fireflyframework.common.application.security.annotation.Secure;

@RestController
@RequestMapping("/api/accounts")
public class AccountController extends AbstractApplicationController {

    @Secure(roles = {"ADMIN", "ACCOUNT_MANAGER"})
    @PostMapping
    public Mono<AccountResponse> create(@RequestBody AccountRequest request) {
        return accountService.create(request);
    }
}
```

## Configuration

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

## Documentation

Additional documentation is available in the [docs/](docs/) directory:

- [Architecture](docs/ARCHITECTURE.md)
- [Usage Guide](docs/USAGE_GUIDE.md)
- [Security Guide](docs/SECURITY_GUIDE.md)
- [Security Center Integration](docs/SECURITY_CENTER_INTEGRATION.md)
- [Api Reference](docs/API_REFERENCE.md)
- [Cache Architecture](docs/CACHE_ARCHITECTURE.md)
- [Example Microservice Architecture](docs/EXAMPLE_MICROSERVICE_ARCHITECTURE.md)
- [Testing](docs/TESTING.md)

## Contributing

Contributions are welcome. Please read the [CONTRIBUTING.md](CONTRIBUTING.md) guide for details on our code of conduct, development process, and how to submit pull requests.

## License

Copyright 2024-2026 Firefly Software Solutions Inc.

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.
