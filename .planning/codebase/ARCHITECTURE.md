# Architecture

**Analysis Date:** 2026-01-17

## Pattern Overview

**Overall:** Layered Monolithic Architecture with Modular Domain Organization

**Key Characteristics:**
- Spring Boot 3.5 monolithic backend with clear layer separation (API, Service, Repository, Domain)
- Domain-driven module organization within packages (security, email, common)
- Vue.js/Quasar SPA frontend bundled into Spring Boot static resources during Maven build
- JWT-based stateless authentication with two-factor authentication (OTP) support
- Event-driven communication using Spring Application Events for cross-cutting concerns

## Layers

**API/Controller Layer:**
- Purpose: HTTP request handling, input validation, response formatting
- Location: `src/main/java/com/softropic/promora/*/api/`
- Contains: REST controllers (`*Resource.java`), DTOs, request/response mappers
- Depends on: Service layer, MapStruct mappers
- Used by: HTTP clients (frontend, external consumers)

**Service Layer:**
- Purpose: Business logic orchestration, transaction management, security enforcement
- Location: `src/main/java/com/softropic/promora/*/service/`
- Contains: Service classes with `@Service` and `@Transactional` annotations
- Depends on: Repository layer, Domain entities, other services
- Used by: API layer, Event listeners

**Facade Layer:**
- Purpose: Coordination of multiple services for complex operations
- Location: `src/main/java/com/softropic/promora/security/api/AccountManagementFacade.java`
- Contains: High-level business operations spanning multiple services
- Depends on: Multiple services (UserService, UserRegistrationService, PasswordResetService, etc.)
- Used by: API controllers for complex multi-step operations

**Repository Layer:**
- Purpose: Data persistence and query operations
- Location: `src/main/java/com/softropic/promora.*/repository/`
- Contains: Spring Data JPA repositories extending `JpaRepository`
- Depends on: Domain entities, JPA/Hibernate
- Used by: Service layer

**Domain Layer:**
- Purpose: Core business entities and domain logic
- Location: `src/main/java/com/softropic/promora.*/domain/`
- Contains: JPA entities with business methods embedded (Rich Domain Model approach)
- Depends on: Common persistence abstractions (BaseEntity, AbstractAuditingEntity)
- Used by: All layers

**Common/Shared Layer:**
- Purpose: Cross-cutting concerns, utilities, shared abstractions
- Location: `src/main/java/com/softropic/promora.common/`
- Contains: Base entities, exceptions, validation, utilities, DTOs, thread pool configs
- Depends on: External libraries only
- Used by: All domain modules

**Frontend Layer:**
- Purpose: User interface and client-side logic
- Location: `src/frontend/src/`
- Contains: Vue.js 3 components, Quasar UI, Pinia stores, Vue Router
- Depends on: Backend REST API
- Used by: End users via browser

## Data Flow

**User Registration Flow:**

1. Client sends POST to /v1/account/register with UserDto
2. AccountResource.registerAccount() receives validated request
3. AccountManagementFacade.registerAccount() orchestrates:
   - Checks existing user via UserRegistrationService.findUserByEmailOrLogin()
   - Selects notification strategy (Email or SMS) based on login type
   - Creates user via UserRegistrationService.createUser() if new
   - Publishes Envelope event for email notification
4. SendMailListener receives event and processes email via MailManager
5. Response with tracking code returned to client

**Authentication Flow:**

1. Client POSTs credentials to /authenticate
2. JWTAuthenticationFilter.attemptAuthentication() extracts credentials
3. FraudAwareAuthenticationManager wraps AuthenticationManager with fraud checks
4. DaoAuthProvider validates credentials against database via LoadUserByUserNameService
5. On success with OTP enabled:
   - TwoFactorLoginManager.processLogin() generates OTP and stores login info
   - OTP sent via email
   - Client receives loginInfoId for second factor submission
6. On success without OTP or after OTP verification:
   - LoginTokenManager.createLoginToken() generates JWT cookie
   - JWT stored in HttpOnly cookie

**State Management:**
- Backend: Stateless JWT authentication; database as source of truth
- Frontend: Pinia stores for client-side state (see src/frontend/src/stores/)
- Session tracking via sessionId field in AbstractAuditingEntity for audit purposes

## Key Abstractions

