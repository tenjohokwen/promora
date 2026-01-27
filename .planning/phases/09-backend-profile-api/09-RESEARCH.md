# Phase 9: Backend Profile API - Research

**Researched:** 2026-01-27
**Domain:** Spring Boot REST API for user profile management
**Confidence:** HIGH

## Summary

The codebase has substantial existing infrastructure for profile management. Key findings:

1. **UserProfileService exists with methods for user info, postal address, and email updates** - This service already handles 3 of the 6 update operations needed. It uses `@PreAuthorize(SecurityConstants.HAS_ANY_ROLE)` for security and follows the pattern of returning `Optional<User>`.

2. **AccountResource provides the REST API patterns** - The existing controller at `/v1/account` demonstrates all patterns needed: `@RestController`, `@Timed`, validation via `@Valid @RequestBody`, response types (`Success`, `Response`, `ResponseEntity<UserDto>`), and error handling.

3. **AccountChangeEvent exists but is not yet consumed** - The event class is defined with actions (PASSWORD_CHANGED, EMAIL_CHANGED, ADDRESS_CHANGED, TWO_FACTOR_AUTH_ENABLED/DISABLED) but no listener processes it. This will be implemented in Phase 10.

4. **DTOs and mappers are in place** - UserDto, UserMapper (MapStruct), ChangeEmailDto already exist. New DTOs needed for password change, phone, address, info, and 2FA toggle.

**Primary recommendation:** Extend existing patterns - add new endpoints to AccountResource, add missing service methods to UserProfileService, create focused DTOs for each update operation.

## Existing Infrastructure

### Services Already Available

| Service | Method | Purpose | Reuse |
|---------|--------|---------|-------|
| `UserProfileService` | `updateUserInformation(firstname, lastname, langKey, nationalId, gender, title)` | Updates core user info | Direct reuse |
| `UserProfileService` | `updatePostalAddress(Address)` | Updates user address | Direct reuse |
| `UserProfileService` | `updateUserEmail(oldEmail, newEmail, password)` | Updates email with password verification | Direct reuse |
| `UserService` | `getUserWithAuthorities()` | Gets current user with authorities | Direct reuse for GET profile |
| `PasswordResetService` | `isPasswordMatch(password, encodedPassword)` | Verifies password | Reuse for password/2FA changes |

### DTOs Already Available

| DTO | Location | Purpose |
|-----|----------|---------|
| `UserDto` | `security.exposed.UserDto` | Full user data (used by UserMapper) |
| `ChangeEmailDto` | `security.api.ChangeEmailDto` | Email change request (oldEmail, newEmail, password) |
| `KeyAndPasswordDto` | `security.api.KeyAndPasswordDto` | Password reset completion (record) |

### Controller Patterns (from AccountResource)

```java
// Base path
@RestController
@RequestMapping("/v1/account")

// GET endpoint returning ResponseEntity
@GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
@Timed
public ResponseEntity<UserDto> getAccount() {
    return Optional.ofNullable(userService.getUserWithAuthorities())
        .map(user -> new ResponseEntity<>(userMapper.toUserDto(user), HttpStatus.OK))
        .orElse(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
}

// POST endpoint returning Success
@PostMapping(value = "/change_email", consumes = MediaType.APPLICATION_JSON_VALUE)
@Timed
public Response changeEmail(@Valid @RequestBody ChangeEmailDto changeEmailDto) {
    // ... returns Success or Failure
}
```

### Security Patterns

```java
// Service-level security via @PreAuthorize
@PreAuthorize(SecurityConstants.HAS_ANY_ROLE)  // "hasRole('ROLE_ADMIN') or hasRole('ROLE_LTD_ADMIN') or hasRole('ROLE_USER')"
public Optional<User> updateUserInformation(...) { ... }

// Current user access
securityUtil.getCurrentUser().getUsername()  // Gets logged-in user's username
userRepository.findOneByLogin(username)       // Fetches user by login
```

### Response Types

| Type | Usage | Structure |
|------|-------|-----------|
| `Success` | Successful mutation | `(helpCode, msgKey, msg, payload)` |
| `Failure` | Failed mutation | `(helpCode, msgKey, msg)` |
| `Response` | Interface for both | `helpCode()` |
| `ErrorDto` | Validation/error response | From ApiAdvice exception handlers |

### Domain Entities

