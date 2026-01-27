# Phase 10: Notifications & Audit - Research

**Researched:** 2026-01-27
**Domain:** Spring Event Listeners, Thymeleaf Email Templates, Audit Trail
**Confidence:** HIGH

## Summary

Phase 10 implements security notifications and audit trail for profile changes. The codebase has comprehensive existing infrastructure that makes this phase straightforward:

1. **AccountChangeEvent already exists** - The event class at `security/audit/shared/event/AccountChangeEvent.java` defines all required actions (PASSWORD_CHANGED, EMAIL_CHANGED, ADDRESS_CHANGED, TWO_FACTOR_AUTH_ENABLED, TWO_FACTOR_AUTH_DISABLED) but currently has no listener consuming it.

2. **TrailService and audit infrastructure are ready** - The `TrailService` provides a simple `recordTrail(AuditTrail)` method that persists to `AuditLog` entity. The existing `SecurityAuditListener` demonstrates the exact pattern for creating `AuditTrail` objects with context metadata.

3. **Email sending infrastructure is mature** - `MailManager` handles async email delivery with retry logic, `MailService` processes Thymeleaf templates with i18n via `MessageSource`, and the `Envelope`/`Recipient` pattern is well established.

**Primary recommendation:** Create an `AccountChangeEventListener` that consumes `AccountChangeEvent`, sends a notification email using a new generic template, and records the change in the audit trail. Follow existing patterns from `SecurityAuditListener` and `SendMailListener`.

## Standard Stack

The established libraries/tools for this domain:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Events | 6.x (Spring Boot 3.x) | Event publishing/listening | Built-in, already used throughout codebase |
| Thymeleaf | 3.1.x | Email template rendering | Already configured via `ThymeleafConfiguration` |
| Spring MessageSource | 6.x | i18n for templates | Already configured, supports `messages_en.properties` and `messages_fr.properties` |
| MapStruct | 1.5.x | AuditTrail to AuditLog mapping | Already used via `AuditTrailMapper` |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Jakarta Mail | 2.x | Email sending | Already configured via `JavaMailSenderImpl` |
| Slf4j/Logback | 2.x | Logging | Standard logging, already used |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Spring Events | Kafka/RabbitMQ | Overkill for single-app scenario |
| Thymeleaf | Freemarker | Would require new configuration, Thymeleaf already set up |

**Installation:** No additional dependencies needed - all required libraries are already present.

## Architecture Patterns

### Recommended Project Structure
```
src/main/java/com/softropic/promora/security/
├── audit/
│   ├── listener/
│   │   ├── SecurityAuditListener.java     # Existing - handles AuthEvent, SecurityAlertEvent
│   │   └── AccountChangeEventListener.java # NEW - handles AccountChangeEvent
│   ├── service/
│   │   └── TrailService.java              # Existing - recordTrail()
│   └── shared/event/
│       └── AccountChangeEvent.java        # Existing - event with Action enum
└── listener/
    └── SendMailListener.java              # Existing - handles SendMailEvent

src/main/resources/
├── mails/
│   ├── activation.html                    # Existing template
│   ├── passwordReset.html                 # Existing template
│   └── profileChange.html                 # NEW - generic profile change template
└── i18n/
    ├── messages_en.properties             # Add profile change keys
    └── messages_fr.properties             # Add profile change keys
```

### Pattern 1: Event Listener with @EventListener
**What:** Spring-managed bean that listens to application events
**When to use:** Processing domain events asynchronously
**Example:**
```java
// Source: Existing SecurityAuditListener.java pattern
@Slf4j
@Component
public class AccountChangeEventListener {

    private final TrailService trailService;
    private final ApplicationEventPublisher publisher;

    public AccountChangeEventListener(TrailService trailService,
                                      ApplicationEventPublisher publisher) {
        this.trailService = trailService;
        this.publisher = publisher;
    }

    @EventListener
    public void handleAccountChange(AccountChangeEvent event) {
        // 1. Record audit trail
        recordAuditTrail(event);

        // 2. Send notification email
        sendNotificationEmail(event);
    }
}
```

