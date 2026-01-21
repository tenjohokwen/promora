---
phase: 01-foundation
plan: 01
subsystem: api
tags: [axios, interceptors, error-handling, loading-state, withCredentials]

# Dependency graph
requires:
  - phase: none
    provides: N/A
provides:
  - Axios instance with withCredentials for cookie-based auth
  - Request/response interceptors with loading state tracking
  - Error parser utility for backend ErrorDto structure
  - onLoadingChange subscriber pattern for global loading state
affects: [api-services, session-management, composables]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Subscriber pattern for loading state via onLoadingChange"
    - "Response interceptor unwraps data automatically"
    - "Error handling by status code (401/403/5xx)"

key-files:
  created:
    - src/frontend/src/utils/errorHandler.js
  modified:
    - src/frontend/src/boot/axios.js

key-decisions:
  - "Empty baseURL since backend uses relative paths with dev proxy"
  - "Auto-redirect to /login on 401 with sessionExpired/unauthorized errorKey"
  - "Unwrap response.data in interceptor so API calls receive data directly"

patterns-established:
  - "Error parsing via parseApiError extracts helpCode, errorKey, message, fieldErrors"
  - "Loading state via subscription not global store for flexibility"
  - "Default error keys by HTTP status when backend errorMsg is missing"

# Metrics
duration: 2min
completed: 2026-01-17
---

# Phase 1 Plan 1: Axios and Error Handler Summary

**Axios instance configured with withCredentials, request/response interceptors for loading state and error handling, plus error parser utility for backend ErrorDto structure**

## Performance

- **Duration:** 2 min
- **Started:** 2026-01-17T15:57:57Z
- **Completed:** 2026-01-17T16:00:20Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments

- Axios instance with withCredentials: true for cookie-based authentication
- Request interceptor incrementing pending request counter and notifying subscribers
- Response interceptor handling 401/403/5xx errors with appropriate logging and auto-redirect
- Response interceptor unwrapping response.data for cleaner API call results
- Error handler utility parsing backend ErrorDto into structured objects
- Helper functions for common error access patterns (getFieldError, getErrorMessage, etc.)

## Task Commits

Each task was committed atomically:

1. **Task 1: Implement axios instance with interceptors** - `5de3163` (feat)
2. **Task 2: Create error handler utility** - `3b32e22` (feat)

**Plan metadata:** (pending)

## Files Created/Modified

- `src/frontend/src/boot/axios.js` - Axios instance with interceptors and onLoadingChange subscriber
- `src/frontend/src/utils/errorHandler.js` - Error parsing utilities for backend ErrorDto

## Decisions Made

- Used empty string for baseURL since backend uses relative paths with Quasar dev proxy
- Auto-redirect to /login?expired=true on 401 with sessionExpired/unauthorized errorKey
- Unwrap response.data in interceptor so API services receive data directly
- Use subscriber pattern (onLoadingChange) instead of global state for loading

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Axios foundation complete with interceptors and error handling
- Ready for API services to use the configured axios instance
- Ready for composables to subscribe to loading state changes
- Error parser ready for useErrorHandler composable in Phase 3
- No blockers for next phase

---
*Phase: 01-foundation*
*Completed: 2026-01-17*