**User** extends **Customer** (MappedSuperclass):
- Customer fields: firstName, lastName, title, gender, dateOfBirth, langKey, phone (PhoneNumber), email, addresses (Set<Address>), nationalId
- User fields: login, loginIdType, password, activated, locked, otpEnabled, authorities, etc.

**Address** (@Embeddable):
- name (required, identifies address type like HOME, WORK)
- companyName, addressLine1, addressLine2, addressLine3
- city, stateProvince, postalCode, country

**PhoneNumber** (@Embeddable):
- phone, iso2Country, phoneType (MOBILE/FIXED), provider

### Password Handling

```java
// In UserProfileService - password verification
passwordEncoder.matches(password, user.getPassword())

// In PasswordResetService - encoding new password
passwordEncoder.encode(newPassword)

// In User domain - setting password
user.setPassword(passwordEncoder.encode(newPassword));
```

### Event Publishing Pattern

```java
// In AccountManagementFacade
private final ApplicationEventPublisher publisher;

// Publishing email envelope event
publisher.publishEvent(envelope);

// AccountChangeEvent structure (ready for Phase 10)
new AccountChangeEvent(Action.PASSWORD_CHANGED, oldValue, newValue, recipient)
```

## Gaps to Fill

### New Endpoints Needed

| Endpoint | Method | Purpose | DTO Needed | Service Method |
|----------|--------|---------|------------|----------------|
| `/profile` | GET | Get user profile data | UserDto (existing) | UserService.getUserWithAuthorities() |
| `/email` | PUT | Update email | ChangeEmailDto (existing) | UserProfileService.updateUserEmail() |
| `/password` | PUT | Update password | **NEW: ChangePasswordRequestDto** | **NEW: changePassword()** |
| `/phone` | PUT | Update phone | **NEW: ChangePhoneDto** | **NEW: updatePhone()** |
| `/address` | PUT | Update address | **NEW: AddressDto** | UserProfileService.updatePostalAddress() |
| `/info` | PUT | Update core info | **NEW: UpdateUserInfoDto** | UserProfileService.updateUserInformation() |
| `/2fa` | PUT | Toggle 2FA | **NEW: Toggle2faDto** | **NEW: toggle2fa()** |

### New DTOs Required

1. **ChangePasswordRequestDto** (for logged-in password change)
   - currentPassword (required)
   - newPassword (required, min 5, max 100)

2. **ChangePhoneDto**
   - phone (required, @CamPhone validation)

3. **AddressDto**
   - name (required, min 4, max 50)
   - companyName (optional)
   - addressLine1 (required), addressLine2, addressLine3
   - city, stateProvince, postalCode, country (all required)

4. **UpdateUserInfoDto**
   - firstName, lastName (max 50)
   - title (max 25)
   - gender (MALE, FEMALE, OTHER)
   - nationalId (min 5, max 50)
   - langKey (min 2, max 5)

5. **Toggle2faDto**
   - enabled (boolean)
   - password (required for verification)

### New Service Methods Required

1. **UserProfileService.changePassword(currentPassword, newPassword)**
   - Verify current password matches
   - Encode and set new password
   - Return updated user

2. **UserProfileService.updatePhone(phone)**
   - Convert phone string to PhoneNumber using CamMobileValidator
   - Set on user
   - Return updated user

3. **UserProfileService.toggle2fa(enabled, password)**
   - Verify password
   - Set otpEnabled flag
   - Return updated user

### Mappers Needed

**AddressMapper** (MapStruct):
- AddressDto -> Address conversion

Alternatively, manual mapping in service/controller since Address is simple.

## Implementation Approach

### Recommended Endpoint Base Path

Change from `/v1/account` to `/api/account` to match the requirements specification. The existing `/v1/account` path can remain for backward compatibility with existing endpoints.

**Option A (Recommended):** Add new endpoints to existing AccountResource with `/profile`, `/password`, `/phone`, `/address`, `/info`, `/2fa` paths.

**Option B:** Create new ProfileResource class. Not recommended - adds complexity without benefit.

### Implementation Order

1. **DTOs first** - Create all request DTOs with validation annotations
2. **Service methods** - Add missing methods to UserProfileService
3. **Controller endpoints** - Add endpoints to AccountResource
4. **Testing** - Add integration tests

### Controller Implementation Pattern

