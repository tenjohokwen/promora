# Testing

**Analysis Date:** 2026-01-17

## Framework

**Primary:** JUnit 5 (Jupiter) via `spring-boot-starter-test`

**Supporting Libraries:**
- Mockito - Mocking framework (`mockito-core`)
- AssertJ 3.24.2 - Fluent assertions (`assertj-core`)
- Testcontainers - Integration testing with real PostgreSQL
- Awaitility 4.2.0 - Async testing utilities
- Instancio 2.10.0 - Test data generation
- JSON Unit 4.1.0 - JSON assertion utilities (`json-unit-assertj`)

## Test Structure

```
src/test/java/com/softropic/promora/
├── common/
│   ├── configtest/         # Configuration tests
│   │   └── JacksonTest.java
│   ├── validation/         # Validator tests
│   │   ├── CamMobileValidatorTest.java
│   │   └── InputValidatorTest.java
│   ├── HttpTestClient.java
│   ├── TestClockProvider.java
│   └── TransactionExceptionSimulator.java
├── config/
│   ├── ApplicationNoSecurity.java
│   ├── CustomPostgresContainer.java
│   └── TestConfig.java
├── security/
│   ├── api/util/
│   │   └── InputValidatorTest.java
│   ├── jwt/api/
│   │   ├── JwtManagerImplTest.java
│   │   └── filter/
│   │       ├── JWTAuthenticationFilterTest.java
│   │       └── JWTAuthorizationFilterTest.java
│   ├── manager/
│   │   └── LoginAttemptsServiceTest.java
│   ├── repo/
│   │   └── UserRepositoryIT.java
│   ├── secret/
│   │   └── SecretServiceIT.java
│   ├── service/
│   │   └── UserServiceIT.java
│   └── SecurityIT.java
├── utils/
│   └── sql/                # SQL testing utilities
│       ├── SqlQuery.java
│       ├── Statement.java
│       ├── SqlStatementHolder.java
│       ├── QueryRecorderListener.java
│       ├── EntityFetchAsserter.java
│       └── matcher/
│           ├── AtLeast.java
│           ├── CountStrategy.java
│           ├── CountStrategyFactory.java
│           └── Times.java
├── PromoraApplicationTests.java
├── TestPromoraApplication.java
└── TestcontainersConfiguration.java
```

## Test Categories

**Unit Tests (`*Test.java`):**
- Location: Same package as source under `src/test/java/`
- Naming: `{ClassName}Test.java`
- Characteristics: Fast, isolated, mock dependencies
- Examples:
  - `JwtManagerImplTest.java` - JWT token logic
  - `CamMobileValidatorTest.java` - Phone validation
  - `LoginAttemptsServiceTest.java` - Login attempts logic

**Integration Tests (`*IT.java`):**
- Location: Same package as source under `src/test/java/`
- Naming: `{ClassName}IT.java`
- Characteristics: Use Testcontainers, test real database interactions
- Examples:
  - `SecurityIT.java` - Full security flow
  - `UserServiceIT.java` - User service with database
  - `UserRepositoryIT.java` - Repository queries
  - `SecretServiceIT.java` - Secret management

## Mocking

**Framework:** Mockito (via spring-boot-starter-test)

**Patterns:**
```java
// Mock creation
@Mock
private SecretService secretService;

// Stubbing
when(secretService.fetchSecret(anyString(), anyString()))
    .thenReturn(mockSecret);

// Verification
verify(secretService, times(1)).fetchSecret("busId", "version");
```

**MockMvc for Controllers:**
```java
@Autowired
private MockMvc mockMvc;

mockMvc.perform(post("/v1/account/register")
    .contentType(MediaType.APPLICATION_JSON)
    .content(objectMapper.writeValueAsString(userDto)))
    .andExpect(status().isOk());
```

## Test Utilities

**TestClockProvider:**
- Location: `src/test/java/com/softropic/promora/common/TestClockProvider.java`
- Purpose: Control time in tests for time-dependent logic

**TestRequestMetadataProvider:**
- Location: `src/test/java/com/softropic/promora.security/exposed/util/TestRequestMetadataProvider.java`
- Purpose: Mock request metadata (client IP, user agent, etc.)

**HttpTestClient:**
- Location: `src/test/java/com/softropic/promora.common/HttpTestClient.java`
- Purpose: HTTP client utilities for integration tests

**SQL Assertion Utilities:**
- Location: `src/test/java/com/softropic/promora/utils/sql/`
- Purpose: Assert on SQL queries executed (N+1 detection, query counting)
- Components:
  - `QueryRecorderListener` - Records executed queries
  - `EntityFetchAsserter` - Asserts fetch counts
  - `SqlStatementHolder` - Holds statement statistics

**DbCleaner:**
- Location: `src/test/java/com/softropic/promora/utils/DbCleaner.java`
- Purpose: Clean database between tests

**TestMailManager:**
- Location: `src/test/java/com/softropic/promora.utils/TestMailManager.java`
- Purpose: Mock email sending in tests

## Testcontainers Setup

**Configuration:**
- Location: `src/test/java/com/softropic/promora/TestcontainersConfiguration.java`
- Custom container: `src/test/java/com/softropic/promora.config/CustomPostgresContainer.java`

**Usage Pattern:**
```java
@Testcontainers
@SpringBootTest
class UserServiceIT {
    @Container
    static PostgreSQLContainer<?> postgres = new CustomPostgresContainer();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

## Coverage

**Current State:**
- 39 test files identified
- Mix of unit and integration tests
- Focus areas: Security, JWT, validation

**Notable Gaps:**
- No frontend tests (Vue.js components)
- Registration flow not fully tested
- Password reset flow not tested
- Most REST endpoints lack dedicated tests
- Email service integration not tested

**Recommended Improvements:**
1. Add controller integration tests for all endpoints
2. Add frontend unit tests with Vitest
3. Add E2E tests with Cypress or Playwright
4. Increase coverage of business logic in facades

## Running Tests

**All Tests:**
```bash
./mvnw test
```

**Integration Tests Only:**
```bash
./mvnw verify -DskipUnitTests
```

**Single Test Class:**
```bash
./mvnw test -Dtest=JwtManagerImplTest
```

**With Coverage Report:**
```bash
./mvnw test jacoco:report
```

## Test Configuration

**Test Application Properties:**
- Testcontainers provides dynamic database configuration
- `ApplicationNoSecurity.java` - Test configuration without security
- `TestConfig.java` - Additional test beans

**Test Spring Boot Application:**
- `TestPromoraApplication.java` - Dedicated test application class
- Uses `@TestConfiguration` for test-specific beans

---

*Testing analysis: 2026-01-17*
