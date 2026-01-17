# Coding Conventions

**Analysis Date:** 2026-01-17

## Naming Patterns

**Files (Java):**
- Classes: PascalCase (e.g., `SecretService.java`, `AccountResource.java`)
- Test classes: `{ClassName}Test.java` for unit tests, `{ClassName}IT.java` for integration tests
- Mappers: `{Entity}Mapper.java` (e.g., `UserMapper.java`)
- Repositories: `{Entity}Repository.java` (e.g., `SecretRepository.java`)
- Configuration: `{Feature}Configuration.java` or `{Feature}Config.java`

**Files (Vue/Frontend):**
- Components: PascalCase (e.g., `EssentialLink.vue`, `MainLayout.vue`)
- Pages: PascalCase with `Page` suffix (e.g., `IndexPage.vue`, `ErrorNotFound.vue`)
- Boot files: lowercase (e.g., `axios.js`)
- Stores: lowercase (e.g., `index.js`)

**Functions/Methods (Java):**
- camelCase for method names
- Verb-first naming: `fetchSecret`, `createActiveSecret`, `validateBusId`
- Boolean methods: `isTokenFixed`, `hasDbRefreshTokenExpired`, `activateSecurity`

**Functions (JavaScript/Vue):**
- camelCase for function names (e.g., `toggleLeftDrawer`)
- Use `const` for function references with arrow functions or `function` keyword

**Variables:**
- Java: camelCase for variables, UPPER_SNAKE_CASE for constants
- JavaScript: camelCase for variables

**Types/Interfaces:**
- Java: PascalCase for classes, interfaces, enums
- Error codes: Enum classes with UPPER_SNAKE_CASE values (e.g., `SecError.KEY_NOT_FOUND`)

## Code Style

**Formatting:**
- Java: Standard Java formatting (IDE default, likely IntelliJ based on `.idea` directory)
- Frontend: Prettier with ESLint
  - Config: `src/frontend/eslint.config.js`
  - Uses `@vue/eslint-config-prettier/skip-formatting`

**Linting:**
- Frontend: ESLint 9.x with flat config
  - Plugin: `eslint-plugin-vue` with `flat/essential` rules
  - Quasar plugin: `@quasar/app-vite/eslint` recommended config
  - Run: `npm run lint` in `src/frontend/`

**Java Code Style Guidelines:**
- Use constructor injection for Spring beans (preferred) or `@Autowired` field injection
- Static imports for constants and assertion methods
- Private static final for constants within classes

## Import Organization

**Java Import Order:**
1. `com.softropic.promora.*` (project packages)
2. Third-party libraries (`org.springframework.*`, `org.apache.*`, etc.)
3. `java.*` / `javax.*` / `jakarta.*` packages
4. Static imports at bottom

**Example from `src/main/java/com/softropic/promora/security/api/AccountResource.java`:**
```java
package com.softropic.promora.security.api;

import com.softropic.promora.common.message.Failure;
import com.softropic.promora.common.message.Response;
import com.softropic.promora.common.message.Success;
// ... more project imports

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
// ... more third-party imports

import java.util.Map;
import java.util.Optional;

import io.micrometer.core.annotation.Timed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
```

**Vue Import Order:**
1. Vue core (`vue`, `vue-router`)
2. Component imports from relative paths

**Path Aliases (Frontend):**
- `components/` maps to `src/components/`

## Error Handling

**Patterns (Java):**
- Custom exceptions extend `ApplicationException` base class
- All exceptions include:
  - Support ID (auto-generated unique identifier for user support)
  - Error code enum for categorization
  - Log context map for structured logging
- Throw domain-specific exceptions: `SecException`, `AuthorizationException`, `InvalidJWTDataException`

