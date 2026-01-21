# Roadmap: Promora Frontend Security Features

## Overview

Implement complete frontend authentication and security features for the Vue.js 3 + Quasar 2 application. Starting with foundational infrastructure (axios, i18n), building up through API services and composables, then delivering page components, and finally integrating with router guards.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

- [x] **Phase 1: Foundation** - Axios interceptors, error utilities, i18n setup
- [x] **Phase 2: API Services** - Auth, account, and session API services
- [x] **Phase 3: Session Management** - Session plugin and composables
- [x] **Phase 4: Global Components** - Loading bar, session warning dialog
- [x] **Phase 5: Auth Pages** - Login, register, and OTP pages
- [x] **Phase 6: Account Pages** - Password reset and activation pages
- [x] **Phase 7: Router Integration** - Route definitions and auth guards
- [x] **Phase 8: Session Initialization** - Wire session monitoring after login (GAP CLOSURE)

## Phase Details

### Phase 1: Foundation
**Goal**: Establish core infrastructure for API communication and internationalization
**Depends on**: Nothing (first phase)
**Requirements**: INFRA-01, INFRA-03, I18N-01, I18N-02, I18N-03
**Success Criteria** (what must be TRUE):
  1. Axios instance configured with `withCredentials: true`
  2. Request interceptor tracks loading state and records activity
  3. Response interceptor handles 401/403/5xx errors appropriately
  4. Error handler utility parses ErrorDto into structured objects
  5. All translation keys available in English and French
**Research**: Unlikely
**Plans**: TBD

### Phase 2: API Services
**Goal**: Create typed API service layer for backend communication
**Depends on**: Phase 1
**Requirements**: API-01, API-02, API-03
**Success Criteria** (what must be TRUE):
  1. authApi exposes login, verifyOtp, logout, checkAuth methods
  2. accountApi exposes register, resendActivation, activate, requestPasswordReset, resetPassword, getAccount methods
  3. sessionApi exposes refresh method
  4. All services use the configured axios instance
**Research**: Unlikely
**Plans**: TBD

### Phase 3: Session Management
**Goal**: Implement session tracking with expiry warnings and reactive state
**Depends on**: Phase 1, Phase 2
**Requirements**: INFRA-02, COMP-01, COMP-02, COMP-03
**Success Criteria** (what must be TRUE):
  1. Session manager tracks activity and warns 2 minutes before 15-min expiry
  2. useSession provides reactive showWarning, timeUntilExpiry, and refresh/logout actions
  3. useErrorHandler provides reactive error state with field-level error access
  4. useLoading tracks pending requests and provides isLoading state
  5. Session expiry dispatches `session:expired` event
**Research**: Unlikely
**Plans**: TBD

### Phase 4: Global Components
**Goal**: Create app-level components for loading and session warnings
**Depends on**: Phase 3
**Requirements**: GLOB-01, GLOB-02, GLOB-03
**Success Criteria** (what must be TRUE):
  1. GlobalLoadingBar shows progress indicator when requests are pending
  2. SessionWarningDialog appears when session is expiring with continue/logout options
  3. App.vue includes both global components
  4. Components are mobile-responsive
**Research**: Unlikely
**Plans**: TBD

### Phase 5: Auth Pages
**Goal**: Implement login, registration, and OTP verification pages
**Depends on**: Phase 2, Phase 3, Phase 4
**Requirements**: PAGE-01, PAGE-02, PAGE-03
**Success Criteria** (what must be TRUE):
  1. User can log in with email/password
  2. User sees session expired message when redirected with `?expired=true`
  3. User is redirected to OTP page when 2FA is required
  4. User can register with full form validation
  5. User can enter OTP with auto-submit on completion
  6. All pages show field-level validation errors
  7. All pages are mobile-responsive
**Research**: Unlikely
**Plans**: TBD

### Phase 6: Account Pages
**Goal**: Implement password reset and account activation flows
**Depends on**: Phase 2, Phase 3
**Requirements**: PAGE-04, PAGE-05, PAGE-06
**Success Criteria** (what must be TRUE):
  1. User can request password reset with email and DOB
  2. User can set new password via reset link
  3. User account activates automatically when visiting activation link
  4. Invalid/expired keys show appropriate error messages
  5. All pages are mobile-responsive
**Research**: Unlikely
**Plans**: TBD

### Phase 7: Router Integration
**Goal**: Configure routes with authentication guards
**Depends on**: Phase 5, Phase 6
**Requirements**: ROUT-01, ROUT-02
**Success Criteria** (what must be TRUE):
  1. All auth pages route to correct components
  2. Auth routes have `requiresGuest: true` meta
  3. Protected routes have `requiresAuth: true` meta
  4. Unauthenticated users redirected to login with return URL
  5. Authenticated users redirected away from guest-only pages
**Research**: Unlikely
**Plans**: TBD

### Phase 8: Session Initialization (GAP CLOSURE)
**Goal**: Wire session monitoring to start after successful authentication
**Depends on**: Phase 5, Phase 7
**Gap Closure**: Closes integration gap from v1-MILESTONE-AUDIT.md
**Success Criteria** (what must be TRUE):
  1. LoginPage calls initSession() after successful login
  2. OtpPage calls initSession() after successful OTP verification
  3. Session warning dialog appears 2 minutes before session expiry
**Research**: None (fix is documented in audit)
**Plans**: TBD

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Foundation | 2/2 | Complete | 2026-01-17 |
| 2. API Services | 1/1 | Complete | 2026-01-17 |
| 3. Session Management | 2/2 | Complete | 2026-01-17 |
| 4. Global Components | 1/1 | Complete | 2026-01-17 |
| 5. Auth Pages | 3/3 | Complete | 2026-01-19 |
| 6. Account Pages | 1/1 | Complete | 2026-01-19 |
| 7. Router Integration | 1/1 | Complete | 2026-01-19 |
| 8. Session Initialization | 1/1 | Complete | 2026-01-20 |

---

*Roadmap created: 2026-01-17*
