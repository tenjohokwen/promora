---
phase: 11-frontend-profile-page
plan: 05
subsystem: ui, backend
tags: [vue3, quasar, validation, mapstruct, gap-closure]

# Dependency graph
requires:
  - phase: 11-04
    provides: All 6 profile dialogs wired to ProfilePage
  - phase: v1.1-UAT
    provides: Gap identification (4 issues)
provides:
  - Help code display in all dialog error banners
  - Same-value validation in email, password, phone dialogs
  - Address field in profile API response (UserDto + UserMapper)
affects: [v1.1-UAT]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Same-value validation: compare new input against current prop/form value"
    - "MapStruct @AfterMapping for extracting first element from Set to singular field"

key-files:
  created: []
  modified:
    - src/frontend/src/components/profile/UpdateEmailDialog.vue
    - src/frontend/src/components/profile/UpdatePasswordDialog.vue
    - src/frontend/src/components/profile/UpdatePhoneDialog.vue
    - src/frontend/src/i18n/en-US/index.js
    - src/frontend/src/i18n/fr-FR/index.js
    - src/main/java/com/softropic/promora/security/exposed/UserDto.java
    - src/main/java/com/softropic/promora/security/core/mapper/UserMapper.java

key-decisions:
  - "Address mapped as singular AddressDto (first from Set) matching frontend expectation"
  - "Phone same-value comparison strips non-digit characters before comparing"
  - "originalPhone ref tracks value at dialog open (not reactive to prop changes during edit)"

patterns-established:
  - "Gap closure plan pattern: UAT issues drive focused fix plans"

# Metrics
duration: 8min
completed: 2026-01-29
---

# Phase 11 Plan 05: UAT Gap Closure Summary

**Closed 4 UAT gaps: help code display in error banners, same-value validation for email/password/phone, and address data in profile API response**

## Performance

- **Duration:** 8 min (across multiple sessions due to rate limits)
- **Completed:** 2026-01-29
- **Tasks:** 3
- **Files modified:** 7

## Accomplishments
- All 6 profile dialogs now display help code in error banners (Task 1, committed d7bff83)
- UpdateEmailDialog rejects submission when newEmail equals currentEmail
- UpdatePasswordDialog rejects submission when newPassword equals currentPassword
- UpdatePhoneDialog rejects submission when phone equals currentPhone (digit-normalized comparison)
- Added sameEmail, samePassword, samePhone i18n messages in English and French
- Added AddressDto address field to UserDto with getter/setter
- Added @AfterMapping in UserMapper to extract first address from User's addresses Set
- Profile API now returns address data, fixing "Not provided" display after address update

## Task Commits

1. **Task 1: Help code display** - `d7bff83` (fix) - Add help code to error banners in all dialogs
2. **Task 2: Same-value validation** - `0f29f1f` (feat) - Add same-value validation to email, password, phone dialogs
3. **Task 3: Address display fix** - `acbb9e3` (fix) - Include address in profile API response
4. **Styling tweaks** - `bee7683` (style) - Minor dialog layout adjustments

## Files Modified
- `UpdateEmailDialog.vue` - Added notSameEmail validation rule
- `UpdatePasswordDialog.vue` - Added notSamePassword validation rule
- `UpdatePhoneDialog.vue` - Added originalPhone ref and notSamePhone validation rule
- `en-US/index.js` - Added sameEmail, samePassword, samePhone messages
- `fr-FR/index.js` - Added French equivalents for same-value messages
- `UserDto.java` - Added AddressDto address field with getter/setter
- `UserMapper.java` - Added @AfterMapping mapAddress and addressToAddressDto method

## Gaps Closed (from v1.1-UAT.md)

| UAT Gap | Status |
|---------|--------|
| Help codes not displayed in error messages | Fixed (all 6 dialogs) |
| No validation for same old/new values | Fixed (email, password, phone) |
| Address not reflecting after first-time update | Fixed (backend API now returns address) |
| Profile reload doesn't show new address | Fixed (same root cause as above) |

## Root Cause Analysis (Address Bug)
- **Symptom:** Profile page showed "Not provided" after adding address for user with no prior address
- **Root cause:** `UserDto.java` had no `address` field; `UserMapper` had no address mapping
- **Fix:** Added `AddressDto address` to UserDto and `@AfterMapping` in UserMapper to extract first address from `Set<Address>`
- **Why it wasn't caught earlier:** Address update endpoint worked correctly (saving data), but the GET profile endpoint never returned address data

## Deviations from Plan
- Plan suggested debugging the frontend `loadProfile()` and `formattedAddress` computed first. Investigation revealed the root cause was entirely in the backend (missing DTO field and mapper).

## Issues Encountered
- Executor agent hit rate limit during initial run, requiring manual completion of Tasks 2 and 3.

---
*Phase: 11-frontend-profile-page*
*Plan: 05 (gap-closure)*
*Completed: 2026-01-29*