```java
@PutMapping(value = "/password", consumes = MediaType.APPLICATION_JSON_VALUE)
@Timed
public Success changePassword(@Valid @RequestBody ChangePasswordRequestDto dto) {
    userProfileService.changePassword(dto.getCurrentPassword(), dto.getNewPassword())
        .orElseThrow(() -> new SecException("Password change failed",
            Map.of(), SecurityError.EMAIL_OR_PW_MISMATCH));
    return new Success(null, "password.changed", "Password updated successfully", Map.of());
}
```

### Service Implementation Pattern

```java
@PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
public Optional<User> changePassword(String currentPassword, String newPassword) {
    return userRepository.findOneByLogin(securityUtil.getCurrentUser().getUsername())
        .filter(user -> passwordEncoder.matches(currentPassword, user.getPassword()))
        .map(user -> {
            user.setPassword(passwordEncoder.encode(newPassword));
            log.debug("Changed password for User: {}", user.getLogin());
            return user;
        });
}
```

### Security Considerations

| Operation | Verification Required | Rationale |
|-----------|----------------------|-----------|
| GET profile | Authentication only | Read operation |
| PUT email | Current password | Sensitive, could lock out user |
| PUT password | Current password | Sensitive, requires proof of identity |
| PUT phone | Authentication only | Not critical, recoverable |
| PUT address | Authentication only | Not critical |
| PUT info | Authentication only | Not critical |
| PUT 2fa | Current password | Sensitive security setting |

### Validation Annotations to Use

```java
// Email validation (existing pattern)
@Email(regexp = "[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,3}", flags = Pattern.Flag.CASE_INSENSITIVE)
@Size(max = 100)

// Phone validation (existing)
@CamPhone

// Password validation
@Size(min = 5, max = 100)

// Name fields
@Size(max = 50)

// Required fields
@NotNull or @NotBlank
```

## Key Files

### Files to Read

| File | Purpose |
|------|---------|
| `security/api/AccountResource.java` | Extend with new endpoints |
| `security/service/UserProfileService.java` | Add missing service methods |
| `security/service/PasswordResetService.java` | Reference for password encoding |
| `security/domain/User.java` | User entity fields |
| `security/domain/Address.java` | Address entity fields |
| `security/core/mapper/UserMapper.java` | MapStruct mapper pattern |
| `security/exposed/UserDto.java` | Existing DTO pattern |
| `security/api/ChangeEmailDto.java` | DTO pattern |
| `security/ApiAdvice.java` | Exception handling patterns |
| `common/message/Success.java` | Response record |
| `common/message/Failure.java` | Response record |
| `security/common/util/SecurityConstants.java` | Security expressions |

### Files to Create

| File | Purpose |
|------|---------|
| `security/api/dto/ChangePasswordRequestDto.java` | Password change request |
| `security/api/dto/ChangePhoneDto.java` | Phone update request |
| `security/api/dto/AddressDto.java` | Address update request |
| `security/api/dto/UpdateUserInfoDto.java` | User info update request |
| `security/api/dto/Toggle2faDto.java` | 2FA toggle request |

### Files to Modify

| File | Changes |
|------|---------|
| `security/api/AccountResource.java` | Add 7 new endpoints |
| `security/service/UserProfileService.java` | Add changePassword, updatePhone, toggle2fa methods |

## Common Pitfalls

### Pitfall 1: Phone Number Parsing

**What goes wrong:** Phone string not converted to PhoneNumber embedded entity correctly
**Why it happens:** CamMobileValidator returns PhoneNumberDto, not PhoneNumber entity
**How to avoid:** Follow existing pattern in AccountManagementFacade.toPhoneNumber()
```java
final PhoneNumberDto phoneNoDto = CamMobileValidator.validate(phone);
phoneNumber.setPhone(phoneNoDto.getPhone());
phoneNumber.setIso2Country(phoneNoDto.getIso2Country());
// etc.
```

### Pitfall 2: Address Name Uniqueness

**What goes wrong:** Duplicate addresses created instead of replacing existing
**Why it happens:** Address equality is based on `name` field
**How to avoid:** Use existing `user.addOrReplaceAddress(address)` method which removes existing address with same name before adding new one

### Pitfall 3: Transactional Boundaries

**What goes wrong:** Changes not persisted or lazy loading exceptions
**Why it happens:** Repository operations outside transaction
**How to avoid:** Service methods already have `@Transactional` at class level via `UserProfileService`

