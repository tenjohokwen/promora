---
phase: 06-account-pages
plan: 01
subsystem: frontend-auth
tags: [vue, quasar, password-reset, account-activation, forms]

dependency-graph:
  requires:
    - 01-foundation (axios, i18n)
    - 02-api-services (account.api.js)
    - 05-auth-pages (pattern reference)
  provides:
    - ForgotPasswordPage.vue
    - ResetPasswordPage.vue
    - ActivatePage.vue
  affects:
    - 06-02 (if additional pages planned)
    - Router configuration (routes need to be added)

tech-stack:
  added: []
  patterns:
    - Auto-activation on mount with timer cleanup
    - Success state toggling (form/success message)
    - Query parameter validation

key-files:
  created:
    - src/frontend/src/pages/auth/ForgotPasswordPage.vue
    - src/frontend/src/pages/auth/ResetPasswordPage.vue
    - src/frontend/src/pages/auth/ActivatePage.vue
  modified: []

decisions:
  - "Replace '?' in forgotPassword title for cleaner heading"
  - "Use computed for route.query.key to ensure reactivity"
  - "Auto-activate on mount rather than button click"
  - "3-second countdown before redirect after activation"

metrics:
  duration: ~2 min
  completed: 2026-01-19
---

# Phase 06 Plan 01: Account Pages Summary

Three Vue pages for password recovery and account activation using established auth page patterns.

## What Was Built

### ForgotPasswordPage.vue
- Email and date of birth inputs for password reset request
- Success state toggling between form and confirmation message
- Error handling with useErrorHandler composable
- Calls `accountApi.requestPasswordReset(email, dob, email)`

### ResetPasswordPage.vue
- Password and confirm password inputs with visibility toggles
- Password match validation
- Reads reset key from `?key=xxx` query parameter
- Shows error if key is missing/invalid
- On success: shows notification, redirects to login

### ActivatePage.vue
- Auto-activates account on component mount
- Loading spinner during API call
- 3-second countdown on success before redirect
- Proper timer cleanup in onUnmounted
- Handles missing/invalid activation keys

## Technical Approach

All three pages follow the established patterns from LoginPage.vue and RegisterPage.vue:
- `<script setup>` composition API
- useErrorHandler for error state management
- useI18n for all user-facing text
- Quasar components (q-page, q-card, q-input, q-banner, q-btn)
- Mobile-responsive classes (col-12 col-sm-8 col-md-6 col-lg-4)

## Commits

| Commit | Description |
|--------|-------------|
| 043c26f | feat(06-01): create ForgotPasswordPage.vue |
| 7a597bf | feat(06-01): create ResetPasswordPage.vue |
| 29627b8 | feat(06-01): create ActivatePage.vue |

## Deviations from Plan

None - plan executed exactly as written.

## Next Phase Readiness

**Ready for:**
- Router configuration to add routes for /forgot-password, /reset-password, /activate
- Integration testing with backend API

**No blockers identified.**
