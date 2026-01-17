# External Integrations

**Analysis Date:** 2026-01-17

## Databases

**PostgreSQL (Primary Database):**
- Purpose: Main application data storage
- Connection: `jdbc:postgresql://localhost/promora?TimeZone=UTC`
- Configuration: `src/main/resources/application-dev.yaml:42-63`
- Schema: `main` (default_schema in Hibernate config)
- Features used:
  - Flyway migrations (`src/main/resources/db/migration/`)
  - Hibernate Envers for audit history
  - TSID for ID generation (via Hypersistence Utils)
  - Batch inserts with `reWriteBatchedInserts`
- Connection pool: HikariCP with 25 max connections

## External APIs

**MTN MoMo (Mobile Money):**
- Purpose: Mobile payment collection
- Base URL: `https://sandbox.momodeveloper.mtn.com/collection`
- Configuration: `src/main/resources/application-dev.yaml:146-169`
- Endpoints:
  - `POST /v1_0/requesttopay` - Initiate payment request
  - `GET /v1_0/requesttopay/{referenceId}` - Check transaction status
  - `GET /v1_0/account/balance` - Get account balance
  - `GET /v1_0/accountholder/{type}/{id}/basicuserinfo` - Get user info
  - `POST /token/` - Request auth token
- Authentication: Subscription key header (`Ocp-Apim-Subscription-Key`)
- Environment: Sandbox (X-Target-Environment: sandbox)
- Client implementation: `src/main/java/com/softropic/promora/common/client/`

## Email Providers

**SMTP Email (Multiple Providers):**
- Purpose: Transactional email (registration, password reset, notifications)
- Configuration: `src/main/resources/application-dev.yaml:69-109`
- Providers configured:
  1. GMX (`mail.gmx.net:587`)
  2. Gmail (`smtp.gmail.com:587`)
  3. Mail.de (`smtp.mail.de:587`)
- Features: STARTTLS enabled, SMTP auth
- Implementation:
  - `src/main/java/com/softropic/promora/email/service/MailService.java`
  - `src/main/java/com/softropic/promora/email/api/MailManager.java`
- Template engine: Thymeleaf (`src/main/resources/templates/`)

## Logging & Monitoring

**Grafana Loki (Optional):**
- Purpose: Centralized log aggregation
- Configuration: `src/main/resources/config/logback-spring.xml`
- Authentication: Via `LOKI_API_KEY` environment variable
- Encoder: Logstash JSON format

**Prometheus (Metrics):**
- Purpose: Application metrics collection
- Endpoint: `/manage/prometheus`
- Configuration: `spring-boot-starter-actuator` + `micrometer-registry-prometheus`
- Exposed via: Spring Boot Actuator at `/manage/**`

## Authentication Services

**JWT (Internal):**
- Purpose: Stateless authentication tokens
- Implementation: `src/main/java/com/softropic/promora/security/jwt/`
- Library: JJWT 0.12.6
- Storage: HttpOnly cookies
- Secret management: Database-stored secrets (`SecretService`)

## Webhooks

**No outbound webhooks configured.**

The application receives requests but does not appear to send webhook notifications to external services.

## Third-Party Libraries with External Calls

**libphonenumber:**
- Purpose: Phone number validation and formatting
- No external API calls (local validation only)

**User Agent Parser (uap-java):**
- Purpose: Parse User-Agent headers for device detection
- No external API calls (local parsing only)

## Integration Patterns

**HTTP Client Pattern:**
- Base: `src/main/java/com/softropic/promora/common/client/AbstractClient.java`
- Configuration: `src/main/java/com/softropic/promora/common/client/ClientConfiguration.java`
- TCP settings: Configurable timeouts, connection pools
- Default timeouts:
  - Read: 15000ms
  - Connection: 15000ms
  - Connection request: 2000ms
- Max connections: 50 total, 25 per route

**Event-Driven Integration:**
- Spring Application Events for async processing
- Email sending triggered via `Envelope` events
- Listener: `src/main/java/com/softropic/promora/email/listener/SendMailListener.java`

---

*Integrations analysis: 2026-01-17*