### Pattern 2: Generic Thymeleaf Template with Action-Based Content
**What:** Single template that renders different content based on action type
**When to use:** Multiple similar notifications with shared layout
**Example:**
```html
<!-- Source: Based on existing activation.html pattern -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title th:text="#{email.profile_change.title}">Profile Change Notification</title>
</head>
<body>
    <p th:text="#{email.greeting(${recipient.firstname})}">Hello User,</p>

    <!-- Action-specific message using th:switch -->
    <div th:switch="${map.action}">
        <p th:case="'PASSWORD_CHANGED'" th:text="#{email.profile_change.password}">
            Your password was changed.
        </p>
        <p th:case="'EMAIL_CHANGED'" th:text="#{email.profile_change.email(${map.oldValue}, ${map.newValue})}">
            Your email was changed.
        </p>
        <!-- ... other cases ... -->
    </div>

    <p th:text="#{email.profile_change.not_you}">If this wasn't you, contact support.</p>

    <hr/>
    <p><small><span th:text="#{email.common.helpCode}">Help Code:</span> <span th:text="${map.helpCode}">CODE</span></small></p>
</body>
</html>
```

### Pattern 3: Audit Trail Recording
**What:** Persist security-relevant events to AuditLog table
**When to use:** Any profile change operation
**Example:**
```java
// Source: Based on SecurityAuditListener.java pattern
private void recordAuditTrail(AccountChangeEvent event) {
    final AuditTrail auditTrail = new AuditTrail();
    final RequestMetadata clientInfo = RequestMetadataProvider.getClientInfo();

    auditTrail.setEventTimestamp(ClockProvider.getClock().instant());
    auditTrail.setMsg("PROFILE_CHANGE: %s".formatted(event.getAction()));
    auditTrail.setLogin(event.getRecipient().getEmail());
    auditTrail.setAuthenticated(true); // Profile changes require auth
    auditTrail.setRelevantProperties(Map.of(
        "action", event.getAction().name(),
        "oldValue", event.getOldValue(),
        "newValue", event.getNewValue()
    ));
    auditTrail.setUserAgent(clientInfo.getUserAgent());
    auditTrail.setIpAddress(clientInfo.getIpAddress());
    auditTrail.setClientId(clientInfo.getClientIdentifier());
    auditTrail.setBrowserCookie(clientInfo.getBrowserCookie());
    auditTrail.setUrl(clientInfo.getReqUrl());
    auditTrail.setLogId(ShortCode.shortenInt(UUID.randomUUID().hashCode()));
    auditTrail.setSessionId(clientInfo.getSessionId());

    trailService.recordTrail(auditTrail);
}
```

### Anti-Patterns to Avoid
- **Sending emails synchronously:** Always use async via `Envelope` event publishing or `@Async` annotation
- **Creating multiple email templates per action:** Use a single generic template with conditional content
- **Putting business logic in listener:** Keep listener thin, delegate complex logic to services
- **Skipping audit trail for non-critical changes:** ALL profile changes should be audited per AUDT-01

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Audit trail persistence | Custom repository/entity | Existing `TrailService.recordTrail()` | Already handles transactions, mapping |
| Email sending | Direct `JavaMailSender` calls | Existing `Envelope` + `MailManager` | Handles async, retry, tracking |
| i18n in templates | Hardcoded strings | `#{message.key}` Thymeleaf syntax | Already configured MessageSource |
| Request context (IP, user agent) | Manual extraction | `RequestMetadataProvider.getClientInfo()` | Thread-safe, MDC-aware |
| Unique help codes | UUID.toString() | `ShortCode.shortenInt(UUID.randomUUID().hashCode())` | Short, user-friendly codes |

**Key insight:** The codebase has mature infrastructure for events, email, and audit. The implementation task is primarily wiring existing components together.

## Common Pitfalls

### Pitfall 1: Event Not Being Consumed
**What goes wrong:** `AccountChangeEvent` is published but listener never fires
**Why it happens:** Event publishing must use Spring's `ApplicationEventPublisher`, not manual instantiation
**How to avoid:**
- Inject `ApplicationEventPublisher` in services that publish events
- Verify listener bean is component-scanned (`@Component`)
- Ensure `@EventListener` annotation is present
**Warning signs:** No log output from listener, no audit trail entries

