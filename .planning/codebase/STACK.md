# Technology Stack

**Analysis Date:** 2026-01-17

## Languages

**Primary:**
- Java 17 - Backend API and business logic (`src/main/java/com/softropic/promora/`)
- JavaScript (ES Modules) - Frontend application (`src/frontend/src/`)

**Secondary:**
- SQL - Database migrations and queries (Flyway migrations in `src/main/resources/db/migration/`)
- YAML - Configuration (`src/main/resources/application*.yaml`)
- XML - Maven build configuration (`pom.xml`)

## Runtime

**Backend Environment:**
- JVM (Java 17) - Spring Boot application runtime
- Embedded Tomcat - Web server (configured in `application-dev.yaml`)

**Frontend Environment:**
- Node.js v22.16.0 - Build and development runtime
- npm 11.4.2 - Package manager

**Package Managers:**
- Maven - Java dependency management (`pom.xml`)
- npm - JavaScript dependency management (`src/frontend/package.json`)
- Lockfiles: `package-lock.json` present for frontend

## Frameworks

**Backend Core:**
- Spring Boot 3.5.7 - Application framework (`pom.xml` parent)
- Spring Security 6 - Authentication/authorization (`src/main/java/com/softropic/promora/security/config/SecurityConfiguration.java`)
- Spring Data JPA - Data persistence (`spring-boot-starter-data-jpa`)
- Hibernate 6.6.14 - ORM with Envers for auditing (`hibernate-envers`)
- Flyway - Database migrations (`flyway-core`, `flyway-database-postgresql`)

**Frontend Core:**
- Vue.js 3.5.22 - UI framework (`src/frontend/package.json`)
- Quasar 2.16.0 - Component library and build tooling (`@quasar/app-vite`)
- Vue Router 4 - Client-side routing (`src/frontend/src/router/`)
- Pinia 3.0.1 - State management (`src/frontend/src/stores/`)
- Axios 1.2.1 - HTTP client (`src/frontend/src/boot/axios.js`)
- Vue I18n 11.0.0 - Internationalization (`vue-i18n`)

**Testing:**
- JUnit 5 - Unit testing (via `spring-boot-starter-test`)
- Testcontainers - Integration testing with PostgreSQL (`testcontainers`, `spring-boot-testcontainers`)
- Mockito - Mocking (`mockito-core`)
- AssertJ 3.24.2 - Fluent assertions (`assertj-core`)
- Instancio 2.10.0 - Test data generation (`instancio-core`)
- Awaitility 4.2.0 - Async testing (`awaitility`)
- JSON Unit 4.1.0 - JSON assertions (`json-unit-assertj`)

**Build/Dev:**
- Maven 3.x - Build tool (via `mvnw` wrapper)
- Vite - Frontend build tool (via `@quasar/app-vite`)
- ESLint 9.14.0 - JavaScript linting (`src/frontend/eslint.config.js`)
- Prettier 3.3.3 - Code formatting (`src/frontend/.prettierrc.json`)
- frontend-maven-plugin 1.15.1 - Node/npm integration in Maven build

## Key Dependencies

**Critical Backend:**
- `jjwt-api` 0.12.6 - JWT token generation/validation (`src/main/java/com/softropic/promora/security/jwt/`)
- `postgresql` - PostgreSQL JDBC driver
- `hypersistence-utils-hibernate-63` 3.9.10 - Advanced Hibernate types
- `lombok` - Boilerplate reduction (annotation processor)
- `mapstruct` 1.6.3 - Object mapping (`src/main/java/com/softropic/promora/security/core/mapper/`)
- `jasypt` 1.9.3 - Encryption utilities (`src/main/java/com/softropic/promora/security/secret/`)
- `libphonenumber` 9.0.5 - Phone number validation (`src/main/java/com/softropic/promora/common/validation/`)
- `sqids` 0.1.0 - ID obfuscation

**Infrastructure:**
- `spring-boot-starter-actuator` - Health checks and metrics
- `micrometer-registry-prometheus` - Prometheus metrics export
- `logstash-logback-encoder` 8.1 - Structured logging
- `spring-boot-starter-mail` - Email sending via SMTP
- `spring-boot-starter-cache` - Caching abstraction
- `spring-retry` - Retry mechanism

**Utilities:**
- `guava` 33.4.8-jre - Google core libraries
- `commons-lang3` 3.20.0, `commons-text` 1.13.1, `commons-validator` 1.9.0 - Apache utilities
- `commons-codec` 1.19.0 - Encoding utilities
- `uap-java` 1.6.1 - User agent parsing

## Configuration

**Environment Configuration:**
- `src/main/resources/application.yaml` - Base Spring configuration (minimal)
- `src/main/resources/application-dev.yaml` - Development profile with full configuration
- `src/frontend/quasar.config.js` - Frontend build and dev server configuration

**Key Environment Variables Required:**
- Database: Connection configured in `application-dev.yaml` (hardcoded for dev)
- Mail: SMTP credentials in `application-dev.yaml` (`spring.mail.*`)
- Loki: `LOKI_API_KEY` for log shipping (`src/main/resources/config/logback-spring.xml`)
- Allowed clients: `allowed.clients` property

**Build Configuration:**
- `pom.xml` - Maven build with annotation processors (Lombok, MapStruct)
- `src/frontend/quasar.config.js` - Quasar/Vite build configuration
- `src/frontend/eslint.config.js` - ESLint flat config
- `src/frontend/postcss.config.js` - PostCSS configuration

**Annotation Processors:**
- Lombok - Getters, setters, builders
- MapStruct 1.6.3 - DTO mapping with Lombok binding

## Platform Requirements

**Development:**
- JDK 17+
- Node.js 20/22/24/26/28 (per `package.json` engines)
- PostgreSQL (or Docker for Testcontainers)
- Docker (for Testcontainers integration tests)
- Maven 3.x (wrapper included)

**Production:**
- JVM 17+
- PostgreSQL database
- SMTP server for email
- Optional: Grafana Loki for centralized logging
- Optional: Prometheus for metrics scraping

**Build Output:**
- Frontend SPA built to `src/frontend/dist/spa/`
- Frontend copied to `target/classes/static/` during Maven build
- Spring Boot JAR with embedded frontend

---

*Stack analysis: 2026-01-17*
