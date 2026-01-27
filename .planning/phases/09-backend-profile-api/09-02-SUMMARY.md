---
phase: 09-backend-profile-api
plan: 02
subsystem: api
tags: [spring-boot, user-profile, password, phone, 2fa, service-layer]

# Dependency graph
requires:
  - phase: 09-backend-profile-api
    provides: Request DTOs (Plan 01)
provides:
  - UserProfileService.changePassword() method with password verification
  - UserProfileService.updatePhone() method with CamMobileValidator
  - UserProfileService.toggle2fa() method with password verification
affects: [09-03]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Password change with current password verification
    - Phone conversion using CamMobileValidator.validate()
    - 2FA toggle requiring password for security

key-files:
  created: []
  modified:
    - src/main/java/com/softropic/promora/security/service/UserProfileService.java

key-decisions:
  - "Password change throws SecException on mismatch (not empty Optional)"
  - "2FA toggle requires password verification for security-sensitive operation"
  - "Phone conversion uses private toPhoneNumber() helper matching AccountManagementFacade pattern"

patterns-established:
  - "Service method security: @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)"
  - "Password verification pattern: passwordEncoder.matches() with SecException on failure"
  - "Phone conversion: CamMobileValidator.validate() -> PhoneNumber entity"

# Metrics
duration: 5min
completed: 2026-01-27
---

# Phase 9 Plan 2: Service Methods Summary

**3 new UserProfileService methods for password change, phone update, and 2FA toggle with proper security verification**

## Performance

- **Duration:** 5 min
- **Started:** 2026-01-27T13:15:00Z
- **Completed:** 2026-01-27T13:20:00Z
- **Tasks:** 2
- **Files modified:** 1

## Accomplishments

- Added changePassword() method with current password verification and new password encoding
- Added updatePhone() method converting phone string to PhoneNumber entity via CamMobileValidator
- Added toggle2fa() method with password verification before changing otpEnabled flag
- Added private toPhoneNumber() helper following AccountManagementFacade pattern
- All methods use @PreAuthorize(SecurityConstants.HAS_ANY_ROLE) for security
- UserProfileService now has 6 public methods (3 existing + 3 new)

## Task Commits

Each task was committed atomically:

1. **Task 1: Add changePassword and updatePhone methods** - `82c245d` (feat)
2. **Task 2: Add toggle2fa method** - `083af83` (feat)

## Files Modified

- `src/main/java/com/softropic/promora/security/service/UserProfileService.java` - Added 3 new public methods (changePassword, updatePhone, toggle2fa) and 1 private helper (toPhoneNumber)

## Decisions Made

- **Password change security:** changePassword() throws SecException when current password doesn't match, following the same pattern as updateUserEmail() rather than returning empty Optional
- **2FA toggle security:** toggle2fa() requires password verification since changing 2FA settings is a sensitive operation (consistent with password and email changes)
- **Phone conversion:** Created private toPhoneNumber() helper in UserProfileService rather than reusing AccountManagementFacade.toPhoneNumber() which is protected - keeps service self-contained

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- All service methods ready for Plan 09-03 controller endpoints
- changePassword, updatePhone, toggle2fa can be called from AccountResource
- All methods follow established @PreAuthorize and Optional<User> patterns

---
*Phase: 09-backend-profile-api*
*Completed: 2026-01-27*