### Pitfall 2: Missing Request Context in Listener
**What goes wrong:** `RequestMetadataProvider.getClientInfo()` returns null/empty values
**Why it happens:** If event is processed asynchronously in different thread, HTTP request context is lost
**How to avoid:**
- Use synchronous event processing (default for `@EventListener`)
- If async needed, pass context data in the event itself
**Warning signs:** Audit trail entries with null IP address, user agent

### Pitfall 3: Template Name Mismatch
**What goes wrong:** `TemplateProcessingException` at runtime
**Why it happens:** `MailService.sendEmailFromTemplate` converts enum name to camelCase for template lookup
**How to avoid:**
- EmailTemplate enum: `PROFILE_CHANGE`
- Template file: `profileChange.html` (camelCase, in `mails/` folder)
- Follow CaseFormat conversion: `UPPER_UNDERSCORE` -> `LOWER_CAMEL`
**Warning signs:** `Could not parse as expression` errors

### Pitfall 4: i18n Keys Not Found
**What goes wrong:** Raw message keys displayed instead of translated text
**Why it happens:** Keys missing from properties files or locale mismatch
**How to avoid:**
- Add all keys to BOTH `messages_en.properties` and `messages_fr.properties`
- Verify `recipient.getLangKey()` returns valid locale (`en`, `fr`, not `en-US`)
- Test with both locales
**Warning signs:** Template shows `email.profile_change.title` literally

### Pitfall 5: Publishing Event from Service Without User Context
**What goes wrong:** Events published but recipient data is incomplete
**Why it happens:** Event created without fetching complete user data
**How to avoid:**
- Ensure `Recipient` has all required fields (firstname, lastname, email, langKey)
- Populate `Recipient` from `User` entity in the service before publishing
**Warning signs:** Emails sent to wrong address or with empty name

## Code Examples

Verified patterns from official sources:

### Creating AccountChangeEventListener
```java
// Source: Pattern from SecurityAuditListener.java
package com.softropic.promora.security.audit.listener;

import com.softropic.promora.common.ClockProvider;
import com.softropic.promora.email.api.EmailTemplate;
import com.softropic.promora.email.api.Envelope;
import com.softropic.promora.email.api.Recipient;
import com.softropic.promora.security.audit.api.AuditTrail;
import com.softropic.promora.security.audit.service.TrailService;
import com.softropic.promora.security.audit.shared.event.AccountChangeEvent;
import com.softropic.promora.security.exposed.util.RequestMetadata;
import com.softropic.promora.security.exposed.util.RequestMetadataProvider;
import com.softropic.promora.security.exposed.util.ShortCode;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class AccountChangeEventListener {

    private final TrailService trailService;
    private final ApplicationEventPublisher publisher;
    private final String baseUrl;

    public AccountChangeEventListener(TrailService trailService,
                                      ApplicationEventPublisher publisher,
                                      @Value("${baseurl}") String baseUrl,
                                      @Value("${server.port}") String serverPort) {
        this.trailService = trailService;
        this.publisher = publisher;
        this.baseUrl = baseUrl + ":" + serverPort;
    }

    @EventListener
    public void handleAccountChange(AccountChangeEvent event) {
        log.info("Processing AccountChangeEvent: {}", event.getAction());

        // Record audit trail
        recordAuditTrail(event);

        // Send notification email
        sendNotificationEmail(event);
    }

    private void recordAuditTrail(AccountChangeEvent event) {
        // ... implementation as shown in Pattern 3 ...
    }

    private void sendNotificationEmail(AccountChangeEvent event) {
        final String helpCode = ShortCode.shortenInt(UUID.randomUUID().hashCode());

        Map<String, Object> data = new HashMap<>();
        data.put("helpCode", helpCode);
        data.put("baseUrl", baseUrl);
        data.put("action", event.getAction().name());
        data.put("oldValue", event.getOldValue() != null ? event.getOldValue() : "");
        data.put("newValue", event.getNewValue() != null ? event.getNewValue() : "");

        Envelope envelope = new Envelope(
            List.of(event.getRecipient()),
            EmailTemplate.PROFILE_CHANGE,
            LocalDateTime.now(ClockProvider.getClock()).plusDays(7),
            data,
            helpCode
        );

        publisher.publishEvent(envelope);
    }
}
```

