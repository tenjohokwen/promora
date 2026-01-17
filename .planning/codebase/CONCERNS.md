# Codebase Concerns

**Analysis Date:** 2026-01-17

## Tech Debt

**SMS Registration Not Implemented:**
- Issue: `SmsRegistrationStrategy` is a complete stub returning random UUIDs
- Files: `src/main/java/com/softropic/promora/security/api/registration/SmsRegistrationStrategy.java`
- Impact: Phone-based registration silently fails; users receive no SMS
- Fix approach: Integrate SMS gateway (Twilio, AWS SNS) and implement message sending

**Email Send Failure Not Handled:**
- Issue: `MailService.sendEmail()` has TODO for handling send failures (quota exceeded, network issues)
- Files: `src/main/java/com/softropic/promora/email/service/MailService.java:69`
- Impact: Email failures may go unnoticed, users may not receive critical emails
- Fix approach: Implement retry mechanism with dead-letter queue for failed sends

**In-Memory Login Attempts Cache - Not Scalable:**
- Issue: `LoginAttemptsService` uses Guava LoadingCache which does not work for multi-node deployments
- Files: `src/main/java/com/softropic/promora.security/manager/LoginAttemptsService.java:37`
- Impact: Brute force protection bypassed by targeting different nodes
- Fix approach: Migrate to Redis or database-backed distributed cache

**Client Blacklist Not Persisted:**
- Issue: Blacklisted clients stored in-memory only; `blacklistClient()` records but does not prevent access
- Files: `src/main/java/com/softropic/promora.security/manager/LoginAttemptsService.java:142-143`
- Impact: Fraudulent clients not actually blocked; data lost on restart
- Fix approach: Create database table for blacklisted clients with service layer

**Allowed Machine Clients Hardcoded:**
- Issue: `ClientIdAccessDecisionManager` uses config-file list instead of database
- Files: `src/main/java/com/softropic/promora.security/manager/ClientIdAccessDecisionManager.java:29-32`
- Impact: Requires redeploy to add/remove allowed clients
- Fix approach: Move allowed clients to database with admin management interface

**Rate Limiting Not Implemented:**
- Issue: Multiple TODOs indicate rate limiting is planned but not implemented
- Files:
  - `src/main/java/com/softropic/promora.security/api/AccountManagementFacade.java:112`
  - `src/main/java/com/softropic/promora.security/api/registration/EmailRegistrationStrategy.java:97`
- Impact: Account enumeration, brute force, and spam attacks possible
- Fix approach: Implement rate limiting at API gateway or using Spring interceptors

**Frontend API Configuration Placeholder:**
- Issue: Axios configured with placeholder `https://api.example.com`
- Files: `src/frontend/src/boot/axios.js:10`
- Impact: Frontend cannot communicate with backend
- Fix approach: Configure proper backend URL via environment variables

## Known Bugs

**Nullable Return in Security-Critical Paths:**
- Symptoms: Several methods return null instead of throwing exceptions
- Files:
  - `src/main/java/com/softropic/promora.security/api/AccountManagementFacade.java:160` (resendRegistrationLink)
  - `src/main/java/com/softropic/promora.security/api/AccountManagementFacade.java:176` (changeEmail)
- Trigger: Edge cases where user not found or password mismatch
- Workaround: Callers must handle null returns

**Token Fixation Check Incomplete:**
- Symptoms: `isTokenFixed()` returns false when token is blank without full validation
- Files: `src/main/java/com/softropic/promora.security/jwt/api/JwtManagerImpl.java:273`
- Trigger: Requests without JWT cookie
- Workaround: None documented; potential security gap

## Security Considerations

**Hardcoded Credentials in Configuration:**
- Risk: Email provider passwords exposed in `application-dev.yaml`
- Files: `src/main/resources/application-dev.yaml:73,97,103,109`
- Current mitigation: Only in dev profile (but still in repository)
- Recommendations:
  - Move all credentials to environment variables or secrets manager
  - Never commit credentials, even for dev
  - Rotate exposed credentials immediately

**Database Password in Plain Text:**
- Risk: PostgreSQL credentials exposed
- Files: `src/main/resources/application-dev.yaml:44-45`
- Current mitigation: Dev profile only
- Recommendations: Use environment variables or Docker secrets

**Request/Response Body Logging:**
- Risk: Sensitive data logged in plain text (passwords, tokens)
- Files: `src/main/java/com/softropic/promora.security/audit/filter/LoggingFilter.java:68-69,106`
- Current mitigation: TODOs indicate plan to encrypt
- Recommendations: Implement body sanitization or encryption before logging

**CSRF Disabled:**
- Risk: Cross-site request forgery attacks possible
- Files: `src/main/java/com/softropic/promora.security/config/SecurityConfiguration.java:167`
- Current mitigation: Comment states "not needed for JWT" but not validated
- Recommendations: Review CSRF protection for cookie-based JWT storage

**Frame Options Disabled:**
- Risk: Clickjacking attacks possible
- Files: `src/main/java/com/softropic/promora.security/config/SecurityConfiguration.java:187`
- Current mitigation: None observed
- Recommendations: Re-enable frame options or configure allowed origins