**BaseEntity:**
- Purpose: Common ID generation and equality for all entities
- Location: `src/main/java/com/softropic/promora.common/persistence/BaseEntity.java`
- Pattern: Uses TSID (Time-Sorted Identifier) via @Tsid annotation from Hypersistence Utils

**AbstractAuditingEntity:**
- Purpose: Automatic audit fields (createdBy, createdDate, lastModifiedBy, lastModifiedDate, requestId, sessionId, status)
- Location: `src/main/java/com/softropic/promora.common/persistence/AbstractAuditingEntity.java`
- Pattern: Spring Data JPA Auditing with custom entity listeners

**Consumer Interface:**
- Purpose: Common interface for customer-like entities
- Location: `src/main/java/com/softropic/promora.common/consumer/Consumer.java`
- Pattern: Interface segregation for different user types

**Response Interface:**
- Purpose: Standard API response format with helpCode for tracking
- Location: `src/main/java/com/softropic/promora.common/message/Response.java` (interface), Success.java, Failure.java
- Pattern: Sealed interface pattern for type-safe responses

**RegistrationNotificationStrategy:**
- Purpose: Channel-specific notification logic for user registration
- Location: `src/main/java/com/softropic/promora.security/api/registration/EmailRegistrationStrategy.java`, SmsRegistrationStrategy.java
- Pattern: Strategy pattern for notification channel selection

## Entry Points

**Spring Boot Application:**
- Location: `src/main/java/com/softropic/promora/PromoraApplication.java`
- Triggers: JVM startup via java -jar or Maven
- Responsibilities: Bootstrap Spring context, component scanning, auto-configuration

**REST API Endpoints:**
- Location: Controllers in `src/main/java/com/softropic/promora.*/api/*.java`
- Triggers: HTTP requests
- Responsibilities: Request validation, business operation invocation, response formatting

**Security Filter Chain:**
- Location: `src/main/java/com/softropic/promora.security/config/SecurityConfiguration.java`
- Triggers: Every HTTP request
- Responsibilities: Authentication, authorization, security headers, CORS

**Authentication Filter:**
- Location: `src/main/java/com/softropic/promora.security/jwt/api/filter/JWTAuthenticationFilter.java`
- Triggers: POST to /authenticate
- Responsibilities: Credential extraction, authentication attempt, JWT/OTP generation

**Frontend Application:**
- Location: `src/frontend/src/App.vue`
- Triggers: Browser navigation to application URL
- Responsibilities: Vue.js app initialization, routing, UI rendering

## Error Handling

**Strategy:** Centralized exception handling with structured error responses

**Patterns:**
- Custom domain exceptions extending ApplicationException in `src/main/java/com/softropic/promora.common/exception/`
- Security-specific exceptions in `src/main/java/com/softropic/promora.security/exception/`
- HandlerExceptionResolver injection for filter-level exception handling
- Response interface with Success and Failure implementations for consistent API responses
- ErrorCode enums for categorized error identification

## Cross-Cutting Concerns

**Logging:**
- Logback with Logstash encoder (net.logstash.logback:logstash-logback-encoder)
- Structured JSON logging capability
- Access logging via custom LoggingFilter
- Log keys defined in `src/main/java/com/softropic/promora.common/logging/LogKeys.java`

**Validation:**
- Jakarta Bean Validation (@Valid, @NotNull, @Email, etc.)
- Custom validators in `src/main/java/com/softropic/promora.common/validation/`
- Phone number validation with @Phone annotation
- ISO language code validation with @LangIso2

**Authentication:**
- JWT-based with JJWT library
- Two-factor authentication (OTP via email)
- BCrypt password encoding
- Spring Security with custom filters:
  - JWTAuthenticationFilter - handles /authenticate
  - JWTAuthorizationFilter - validates JWT on secured endpoints
  - SecondFactorLoginFilter - OTP verification
  - SecurityAdviceFilter - request metadata enrichment
  - SessionRefreshFilter - token refresh handling

**Auditing:**
- Spring Data JPA Auditing for entity timestamps and user tracking
- Hibernate Envers for entity revision history (@Audited)
- Security audit trail via AuditLog entity and TrailService
- Request ID tracking via RequestIdAuditEntityListener
- Session ID tracking via SessionIdAuditEntityListener

**Caching:**
- Spring Boot Cache starter included
- Hikari connection pool for database connections

**Metrics:**
- Micrometer with Prometheus registry
- Actuator endpoints exposed at /manage/**

---

*Architecture analysis: 2026-01-17*
