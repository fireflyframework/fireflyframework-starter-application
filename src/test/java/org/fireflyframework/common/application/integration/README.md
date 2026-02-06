# Integration Tests

This directory contains integration tests that require Spring Boot context or AOP configuration.

## Test Classes

### 1. ApplicationMetadataProviderIntegrationTest (16 tests)

Tests the `ApplicationMetadataProvider` with full Spring context.

**What it tests:**
- @FireflyApplication annotation scanning from ApplicationContext
- AppMetadata bean creation and registration
- Metadata extraction (name, domain, team, owners, etc.)
- Version resolution from Spring properties
- Build info extraction (git.commit, git.branch, etc.)
- Environment resolution from active profiles
- Startup time capture
- Display name fallback logic

**Requirements:**
- `@SpringBootTest` - Full Spring Boot context
- `@TestPropertySource` - Property injection
- ApplicationContext with @FireflyApplication annotated class

**Key Scenarios:**
```java
@FireflyApplication(
    name = "test-service",
    domain = "test",
    team = "test-team"
)
@SpringBootConfiguration
class TestConfig { }
```

---

### 2. SecurityAspectIntegrationTest (6 tests)

Tests the `SecurityAspect` AOP interception of @Secure annotations.

**What it tests:**
- AOP interception of @Secure annotated methods
- Authorization granted scenarios
- Authorization denied with AccessDeniedException
- Roles extraction from annotation attributes
- Permissions extraction from annotation attributes
- Execution without ExecutionContext (security skip)

**Requirements:**
- `AspectJProxyFactory` - Manual AOP proxy creation
- Mockito for SecurityAuthorizationService
- @Secure annotated test methods

**Key Scenarios:**
```java
@Secure(roles = {"ADMIN"}, permissions = {"WRITE"})
public String secureMethod(ApplicationExecutionContext context) {
    return "success";
}
```

---

## Running Integration Tests

### All Integration Tests
```bash
mvn test -Dtest='*Integration*'
```

### Specific Test
```bash
mvn test -Dtest=ApplicationMetadataProviderIntegrationTest
mvn test -Dtest=SecurityAspectIntegrationTest
```

### With Debug Output
```bash
mvn test -Dtest=ApplicationMetadataProviderIntegrationTest -X
```

---

## Test Statistics

| Test Class | Tests | Type | Duration |
|------------|-------|------|----------|
| ApplicationMetadataProviderIntegrationTest | 16 | Spring Context | ~430ms |
| SecurityAspectIntegrationTest | 6 | AOP | ~213ms |
| **TOTAL** | **22** | | **~643ms** |

---

## Why Integration Tests?

These components cannot be tested with simple unit tests because:

1. **ApplicationMetadataProvider**
   - Needs to scan Spring ApplicationContext for annotations
   - Requires Spring Environment for property resolution
   - Must be tested with actual Spring Boot lifecycle

2. **SecurityAspect**
   - Requires AspectJ AOP weaving/proxying
   - Must intercept method calls at runtime
   - Needs to validate annotation attribute extraction

---

## Test Patterns

### Spring Context Integration
```java
@SpringBootTest(classes = TestConfig.class)
@TestPropertySource(properties = {
    "spring.application.version=1.0.0-TEST"
})
class MyIntegrationTest {
    
    @Autowired
    private AppMetadata appMetadata;
    
    @Test
    void shouldLoadMetadata() {
        assertThat(appMetadata).isNotNull();
    }
}
```

### AOP Proxy Testing
```java
@BeforeEach
void setUp() {
    SecurityAspect aspect = new SecurityAspect(authService, registry);
    TestService service = new TestService();
    
    AspectJProxyFactory factory = new AspectJProxyFactory(service);
    factory.addAspect(aspect);
    
    proxiedService = factory.getProxy();
}
```

---

## Maintenance Notes

- Integration tests take longer (~640ms vs <30ms for unit tests)
- They require more setup (Spring context, AOP proxies)
- They test end-to-end functionality
- They validate framework integration points
- Run separately in CI/CD if needed for faster feedback

---

## Related Documentation

- Main testing guide: `docs/TESTING.md`
- Unit tests: `src/test/java/README.md`
- Coverage analysis: `COVERAGE_ANALYSIS.md`

---

**Last Updated**: January 2025  
**Test Count**: 22 integration tests  
**Status**: ✅ All Passing
