# Promora Frontend Security Features

## What This Is

Frontend authentication, security, and user profile features for the Promora Vue.js 3 + Quasar 2 application. Users can register, login, manage their accounts, view and update their profile, with full session handling and security audit trails.

## Core Value

Secure, user-friendly account management with proper security notifications for all sensitive changes.

## Current Milestone: v1.1 User Profile

**Goal:** Enable authenticated users to view and manage their profile information with security notifications and audit trails.

**Target features:**
- Profile page displaying user info (email, masked password, phone, address, core info, 2FA status)
- 6 independent profile update actions (email, password, phone, address, core info, 2FA)
- Backend API endpoints for profile management
- AccountChangeEvent listener for email notifications
- Generic profile change email template
- Security audit trail for all profile changes

## Requirements

### Validated (v1.0.0)

- ✓ Axios with request/response interceptors — v1.0.0
- ✓ Session manager with expiry warnings — v1.0.0
- ✓ Error handler utility — v1.0.0
- ✓ useSession, useErrorHandler, useLoading composables — v1.0.0
- ✓ authApi, accountApi, sessionApi services — v1.0.0
- ✓ GlobalLoadingBar, SessionWarningDialog — v1.0.0
- ✓ Login, Register, OTP, ForgotPassword, ResetPassword, Activate pages — v1.0.0
- ✓ Auth guards and route definitions — v1.0.0
- ✓ i18n (English + French) — v1.0.0

### Active (v1.1)

- [ ] Profile page with user info display
- [ ] Profile menu link in navigation
- [ ] Update email (with new email verification)
- [ ] Update password (requires current password)
- [ ] Update phone number
- [ ] Update address (single address)
- [ ] Update core user info (firstName, lastName, nationalId, gender, title, langKey)
- [ ] Toggle 2FA (requires password)
- [ ] Backend profile API endpoints (GET + 6 PUT)
- [ ] AccountChangeEvent listener
- [ ] Generic profile change email template
- [ ] Audit trail for profile changes

### Out of Scope

- Multiple addresses per user — single address sufficient for v1.1
- Profile photo upload — deferred to future milestone
- Account deletion — requires additional security considerations

## Context

**Codebase state:** Vue.js 3 + Quasar 2 frontend with Spring Boot backend. v1.0.0 delivered complete auth UI (6831 LOC added). Backend has existing AccountChangeEvent with actions for PASSWORD_CHANGED, EMAIL_CHANGED, ADDRESS_CHANGED, TWO_FACTOR_AUTH_*.

**Existing patterns to reuse:**
- AccountChangeEvent for profile change notifications
- SecurityAuditListener pattern for audit trails
- Thymeleaf email templates in `src/main/resources/mails/`
- Address entity with fields: name, companyName, addressLine1-3, city, stateProvince, postalCode, country

## Constraints

- **Vue API**: `<script setup>` only, no Options API
- **Language**: Plain JavaScript, no TypeScript
- **Styling**: Primary color `#1976d2`, mobile-first responsive
- **Forms**: `lazy-rules` for validation on blur
- **Buttons**: `:loading` and `:disable` on submit buttons
- **Cleanup**: `onUnmounted` for timers/listeners
- **i18n**: All user-facing text via `$t()` or `t()`
- **Components**: Under 250 lines, single responsibility
- **Backend**: Reuse existing patterns, AccountChangeEvent for notifications
- **Security**: Email change requires verification, password/2FA changes require current password

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Empty baseURL for axios | Backend uses relative paths with dev proxy | ✓ Good |
| Auto-redirect on 401 | Redirect to /login?expired=true on session expiry | ✓ Good |
| legacy: false for i18n | Enable useI18n() composable in Composition API | ✓ Good |
| 30-second check interval | Balance responsiveness and performance | ✓ Good |
| Cookie check for auth state | document.cookie.includes('user=') for route guard | ✓ Good |
| Single address per user | Simplifies v1.1 scope, multiple addresses deferred | — Pending |

## Milestone History

| Version | Status | Completed | Summary |
|---------|--------|-----------|---------|
| v1.0.0 | Validated | 2026-01-20 | Complete frontend auth UI (23 requirements, 8 phases) |
| v1.1 | Active | — | User profile management with security notifications |

---

*Last updated: 2026-01-27 after v1.1 milestone start*
