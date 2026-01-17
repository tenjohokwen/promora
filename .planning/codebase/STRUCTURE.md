# Directory Structure

**Analysis Date:** 2026-01-17

## Root Layout

```
promora/
├── .planning/              # GSD planning documents (this folder)
├── src/
│   ├── main/
│   │   ├── java/           # Backend Java source code
│   │   └── resources/      # Configuration, templates, migrations
│   ├── test/               # Backend test code
│   └── frontend/           # Vue.js/Quasar frontend application
├── pom.xml                 # Maven build configuration
├── mvnw, mvnw.cmd          # Maven wrapper scripts
└── .idea/                  # IntelliJ IDEA project files
```

## Backend Structure

```
src/main/java/com/softropic/promora/
├── common/                 # Shared utilities and base classes
│   ├── client/             # HTTP client abstractions
│   ├── config/             # Common configuration
│   ├── consumer/           # Consumer interface
│   ├── dto/                # Shared DTOs
│   ├── enums/              # Common enumerations
│   ├── exception/          # Base exceptions and error codes
│   ├── logging/            # Logging utilities
│   ├── message/            # Response wrappers (Success, Failure)
│   ├── payment/            # Payment abstractions
│   ├── persistence/        # Base entities, auditing
│   ├── refund/             # Refund policies
│   ├── threadpool/         # Thread pool utilities
│   ├── util/               # General utilities
│   └── validation/         # Custom validators
├── email/                  # Email module
│   ├── api/                # MailManager facade
│   ├── config/             # Email configuration
│   ├── dto/                # Email DTOs
│   ├── listener/           # Event listeners
│   └── service/            # MailService implementation
├── security/               # Security module (largest)
│   ├── api/                # REST controllers (AccountResource)
│   │   └── registration/   # Registration strategies
│   ├── audit/              # Audit logging
│   │   ├── filter/         # LoggingFilter
│   │   └── repository/     # AuditLog entity
│   ├── common/             # Security utilities
│   │   ├── service/        # LoginTokenManager interface
│   │   └── util/           # Security constants, cookie utils
│   ├── config/             # SecurityConfiguration
│   ├── core/               # Core security (DTO, mappers)
│   ├── domain/             # User, Authority, Customer entities
│   ├── exception/          # Security exceptions
│   ├── exposed/            # Public interfaces for other modules
│   │   ├── exception/      # Exposed exception types
│   │   └── util/           # AuthoritiesConstants, RequestMetadata
│   ├── jwt/                # JWT implementation
│   │   └── api/
│   │       └── filter/     # JWT filters
│   ├── manager/            # Business logic managers
│   ├── repository/         # User, Authority repositories
│   ├── secret/             # Secret/key management
│   │   └── repository/     # Secret, SecKey entities
│   └── service/            # User services
└── PromoraApplication.java # Main application entry point
```

## Frontend Structure

```
src/frontend/
├── src/
│   ├── assets/             # Static assets (images, etc.)
│   ├── boot/               # Quasar boot files
│   │   ├── axios.js        # HTTP client setup
│   │   └── i18n.js         # Internationalization setup
│   ├── components/         # Reusable Vue components
│   │   └── EssentialLink.vue
│   ├── css/                # Global styles
│   │   ├── app.scss
│   │   └── quasar.variables.scss
│   ├── i18n/               # Translation files
│   │   └── en-US/
│   │       └── index.js
│   ├── layouts/            # Page layouts
│   │   └── MainLayout.vue
│   ├── pages/              # Page components
│   │   ├── ErrorNotFound.vue
│   │   └── IndexPage.vue
│   ├── router/             # Vue Router configuration
│   │   ├── index.js
│   │   └── routes.js
│   ├── stores/             # Pinia state stores
│   │   └── index.js
│   └── App.vue             # Root component
├── public/                 # Public static files
├── quasar.config.js        # Quasar/Vite configuration
├── package.json            # npm dependencies
├── eslint.config.js        # ESLint configuration
├── postcss.config.js       # PostCSS configuration
└── index.html              # HTML entry point
```

## Resources Structure

```
src/main/resources/
├── application.yaml        # Base configuration
├── application-dev.yaml    # Development profile
├── config/                 # Additional config files
│   ├── logback-spring.xml  # Logging configuration
│   └── tls/                # TLS certificates (if any)
├── db/
│   └── migration/          # Flyway SQL migrations
├── static/                 # Static web resources
└── templates/              # Thymeleaf email templates
```

## Test Structure

```
src/test/java/com/softropic/promora/
├── common/
│   ├── configtest/         # Configuration tests
│   ├── validation/         # Validator tests
│   ├── HttpTestClient.java # Test HTTP utilities
│   ├── TestClockProvider.java
│   └── TransactionExceptionSimulator.java
├── config/
│   ├── ApplicationNoSecurity.java  # Test config without security
│   ├── CustomPostgresContainer.java # Testcontainers setup
│   └── TestConfig.java
├── security/
│   ├── api/util/           # API utility tests
│   ├── jwt/api/            # JWT tests
│   │   └── filter/         # Filter tests
│   ├── manager/            # Manager tests
│   ├── repo/               # Repository integration tests
│   ├── secret/             # Secret service tests
│   ├── service/            # Service tests
│   └── SecurityIT.java     # Security integration tests
├── utils/                  # Test utilities
│   └── sql/                # SQL assertion helpers
├── PromoraApplicationTests.java
├── TestPromoraApplication.java
└── TestcontainersConfiguration.java
```

## Key Files

**Configuration:**
- `pom.xml` - Maven dependencies and build plugins
- `src/main/resources/application-dev.yaml` - Main configuration
- `src/frontend/quasar.config.js` - Frontend build configuration

**Entry Points:**
- `src/main/java/com/softropic/promora/PromoraApplication.java` - Backend
- `src/frontend/src/App.vue` - Frontend

**Security:**
- `src/main/java/com/softropic/promora/security/config/SecurityConfiguration.java`
- `src/main/java/com/softropic/promora/security/jwt/api/JwtManagerImpl.java`

**API:**
- `src/main/java/com/softropic/promora/security/api/AccountResource.java`
- `src/main/java/com/softropic/promora/security/api/AccountManagementFacade.java`

**Domain:**
- `src/main/java/com/softropic/promora/security/domain/User.java`
- `src/main/java/com/softropic/promora/security/domain/Customer.java`

## Naming Conventions

**Java Packages:**
- `api` - REST controllers and DTOs
- `config` - Spring configuration classes
- `domain` - JPA entities
- `repository` - Spring Data repositories
- `service` - Business logic services
- `exposed` - Public interfaces for cross-module use
- `manager` - Complex business logic coordinators

**Java Classes:**
- `*Resource.java` - REST controllers
- `*Service.java` - Service layer classes
- `*Repository.java` - Data repositories
- `*Dto.java` - Data transfer objects
- `*Configuration.java` - Spring configuration
- `*Filter.java` - Servlet filters
- `*Listener.java` - Event listeners

**Frontend Files:**
- `*Page.vue` - Page components
- `*Layout.vue` - Layout components
- `*.js` - JavaScript modules

---

*Structure analysis: 2026-01-17*