### Adding PROFILE_CHANGE to EmailTemplate Enum
```java
// Source: Existing EmailTemplate.java
public enum EmailTemplate {
    NONE(""),
    ACTIVATION("email.activation.title"),
    CREATION_DUP("email.creation_dup.title"),
    PASSWORD_RESET("email.pw_reset.title"),
    SEND_OTP("email.otp.title"),
    EMAIL_CHANGE("email.change.title"),
    POST_PURCHASE("email.post_purchase.title"),
    PROFILE_CHANGE("email.profile_change.title"); // ADD THIS

    private final String subjectKey;
    // ... rest unchanged
}
```

### Publishing AccountChangeEvent from UserProfileService
```java
// Source: Pattern from AccountManagementFacade
// In UserProfileService.changePassword():
public Optional<User> changePassword(String currentPassword, String newPassword) {
    return userRepository.findOneByLogin(securityUtil.getCurrentUser().getUsername())
            .map(user -> {
                if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                    throw new SecException("Current password does not match",
                            Map.of("login", user.getLogin()),
                            SecurityError.EMAIL_OR_PW_MISMATCH);
                }
                user.setPassword(passwordEncoder.encode(newPassword));
                log.debug("Changed password for User: {}", user.getLogin());

                // Publish event for notification and audit
                Recipient recipient = buildRecipient(user);
                AccountChangeEvent event = new AccountChangeEvent(
                    AccountChangeEvent.Action.PASSWORD_CHANGED,
                    null,  // oldValue not applicable for password
                    null,  // newValue not applicable for password
                    recipient
                );
                publisher.publishEvent(event);

                return user;
            });
}

private Recipient buildRecipient(User user) {
    Recipient recipient = new Recipient();
    recipient.setFirstname(user.getFirstName());
    recipient.setLastname(user.getLastName());
    recipient.setEmail(user.getEmail());
    recipient.setLangKey(user.getLangKey());
    recipient.setTitle(user.getTitle());
    recipient.setGender(Objects.toString(user.getGender()));
    return recipient;
}
```

### Profile Change Email Template (profileChange.html)
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title th:text="#{email.profile_change.title}">Profile Change Notification</title>
</head>
<body>
    <p th:text="#{email.greeting(${recipient.firstname})}">Hello User,</p>

    <div th:switch="${map.action}">
        <p th:case="'PASSWORD_CHANGED'" th:text="#{email.profile_change.password}">
            Your password has been changed.
        </p>
        <p th:case="'EMAIL_CHANGED'" th:utext="#{email.profile_change.email(${map.oldValue}, ${map.newValue})}">
            Your email has been changed.
        </p>
        <p th:case="'ADDRESS_CHANGED'" th:text="#{email.profile_change.address}">
            Your address has been updated.
        </p>
        <p th:case="'TWO_FACTOR_AUTH_ENABLED'" th:text="#{email.profile_change.2fa_enabled}">
            Two-factor authentication has been enabled.
        </p>
        <p th:case="'TWO_FACTOR_AUTH_DISABLED'" th:text="#{email.profile_change.2fa_disabled}">
            Two-factor authentication has been disabled.
        </p>
        <p th:case="*" th:text="#{email.profile_change.generic}">
            Your profile has been updated.
        </p>
    </div>

    <p th:text="#{email.profile_change.not_you}">
        If you did not make this change, please contact support immediately.
    </p>

    <hr/>
    <p><small><span th:text="#{email.common.helpCode}">Help Code:</span> <span th:text="${map.helpCode}">CODE</span></small></p>