**Exception Structure (from `src/main/java/com/softropic/promora/common/exception/ApplicationException.java`):**
```java
public class ApplicationException extends RuntimeException {
    protected ErrorCode errorCode;
    protected final Map<String, Object> logContext = new HashMap<>();
    private final String supportId;  // Auto-generated SQID

    public String getMessage() {
        return String.format("%s: %s, ERROR_CODE: %s, MESSAGE: %s",
            SUPPORT_ID, supportId, errorCode, super.getMessage());
    }
}
```

**Error Code Pattern:**
- Create enum implementing `ErrorCode` interface per module
- Examples: `SecError`, `SecurityError`

**Error Response Pattern:**
```java
throw new SecException("Cannot find requested secret",
    Map.of("busId", busId, "version", version),
    SecError.KEY_NOT_FOUND);
```

## Logging

**Framework:** SLF4J with Logback

**Patterns:**
- Private logger per class: `private final Logger log = LoggerFactory.getLogger(ClassName.class);`
- Use structured logging with logstash-logback-encoder for JSON output
- Log context included via exception `logContext` map

**When to Log:**
- Debug level for request tracing
- Info level for significant business operations
- Error level for exceptions (typically handled by exception handlers)

## Comments

**When to Comment:**
- Javadoc on public API methods (REST endpoints)
- TODO comments for known technical debt
- Explain non-obvious business logic

**Javadoc Pattern:**
```java
/**
 * POST  /register to register the user.
 * @param userDTO holds the user's data
 * @return ResponseEntity
 */
@PostMapping(value="/register", produces = MediaType.APPLICATION_JSON_VALUE)
@Timed
public Success registerAccount(@Valid @RequestBody final UserDto userDTO) {
```

**TODO Format:**
- `//TODO description` - inline todos

## Function Design

**Size:** Methods should be focused and single-purpose

**Parameters:**
- Use `final` keyword for method parameters
- Validate early with domain-specific exceptions
- Use `@Valid` for DTO validation in controllers

**Return Values:**
- Use `Optional<T>` for nullable returns from services
- REST endpoints return `ResponseEntity<T>`, `Success`, or `Failure` response objects
- Avoid returning `null` directly

## Module Design

**Exports:**
- Use package-private visibility by default
- Public interfaces for exposed API
- Separate `exposed` subpackages for cross-module interfaces

**Package Structure:**
```
com.softropic.promora.{module}/
  ├── api/           # REST controllers
  ├── config/        # Configuration classes
  ├── domain/        # Domain entities
  ├── exception/     # Module-specific exceptions
  ├── exposed/       # Public interfaces for other modules
  ├── listener/      # Event listeners
  ├── manager/       # Business logic managers
  ├── repository/    # Data repositories
  └── service/       # Service layer
```

**Barrel Files:** Not applicable to Java; use explicit imports

## Spring-Specific Conventions

**Dependency Injection:**
- Constructor injection for required dependencies (preferred)
- Field injection with `@Autowired` also used
- `@Qualifier` for disambiguating beans

**Service Layer:**
- `@Service` annotation
- `@Transactional` at class level for data services
- Stateless services preferred; use ConcurrentHashMap for caching

**REST Controllers:**
- `@RestController` with `@RequestMapping` base path
- `@Timed` annotation for metrics
- `@Valid` for request body validation
- Version prefix in path: `/v1/{resource}`

**Entity Pattern:**
- Extend `AbstractAuditingEntity` for audit fields
- Use `@Audited` (Hibernate Envers) for entity history
- Unique constraints via `@Table(uniqueConstraints = ...)`

## Vue/Frontend Conventions

**Component Structure:**
- Use `<script setup>` syntax (Composition API)
- Props defined with `defineProps()` with type definitions
- Template first, then script (Vue 3 SFC order)

**Props Pattern:**
```javascript
const props = defineProps({
  title: {
    type: String,
    required: true,
  },
  caption: {
    type: String,
    default: '',
  },
})
```

**Reactivity:**
- Use `ref()` for reactive state
- Define event handlers as regular functions

---

*Convention analysis: 2026-01-17*