**Hibernate DDL Auto Create in Dev:**
- Risk: Schema auto-creation could cause data loss
- Files: `src/main/resources/application-dev.yaml:28`
- Current mitigation: Dev profile only
- Recommendations: Use Flyway migrations consistently; set to `validate` in all profiles

## Performance Bottlenecks

**JwtManagerImpl - Largest Service File:**
- Problem: 533 lines with complex token management logic
- Files: `src/main/java/com/softropic/promora.security/jwt/api/JwtManagerImpl.java`
- Cause: Single class handles token creation, validation, refresh, claims extraction
- Improvement path: Extract into smaller specialized services (TokenCreator, TokenValidator, ClaimsExtractor)

**Synchronous Email Sending Risk:**
- Problem: While `@Async` is used, email failures could block the async thread pool
- Files: `src/main/java/com/softropic/promora.email/api/MailManager.java:40-42`
- Cause: No circuit breaker or timeout on email operations
- Improvement path: Add circuit breaker pattern with resilience4j

**N+1 Query Risk in User Loading:**
- Problem: Eager loading authorities on every user load
- Files: `src/main/java/com/softropic/promora.security/service/LoadUserByUserNameService.java`
- Cause: No pagination or projection optimization
- Improvement path: Review JPA fetch strategies and add query optimization

## Fragile Areas

**JWT Token Management:**
- Files: `src/main/java/com/softropic/promora.security/jwt/api/JwtManagerImpl.java`
- Why fragile: Complex state management, multiple cookie operations, volatile secret
- Safe modification: Add comprehensive unit tests before changes
- Test coverage: Some tests exist but not comprehensive (`JwtManagerImplTest.java`)

**Security Filter Chain:**
- Files: `src/main/java/com/softropic/promora.security/config/SecurityConfiguration.java`
- Why fragile: Filter order matters; many interdependencies
- Safe modification: Integration tests must cover all auth scenarios
- Test coverage: `SecurityIT.java` exists but limited scenarios

**Login Attempts Caching:**
- Files: `src/main/java/com/softropic/promora.security/manager/LoginAttemptsService.java`
- Why fragile: Complex cache invalidation logic across multiple caches
- Safe modification: Ensure `LoginAttemptsServiceTest.java` covers edge cases
- Test coverage: Tests exist but timing-dependent tests are brittle

## Scaling Limits

**Guava Cache for Login Tracking:**
- Current capacity: Single-node only
- Limit: Multi-node deployment breaks protection
- Scaling path: Migrate to Redis with atomic increment operations

**Database Connection Pool:**
- Current capacity: 25 max connections per node
- Limit: PostgreSQL default is 100 total connections (4 nodes max)
- Scaling path: Use PgBouncer or increase PostgreSQL max_connections

## Dependencies at Risk

**Jasypt 1.9.3:**
- Risk: Older encryption library, last release 2014
- Impact: Potential security vulnerabilities
- Migration plan: Consider Spring Security Crypto or Bouncy Castle

**libphonenumber 9.0.5:**
- Risk: Outdated version (current is 8.13+)
- Impact: Phone validation may miss new number formats
- Migration plan: Update to latest version

## Missing Critical Features

**Two-Factor Authentication Incomplete:**
- Problem: OTP infrastructure exists but SMS delivery not implemented
- Files: `src/main/java/com/softropic/promora.security/manager/TwoFactorLoginManager.java`
- Blocks: Secure two-factor authentication for users

**IP Whitelist Not Implemented:**
- Problem: TODO indicates IP whitelist planned but not built
- Files: `src/main/java/com/softropic/promora.security/manager/LoginAttemptsService.java:226`
- Blocks: Restricting access to known safe IPs

**Audit Trail Session Correlation:**
- Problem: Session ID not added to audit logs
- Files: `src/main/java/com/softropic/promora.security/audit/repository/AuditLog.java:37`
- Blocks: Tracing user activities across requests

## Test Coverage Gaps

**Registration Flow Untested:**
- What's not tested: `AccountManagementFacade.registerAccount()`, email/SMS strategies
- Files: `src/main/java/com/softropic/promora.security/api/AccountManagementFacade.java`
- Risk: Registration bugs go unnoticed; security issues in account creation
- Priority: High

**Password Reset Flow Untested:**
- What's not tested: `PasswordResetService`, email sending on reset
- Files: `src/main/java/com/softropic/promora.security/service/PasswordResetService.java`
- Risk: Password reset could fail silently or expose security issues
- Priority: High

**Frontend Completely Untested:**
- What's not tested: All Vue.js components, Pinia stores, routing
- Files: `src/frontend/src/**/*.{vue,js}`
- Risk: UI regressions, broken user flows
- Priority: Medium

**Mail Service Integration:**
- What's not tested: `MailService` with actual SMTP server
- Files: `src/main/java/com/softropic/promora.email/service/MailService.java`
- Risk: Email delivery failures in production
- Priority: Medium

**Controller/API Tests Limited:**
- What's not tested: Most REST endpoints lack integration tests
- Files: `src/main/java/com/softropic/promora.security/api/AccountResource.java`
- Risk: API contract breaks, response format issues
- Priority: High

---

*Concerns audit: 2026-01-17*
