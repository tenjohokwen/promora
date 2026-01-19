# Requirements: Promora Frontend Security Features

**Source:** docs/frontend-implementation-spec.md
**Version:** v1

## Categories

### Infrastructure (INFRA)

| ID | Requirement | Priority |
|----|-------------|----------|
| INFRA-01 | Axios instance with request/response interceptors for cross-cutting concerns | v1 |
| INFRA-02 | Session manager plugin tracking activity and warning before 15-min expiry | v1 |
| INFRA-03 | Error handler utility parsing backend ErrorDto into structured objects | v1 |

### Composables (COMP)

| ID | Requirement | Priority |
|----|-------------|----------|
| COMP-01 | useSession composable with reactive session state, refresh, and logout | v1 |
| COMP-02 | useErrorHandler composable with form error state and field-level errors | v1 |
| COMP-03 | useLoading composable tracking global loading state from pending requests | v1 |

### API Services (API)

| ID | Requirement | Priority |
|----|-------------|----------|
| API-01 | authApi service with login, OTP verification, logout, and auth check | v1 |
| API-02 | accountApi service with registration, activation, password reset | v1 |
| API-03 | sessionApi service with session refresh | v1 |

### Global Components (GLOB)

| ID | Requirement | Priority |
|----|-------------|----------|
| GLOB-01 | GlobalLoadingBar component showing loading indicator during requests | v1 |
| GLOB-02 | SessionWarningDialog component with continue/logout options | v1 |
| GLOB-03 | App.vue integration including global components | v1 |

### Page Components (PAGE)

| ID | Requirement | Priority |
|----|-------------|----------|
| PAGE-01 | LoginPage with email/password, 2FA redirect handling, session expired message | v1 |
| PAGE-02 | RegisterPage with multi-field form, password confirmation, validation | v1 |
| PAGE-03 | OtpPage with 6-digit input, auto-submit, resend capability | v1 |
| PAGE-04 | ForgotPasswordPage with email/DOB for password reset initiation | v1 |
| PAGE-05 | ResetPasswordPage with new password entry and confirmation | v1 |
| PAGE-06 | ActivatePage with auto-activation on mount and error handling | v1 |

### Router (ROUT)

| ID | Requirement | Priority |
|----|-------------|----------|
| ROUT-01 | Route definitions for all auth pages with proper meta flags | v1 |
| ROUT-02 | Auth guards protecting authenticated routes, redirecting guests | v1 |

### Internationalization (I18N)

| ID | Requirement | Priority |
|----|-------------|----------|
| I18N-01 | English (en-US) translation file with all required keys | v1 |
| I18N-02 | French (fr-FR) translation file with all required keys | v1 |
| I18N-03 | i18n boot file configuration | v1 |

## Summary

| Category | v1 Count |
|----------|----------|
| Infrastructure | 3 |
| Composables | 3 |
| API Services | 3 |
| Global Components | 3 |
| Page Components | 6 |
| Router | 2 |
| Internationalization | 3 |
| **Total** | **23** |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| INFRA-01 | Phase 1 | Complete |
| INFRA-02 | Phase 3 | Complete |
| INFRA-03 | Phase 1 | Complete |
| COMP-01 | Phase 3 | Complete |
| COMP-02 | Phase 3 | Complete |
| COMP-03 | Phase 3 | Complete |
| API-01 | Phase 2 | Complete |
| API-02 | Phase 2 | Complete |
| API-03 | Phase 2 | Complete |
| GLOB-01 | Phase 4 | Complete |
| GLOB-02 | Phase 4 | Complete |
| GLOB-03 | Phase 4 | Complete |
| PAGE-01 | Phase 5 | Pending |
| PAGE-02 | Phase 5 | Pending |
| PAGE-03 | Phase 5 | Pending |
| PAGE-04 | Phase 6 | Pending |
| PAGE-05 | Phase 6 | Pending |
| PAGE-06 | Phase 6 | Pending |
| ROUT-01 | Phase 7 | Pending |
| ROUT-02 | Phase 7 | Pending |
| I18N-01 | Phase 1 | Complete |
| I18N-02 | Phase 1 | Complete |
| I18N-03 | Phase 1 | Complete |

**Coverage:**
- v1 requirements: 23 total
- Mapped to phases: 23
- Unmapped: 0

---

*Requirements derived: 2026-01-17*
*Source: docs/frontend-implementation-spec.md*
