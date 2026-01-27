# Requirements: Promora User Profile

**Defined:** 2026-01-27
**Core Value:** Secure, user-friendly account management with proper security notifications for all sensitive changes.

## v1.1 Requirements

Requirements for user profile milestone. Each maps to roadmap phases.

### Profile Display (PROF)

- [ ] **PROF-01**: User can view profile page showing email, masked password, phone, address, core info, 2FA status
- [ ] **PROF-02**: User can access profile page via menu link in navigation bar

### Profile Updates (UPDT)

- [ ] **UPDT-01**: User can update email address (requires verification of new email)
- [ ] **UPDT-02**: User can update password (requires current password)
- [ ] **UPDT-03**: User can update phone number
- [ ] **UPDT-04**: User can update address (name, companyName, addressLine1-3, city, stateProvince, postalCode, country)
- [ ] **UPDT-05**: User can update core info (firstName, lastName, nationalId, gender, title, langKey)
- [ ] **UPDT-06**: User can toggle 2FA (requires password verification)

### Backend API (API)

- [x] **API-04**: GET /api/account/profile endpoint returns user profile data
- [x] **API-05**: PUT /api/account/email endpoint updates email with verification
- [x] **API-06**: PUT /api/account/password endpoint updates password
- [x] **API-07**: PUT /api/account/phone endpoint updates phone number
- [x] **API-08**: PUT /api/account/address endpoint updates address
- [x] **API-09**: PUT /api/account/info endpoint updates core user info
- [x] **API-10**: PUT /api/account/2fa endpoint toggles two-factor authentication

### Notifications (NOTF)

- [x] **NOTF-01**: AccountChangeEvent listener processes profile change events
- [x] **NOTF-02**: Email notification sent for all profile changes
- [x] **NOTF-03**: Generic Thymeleaf email template for profile change notifications

### Audit (AUDT)

- [x] **AUDT-01**: All profile changes recorded in audit trail via TrailService

### Internationalization (I18N)

- [ ] **I18N-04**: English (en-US) translations for profile page and update dialogs
- [ ] **I18N-05**: French (fr-FR) translations for profile page and update dialogs
- [x] **I18N-06**: English (en-US) translations for profile change email template
- [x] **I18N-07**: French (fr-FR) translations for profile change email template

## v2 Requirements

Deferred to future release. Tracked but not in current roadmap.

### Profile Enhancements

- **PROF-03**: User can upload and display profile photo
- **PROF-04**: User can manage multiple addresses (HOME, WORK, etc.)

### Account Management

- **ACCT-01**: User can delete their account (with confirmation flow)
- **ACCT-02**: User can export their account data

## Out of Scope

Explicitly excluded. Documented to prevent scope creep.

| Feature | Reason |
|---------|--------|
| Multiple addresses per user | Single address sufficient for v1.1 |
| Profile photo upload | Deferred to v2 |
| Account deletion | Requires additional security considerations |
| Account data export | GDPR feature, deferred to v2 |

## Traceability

Which phases cover which requirements. Updated by create-roadmap.

| Requirement | Phase | Status |
|-------------|-------|--------|
| PROF-01 | Phase 11 | Pending |
| PROF-02 | Phase 12 | Pending |
| UPDT-01 | Phase 11 | Pending |
| UPDT-02 | Phase 11 | Pending |
| UPDT-03 | Phase 11 | Pending |
| UPDT-04 | Phase 11 | Pending |
| UPDT-05 | Phase 11 | Pending |
| UPDT-06 | Phase 11 | Pending |
| API-04 | Phase 9 | Complete |
| API-05 | Phase 9 | Complete |
| API-06 | Phase 9 | Complete |
| API-07 | Phase 9 | Complete |
| API-08 | Phase 9 | Complete |
| API-09 | Phase 9 | Complete |
| API-10 | Phase 9 | Complete |
| NOTF-01 | Phase 10 | Complete |
| NOTF-02 | Phase 10 | Complete |
| NOTF-03 | Phase 10 | Complete |
| AUDT-01 | Phase 10 | Complete |
| I18N-04 | Phase 11 | Pending |
| I18N-05 | Phase 11 | Pending |
| I18N-06 | Phase 10 | Complete |
| I18N-07 | Phase 10 | Complete |

**Coverage:**
- v1.1 requirements: 23 total
- Mapped to phases: 23
- Unmapped: 0 ✓

---

*Requirements defined: 2026-01-27*
*Last updated: 2026-01-27 after v1.1 milestone start*