</body>
</html>
```

### i18n Messages (English)
```properties
# Profile change email
email.profile_change.title=Security Alert - Profile Changed
email.profile_change.password=Your password has been successfully changed. If you did not make this change, please reset your password immediately.
email.profile_change.email=Your email address has been changed from {0} to {1}. If you did not make this change, please contact support.
email.profile_change.address=Your address has been updated successfully.
email.profile_change.2fa_enabled=Two-factor authentication has been enabled on your account for enhanced security.
email.profile_change.2fa_disabled=Two-factor authentication has been disabled on your account. We recommend keeping 2FA enabled.
email.profile_change.generic=A change has been made to your profile.
email.profile_change.not_you=If you did not make this change, please contact our support team immediately with the help code below.
```

### i18n Messages (French)
```properties
# Email de modification de profil
email.profile_change.title=Alerte de s\u00e9curit\u00e9 - Profil modifi\u00e9
email.profile_change.password=Votre mot de passe a \u00e9t\u00e9 modifi\u00e9 avec succ\u00e8s. Si vous n'avez pas effectu\u00e9 ce changement, veuillez r\u00e9initialiser votre mot de passe imm\u00e9diatement.
email.profile_change.email=Votre adresse e-mail a \u00e9t\u00e9 modifi\u00e9e de {0} \u00e0 {1}. Si vous n'avez pas effectu\u00e9 ce changement, veuillez contacter le support.
email.profile_change.address=Votre adresse a \u00e9t\u00e9 mise \u00e0 jour avec succ\u00e8s.
email.profile_change.2fa_enabled=L'authentification \u00e0 deux facteurs a \u00e9t\u00e9 activ\u00e9e sur votre compte pour une s\u00e9curit\u00e9 renforc\u00e9e.
email.profile_change.2fa_disabled=L'authentification \u00e0 deux facteurs a \u00e9t\u00e9 d\u00e9sactiv\u00e9e sur votre compte. Nous recommandons de garder la 2FA activ\u00e9e.
email.profile_change.generic=Une modification a \u00e9t\u00e9 apport\u00e9e \u00e0 votre profil.
email.profile_change.not_you=Si vous n'avez pas effectu\u00e9 ce changement, veuillez contacter notre \u00e9quipe de support imm\u00e9diatement avec le code d'aide ci-dessous.
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Synchronous email | Async via `@Async("sendMailPool")` | Already in codebase | Non-blocking, better UX |
| Per-action email templates | Generic template with `th:switch` | Best practice | Single template to maintain |
| Manual i18n | Thymeleaf + MessageSource | Already in codebase | Automatic locale resolution |

**Deprecated/outdated:**
- Direct `JavaMailSender` usage without tracking - use `Envelope` pattern
- Creating audit entries without `TrailService` - always use the service

## Open Questions

Things that couldn't be fully resolved:

1. **Should phone/info changes trigger notifications?**
   - What we know: Requirements list PASSWORD_CHANGED, EMAIL_CHANGED, ADDRESS_CHANGED, TWO_FACTOR_AUTH_* explicitly
   - What's unclear: AccountChangeEvent has `OTHERS` action but no specific phone/info actions
   - Recommendation: Add PHONE_CHANGED and INFO_CHANGED to the enum if notifications are desired, or skip notifications for these less-sensitive changes

2. **Email for EMAIL_CHANGED - old or new address?**
   - What we know: AccountChangeEvent has `Recipient` with single email
   - What's unclear: Should notification go to old email (security alert) or new email (confirmation)?
   - Recommendation: Send to OLD email for security (user should know their account email changed). Existing `AccountManagementFacade.changeEmail()` already sends to the user (which is updated to new email). May need to capture old email before update.

## Sources

### Primary (HIGH confidence)
- `security/audit/listener/SecurityAuditListener.java` - Event listener pattern
- `security/audit/shared/event/AccountChangeEvent.java` - Event structure
- `security/audit/service/TrailService.java` - Audit recording API
- `email/api/MailManager.java` - Email sending pattern
- `email/service/MailService.java` - Thymeleaf template processing
- `email/api/EmailTemplate.java` - Template enum pattern
- `resources/mails/*.html` - Template file patterns
- `resources/i18n/messages_*.properties` - i18n key patterns

### Secondary (MEDIUM confidence)
- Spring Framework documentation for `@EventListener`
- Thymeleaf documentation for `th:switch`

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Direct codebase inspection, no external dependencies needed
- Architecture patterns: HIGH - Following existing patterns exactly
- Pitfalls: HIGH - Based on codebase-specific patterns
- i18n: HIGH - Existing template/message patterns are clear

**Research date:** 2026-01-27
**Valid until:** 2026-02-27 (30 days - stable backend patterns)
