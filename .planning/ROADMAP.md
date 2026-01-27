# Roadmap: Promora User Profile

## Overview

Implement user profile management for the Promora Vue.js 3 + Quasar 2 application. Starting with backend API endpoints, then email notifications and audit trail, followed by frontend profile page with update dialogs, and finally menu integration. Builds on v1.0.0 foundation (auth UI, session management, i18n).

## Milestones

- ✅ **v1.0.0 Frontend Security** — Phases 1-8 (shipped 2026-01-20)
- 🚧 **v1.1 User Profile** — Phases 9-12 (in progress)

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

<details>
<summary>✅ v1.0.0 Frontend Security (Phases 1-8) — SHIPPED 2026-01-20</summary>

- [x] **Phase 1: Foundation** — Axios interceptors, error utilities, i18n setup
- [x] **Phase 2: API Services** — Auth, account, and session API services
- [x] **Phase 3: Session Management** — Session plugin and composables
- [x] **Phase 4: Global Components** — Loading bar, session warning dialog
- [x] **Phase 5: Auth Pages** — Login, register, and OTP pages
- [x] **Phase 6: Account Pages** — Password reset and activation pages
- [x] **Phase 7: Router Integration** — Route definitions and auth guards
- [x] **Phase 8: Session Initialization** — Wire session monitoring after login

See `.planning/archive/v1/ROADMAP.md` for full details.

</details>

### 🚧 v1.1 User Profile (In Progress)

**Milestone Goal:** Enable authenticated users to view and manage their profile information with security notifications and audit trails.

- [x] **Phase 9: Backend Profile API** — Profile management API endpoints
- [x] **Phase 10: Notifications & Audit** — Email notifications and audit trail
- [ ] **Phase 11: Frontend Profile Page** — Profile display and update dialogs
- [ ] **Phase 12: Menu Integration** — Profile navigation and routing

## Phase Details

### Phase 9: Backend Profile API
**Goal**: Create profile management API endpoints for viewing and updating user information
**Depends on**: v1.0.0 Foundation (existing backend infrastructure)
**Requirements**: API-04, API-05, API-06, API-07, API-08, API-09, API-10
**Success Criteria** (what must be TRUE):
  1. GET /api/account/profile returns user profile data (email, phone, address, core info, 2FA status)
  2. PUT /api/account/email initiates email change with verification
  3. PUT /api/account/password updates password (requires current password)
  4. PUT /api/account/phone updates phone number
  5. PUT /api/account/address updates address fields
  6. PUT /api/account/info updates core user info
  7. PUT /api/account/2fa toggles 2FA (requires password)
**Research**: Unlikely — uses existing patterns
**Plans**: 4 (09-01 through 09-04)
**Completed**: 2026-01-27

### Phase 10: Notifications & Audit
**Goal**: Implement security notifications and audit trail for all profile changes
**Depends on**: Phase 9
**Requirements**: NOTF-01, NOTF-02, NOTF-03, AUDT-01, I18N-06, I18N-07
**Success Criteria** (what must be TRUE):
  1. AccountChangeEvent listener processes PASSWORD_CHANGED, EMAIL_CHANGED, ADDRESS_CHANGED, TWO_FACTOR_AUTH_* events
  2. Email notification sent to user for every profile change
  3. Generic Thymeleaf template renders profile change notifications
  4. Email template supports English and French
  5. All profile changes recorded in audit trail via TrailService
**Research**: Unlikely — uses existing AccountChangeEvent and TrailService patterns
**Plans**: 2 (10-01 through 10-02)
**Completed**: 2026-01-27

### Phase 11: Frontend Profile Page
**Goal**: Create profile page with display and independent update dialogs
**Depends on**: Phase 9
**Requirements**: PROF-01, UPDT-01, UPDT-02, UPDT-03, UPDT-04, UPDT-05, UPDT-06, I18N-04, I18N-05
**Success Criteria** (what must be TRUE):
  1. User can view profile page showing email, masked password (XXXX), phone, address, core info, 2FA status
  2. User can open dialog to update email (with current password and new email verification)
  3. User can open dialog to update password (current + new password)
  4. User can open dialog to update phone number
  5. User can open dialog to update address fields
  6. User can open dialog to update core info (firstName, lastName, nationalId, gender, title, langKey)
  7. User can toggle 2FA (with password confirmation)
  8. All text displays in English or French based on locale
**Research**: Unlikely — follows existing page patterns from v1.0.0
**Plans**: TBD

### Phase 12: Menu Integration
**Goal**: Add profile navigation to menu and wire router
**Depends on**: Phase 11
**Requirements**: PROF-02
**Success Criteria** (what must be TRUE):
  1. User can access profile page via menu link in navigation bar
  2. Profile route defined with requiresAuth: true meta flag
**Research**: Unlikely — follows existing router patterns
**Plans**: TBD

## Progress

**Execution Order:**
Phases execute in numeric order: 9 → 10 → 11 → 12

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 9. Backend Profile API | 4/4 | ✓ Complete | 2026-01-27 |
| 10. Notifications & Audit | 2/2 | ✓ Complete | 2026-01-27 |
| 11. Frontend Profile Page | 0/TBD | Not started | — |
| 12. Menu Integration | 0/TBD | Not started | — |

---

*Roadmap created: 2026-01-27*
*v1.0.0 shipped: 2026-01-20*