### Pitfall 4: Password Exposure

**What goes wrong:** Password hash returned in response
**Why it happens:** UserDto includes password field
**How to avoid:** UserMapper already maps user to UserDto. Verify password is not included in response. Consider using `@JsonIgnore` if needed.

### Pitfall 5: Email Change Without Verification

**What goes wrong:** Email changed immediately without user proving access to new email
**Why it happens:** Misunderstanding of requirement
**How to avoid:** The existing `updateUserEmail` in UserProfileService changes email immediately. For verification flow, this is handled by AccountManagementFacade.changeEmail() which sends a verification email. Follow the existing pattern.

## Code Examples

### DTO Pattern (from existing ChangeEmailDto)

```java
package com.softropic.promora.security.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ChangeEmailDto {
    @NotNull
    @Email(regexp = "[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,3}",
           flags = Pattern.Flag.CASE_INSENSITIVE)
    @Size(max = 100)
    private String oldEmail;

    @NotNull
    @Email(regexp = "[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,3}",
           flags = Pattern.Flag.CASE_INSENSITIVE)
    @Size(max = 100)
    private String newEmail;

    @Size(min = 4, max = 60)
    private String password;

    // Constructors, getters, setters
}
```

### Service Method Pattern (from UserProfileService)

```java
@PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
public Optional<User> updatePostalAddress(Address address) {
    return userRepository.findOneByLogin(securityUtil.getCurrentUser().getUsername())
            .map(u -> {
                u.addOrReplaceAddress(address);
                return u;
            });
}
```

### Controller Endpoint Pattern (from AccountResource)

```java
@PostMapping(value = "/change_email", consumes = MediaType.APPLICATION_JSON_VALUE)
@Timed
public Response changeEmail(@Valid @RequestBody ChangeEmailDto changeEmailDto) {
    final String code = accountManagementFacade.changeEmail(
        changeEmailDto.getOldEmail(),
        changeEmailDto.getNewEmail(),
        changeEmailDto.getPassword());
    if(StringUtils.isNotBlank(code)) {
        return new Success(code,
                          "email.updated",
                          "Your email address has been updated",
                          Map.of());
    }
    return new Failure(UUID.randomUUID().toString(),
                       "email.change.failure",
                       "Your email cannot be changed");
}
```

## State of the Art

| Pattern | Current Implementation | Notes |
|---------|----------------------|-------|
| REST Controllers | @RestController with @RequestMapping | Standard Spring pattern |
| Validation | Jakarta Bean Validation (JSR-380) | @Valid, @NotNull, @Size, etc. |
| Security | @PreAuthorize with SpEL expressions | Method-level security |
| Response Format | Custom Success/Failure records | Consistent API responses |
| Exception Handling | @RestControllerAdvice (ApiAdvice) | Centralized error handling |
| Mapping | MapStruct | Type-safe entity <-> DTO conversion |
| Transactions | @Transactional at class level | Spring declarative transactions |
| Metrics | @Timed from Micrometer | Endpoint timing metrics |

## Open Questions

1. **GET /api/account/profile vs GET /v1/account/**
   - Existing GET / returns UserDto already
   - Recommendation: Either use existing endpoint or add /profile alias
   - Decision needed: Keep /api path prefix for new endpoints?

2. **Address Update - Single vs Multiple**
   - Current: User has Set<Address> but v1.1 scope says "single address"
   - Recommendation: For /address endpoint, assume name="PRIMARY" or "HOME"
   - Decision needed: Fixed address name or allow user to specify?

## Sources

### Primary (HIGH confidence)
- Codebase files (direct inspection)
- `UserProfileService.java` - existing service methods
- `AccountResource.java` - existing REST patterns
- `AccountChangeEvent.java` - event structure

### Secondary (MEDIUM confidence)
- Spring Boot/Spring Security patterns from training data

## Metadata

**Confidence breakdown:**
- Existing Infrastructure: HIGH - Direct codebase inspection
- Gaps to Fill: HIGH - Clear from requirements and existing code
- Implementation Approach: HIGH - Follows existing patterns
- Pitfalls: MEDIUM - Based on common Spring patterns and codebase specifics

**Research date:** 2026-01-27
**Valid until:** 2026-02-27 (30 days - stable backend patterns)
