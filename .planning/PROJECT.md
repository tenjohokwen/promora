# Promora Frontend Security Features

## What

Implement frontend authentication and security features for the Promora Vue.js 3 + Quasar 2 application. This includes complete user authentication flows, account management, session handling, and supporting infrastructure.

## Why

The backend security infrastructure is complete (JWT authentication, 2FA/OTP, account management APIs), but the frontend lacks the UI components and client-side logic to consume these APIs. Users need a functional interface to register, login, manage their accounts, and interact with the security features.

## Core Requirements

### Infrastructure
1. **Axios with Interceptors** - Request/response interceptors for session management, error handling, loading state tracking, and auto-logout on 401
2. **Session Manager Plugin** - Track activity, warn before 15-minute session expiry, auto-refresh capability
3. **Error Handler Utility** - Parse backend ErrorDto responses into structured objects

### Composables
4. **useSession** - Reactive session state with warning dialog, refresh, and logout actions
5. **useErrorHandler** - Form error state management with field-level errors
6. **useLoading** - Global loading state tracking for pending requests

### API Services
7. **authApi** - Login, OTP verification, logout, auth check
8. **accountApi** - Registration, activation, password reset (init/finish)
9. **sessionApi** - Session refresh

### Global Components
10. **GlobalLoadingBar** - Fixed-position loading indicator at top of viewport
11. **SessionWarningDialog** - Session expiry warning with continue/logout options

### Page Components
12. **LoginPage** - Email/password login with 2FA redirect handling
13. **RegisterPage** - Multi-field registration with password confirmation
14. **OtpPage** - 6-digit OTP input with auto-submit and resend
15. **ForgotPasswordPage** - Password reset initiation with email/DOB
16. **ResetPasswordPage** - New password entry with confirmation
17. **ActivatePage** - Account activation via email link

### Router
18. **Auth Guards** - Protect authenticated routes, redirect guests appropriately
19. **Route Definitions** - All auth pages with proper meta flags

### i18n
20. **English Translations** - Complete en-US translation file
21. **French Translations** - Complete fr-FR translation file

## Technical Constraints

- **Vue API**: `<script setup>` only, no Options API
- **Language**: Plain JavaScript, no TypeScript
- **Styling**: Primary color `#1976d2`, mobile-first responsive
- **Forms**: `lazy-rules` for validation on blur
- **Buttons**: `:loading` and `:disable` on submit buttons
- **Cleanup**: `onUnmounted` for timers/listeners
- **i18n**: All user-facing text via `$t()` or `t()`
- **Components**: Under 250 lines, single responsibility

## API Integration

- Backend base URL: Configure via environment variable (currently placeholder)
- JWT stored in HttpOnly cookies (backend handles this)
- `user` cookie (non-HttpOnly) used for client-side auth state detection
- Cookies: `withCredentials: true` on all requests

## Success Criteria

- [x] All 6 auth pages render correctly and are mobile-responsive
- [x] Login flow works for both standard and 2FA-enabled accounts
- [x] Session warning appears 2 minutes before 15-minute expiry
- [x] Error messages display correctly with field-level validation
- [x] All text displays in English or French based on locale
- [x] Router guards prevent unauthorized access
- [x] Loading indicator shows during API requests

## Milestone History

| Version | Status | Completed | Summary |
|---------|--------|-----------|---------|
| v1.0.0 | Validated | 2026-01-20 | Complete frontend auth UI (23 requirements, 8 phases) |

---

*Project initialized: 2026-01-17*
*v1.0.0 completed: 2026-01-20*
