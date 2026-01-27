---
phase: 09-backend-profile-api
plan: 01
subsystem: api
tags: [spring-boot, jakarta-validation, dto, profile-api]

# Dependency graph
requires:
  - phase: none
    provides: Base project with existing ChangeEmailDto pattern
provides:
  - Request DTOs for profile update endpoints (password, phone, address, info, 2FA)
  - Validation annotations for all profile fields
affects: [09-02, 09-03]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Class-based DTOs with Jakarta Bean Validation
    - @NotNull, @Size, @CamPhone annotations

key-files:
  created:
    - src/main/java/com/softropic/promora/security/api/dto/ChangePasswordRequestDto.java
    - src/main/java/com/softropic/promora/security/api/dto/ChangePhoneDto.java
    - src/main/java/com/softropic/promora/security/api/dto/AddressDto.java
    - src/main/java/com/softropic/promora/security/api/dto/UpdateUserInfoDto.java
    - src/main/java/com/softropic/promora/security/api/dto/Toggle2faDto.java
  modified: []

key-decisions:
  - "All DTOs class-based (not records) to match existing ChangeEmailDto pattern"
  - "Toggle2faDto requires password field for security verification"
  - "AddressDto uses @NotNull for required address fields (addressLine1, city, stateProvince, postalCode, country)"

patterns-established:
  - "DTO package location: security/api/dto/"
  - "Password validation: @NotNull @Size(min=5, max=100)"
  - "Phone validation: @NotNull @CamPhone"

# Metrics
duration: 4min
completed: 2026-01-27
---

# Phase 9 Plan 1: Request DTOs Summary

**5 profile update DTOs with Jakarta Bean Validation for password, phone, address, user info, and 2FA operations**

## Performance

- **Duration:** 4 min
- **Started:** 2026-01-27T13:03:26Z
- **Completed:** 2026-01-27T13:07:03Z
- **Tasks:** 2
- **Files created:** 5

## Accomplishments
- Created ChangePasswordRequestDto with currentPassword and newPassword validation
- Created ChangePhoneDto with @CamPhone Cameroon phone validation
- Created AddressDto matching Address entity constraints (9 fields)
- Created UpdateUserInfoDto with optional fields for user profile updates
- Created Toggle2faDto with enabled flag and password verification

## Task Commits

Each task was committed atomically:

1. **Task 1: Create password and phone DTOs** - `4a88d6c` (feat)
2. **Task 2: Create address, user info, and 2FA DTOs** - `98202aa` (feat)

## Files Created

- `src/main/java/com/softropic/promora/security/api/dto/ChangePasswordRequestDto.java` - Password change request with current/new password validation
- `src/main/java/com/softropic/promora/security/api/dto/ChangePhoneDto.java` - Phone update with @CamPhone validation
- `src/main/java/com/softropic/promora/security/api/dto/AddressDto.java` - Full address with 9 validated fields
- `src/main/java/com/softropic/promora/security/api/dto/UpdateUserInfoDto.java` - User info with firstName, lastName, langKey, nationalId, gender, title
- `src/main/java/com/softropic/promora/security/api/dto/Toggle2faDto.java` - 2FA toggle with enabled boolean and password verification

## Decisions Made

- **DTO package location:** Created new `security/api/dto/` subpackage to organize profile-related DTOs separately from existing DTOs in `security/api/`
- **Class-based DTOs:** All DTOs follow the class-based pattern (not Java records) matching existing ChangeEmailDto for consistency
- **Toggle2faDto password field:** Included password field for security verification when changing 2FA settings, matching the sensitive operation pattern from ChangeEmailDto

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- All request DTOs ready for use in service and controller layers
- Plan 09-02 can implement UserProfileService methods using these DTOs
- Plan 09-03 can implement AccountResource endpoints accepting these DTOs

---
*Phase: 09-backend-profile-api*
*Completed: 2026-01-27*
