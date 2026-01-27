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

- [ ] **API-04**: GET /api/account/profile endpoint returns user profile data
- [ ] **API-05**: PUT /api/account/email endpoint updates email with verification
- [ ] **API-06**: PUT /api/account/password endpoint updates password
- [ ] **API-07**: PUT /api/account/phone endpoint updates phone number
- [ ] **API-08**: PUT /api/account/address endpoint updates address
- [ ] **API-09**: PUT /api/account/info endpoint updates core user info
- [ ] **API-10**: PUT /api/account/2fa endpoint toggles two-factor authentication

### Notifications (NOTF)

- [ ] **NOTF-01**: AccountChangeEvent listener processes profile change events
- [ ] **NOTF-02**: Email notification sent for all profile changes
- [ ] **NOTF-03**: Generic Thymeleaf email template for profile change notifications

### Audit (AUDT)

- [ ] **AUDT-01**: All profile changes recorded in audit trail via TrailService

### Internationalization (I18N)

- [ ] **I18N-04**: English (en-US) translations for profile page and update dialogs
- [ ] **I18N-05**: French (fr-FR) translations for profile page and update dialogs
- [ ] **I18N-06**: English (en-US) translations for profile change email template
- [ ] **I18N-07**: French (fr-FR) translations for profile change email template

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
| PROF-01 | TBD | Pending |
| PROF-02 | TBD | Pending |
| UPDT-01 | TBD | Pending |
| UPDT-02 | TBD | Pending |
| UPDT-03 | TBD | Pending |
| UPDT-04 | TBD | Pending |
| UPDT-05 | TBD | Pending |
| UPDT-06 | TBD | Pending |
| API-04 | TBD | Pending |
| API-05 | TBD | Pending |
| API-06 | TBD | Pending |
| API-07 | TBD | Pending |
| API-08 | TBD | Pending |
| API-09 | TBD | Pending |
| API-10 | TBD | Pending |
| NOTF-01 | TBD | Pending |
| NOTF-02 | TBD | Pending |
| NOTF-03 | TBD | Pending |
| AUDT-01 | TBD | Pending |
| I18N-04 | TBD | Pending |
| I18N-05 | TBD | Pending |
| I18N-06 | TBD | Pending |
| I18N-07 | TBD | Pending |

**Coverage:**
- v1.1 requirements: 23 total
- Mapped to phases: 0 (run /gsd:create-roadmap)
- Unmapped: 23

---

*Requirements defined: 2026-01-27*
*Last updated: 2026-01-27 after v1.1 milestone start*
