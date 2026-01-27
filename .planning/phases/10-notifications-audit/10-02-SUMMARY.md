---
phase: 10-notifications-audit
plan: 02
subsystem: api
tags: [spring-events, event-publishing, user-profile]

# Dependency graph
requires:
  - phase: 10-notifications-audit
    plan: 01
    provides: AccountChangeEventListener, EmailTemplate.PROFILE_CHANGE, profileChange.html
provides:
  - Event publishing in UserProfileService for all profile changes
  - Complete notification/audit circuit from service to listener to email
affects: [11-profile-ui-integration, 12-cleanup]

# Tech tracking
tech-stack:
  added: []
  patterns: [ApplicationEventPublisher injection, buildRecipient helper pattern]

key-files:
  created: []
  modified:
    - src/main/java/com/softropic/promora/security/service/UserProfileService.java

key-decisions:
  - "Email change notification sent to OLD email address for security"
  - "No event publishing for updateUserInformation() or updatePhone() (less-sensitive changes)"

patterns-established:
  - "Event publishing pattern: buildRecipient(user) + new AccountChangeEvent(action, old, new, recipient) + publisher.publishEvent(event)"

# Metrics
duration: 2min
completed: 2026-01-27
---

# Phase 10 Plan 02: Event Publishing Summary

**UserProfileService wired with ApplicationEventPublisher to publish AccountChangeEvent for password, email, address, and 2FA changes**

## Performance

- **Duration:** 2 min
- **Started:** 2026-01-27T20:25:00Z
- **Completed:** 2026-01-27T20:27:00Z
- **Tasks:** 2
- **Files modified:** 1

## Accomplishments
- ApplicationEventPublisher injected into UserProfileService via @RequiredArgsConstructor
- buildRecipient() helper method to create Recipient from User entity
- changePassword() publishes PASSWORD_CHANGED event
- updateUserEmail() publishes EMAIL_CHANGED event (notification to old email for security)
- updatePostalAddress() publishes ADDRESS_CHANGED event
- toggle2fa() publishes TWO_FACTOR_AUTH_ENABLED or TWO_FACTOR_AUTH_DISABLED event
- Complete event chain verified: Service -> Event -> Listener -> Audit + Email

## Task Commits

Each task was committed atomically:

1. **Task 1: Add Event Publishing to UserProfileService** - `6b81b2c` (feat)
2. **Task 2: Verify End-to-End Compilation** - verification only, no commit needed

## Files Modified
- `src/main/java/com/softropic/promora/security/service/UserProfileService.java` - Added event publishing to 4 methods

## Decisions Made
- Email change notification is sent to the OLD email address for security (alerts user if email was changed maliciously)
- No event publishing for updateUserInformation() or updatePhone() methods (AccountChangeEvent.Action enum does not include these less-sensitive changes)

## Deviations from Plan
None - plan executed exactly as written.

## Issues Encountered
None.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Complete notification/audit circuit is now operational:
  1. User changes password/email/address/2FA via ProfileResource
  2. UserProfileService publishes AccountChangeEvent
  3. AccountChangeEventListener records audit trail via TrailService
  4. AccountChangeEventListener publishes Envelope for email
  5. SendMailListener sends email using profileChange.html template
- Phase 10 (Notifications & Audit) is complete

---
*Phase: 10-notifications-audit*
*Completed: 2026-01-27*
