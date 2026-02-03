package com.softropic.promora.security.api;


import com.softropic.promora.common.ClockProvider;
import com.softropic.promora.common.dto.PhoneNumberDto;
import com.softropic.promora.common.validation.CamMobileValidator;
import com.softropic.promora.common.validation.PhoneNumber;
import com.softropic.promora.email.api.EmailTemplate;
import com.softropic.promora.email.api.Envelope;
import com.softropic.promora.email.api.Recipient;
import com.softropic.promora.security.api.registration.EmailRegistrationStrategy;
import com.softropic.promora.security.api.registration.RegistrationNotificationStrategy;
import com.softropic.promora.security.api.registration.SmsRegistrationStrategy;
import com.softropic.promora.security.exposed.ChangePasswordDto;
import com.softropic.promora.security.exposed.LoginIdType;
import com.softropic.promora.security.exposed.UserDto;
import com.softropic.promora.security.exposed.exception.OperationNotAllowedException;
import com.softropic.promora.security.exposed.util.ClientContextProvider;
import com.softropic.promora.security.exposed.util.ShortCode;
import com.softropic.promora.security.domain.User;
import com.softropic.promora.security.service.UserService;
import com.softropic.promora.security.service.UserRegistrationService;
import com.softropic.promora.security.service.UserProfileService;
import com.softropic.promora.security.service.PasswordResetService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import io.micrometer.common.util.StringUtils;

import static com.softropic.promora.security.exposed.exception.SecurityError.PWD_RESET_REJECTED;


@Service
public class AccountManagementFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountManagementFacade.class);

    private final UserService userService;
    private final UserRegistrationService userRegistrationService;
    private final UserProfileService userProfileService;
    private final PasswordResetService passwordResetService;
    private final ApplicationEventPublisher publisher;
    private final EmailRegistrationStrategy emailStrategy;
    private final SmsRegistrationStrategy smsStrategy;
    private final String host;

    public AccountManagementFacade(final UserService userService,
                                   final UserRegistrationService userRegistrationService,
                                   final UserProfileService userProfileService,
                                   final PasswordResetService passwordResetService,
                                   final ApplicationEventPublisher publisher,
                                   final EmailRegistrationStrategy emailStrategy,
                                   final SmsRegistrationStrategy smsStrategy,
                                   @Value("${baseurl}") final String baseurl,
                                   @Value("${server.port}") final String serverPort) {
        this.userService = userService;
        this.userRegistrationService = userRegistrationService;
        this.userProfileService = userProfileService;
        this.passwordResetService = passwordResetService;
        this.publisher = publisher;
        this.emailStrategy = emailStrategy;
        this.smsStrategy = smsStrategy;
        this.host = baseurl + ":" + serverPort;
    }

    public String sendPasswordResetMail(ChangePasswordDto changePasswordDto) {
        return passwordResetService.prepareForPasswordReset(changePasswordDto)
                          .map(user -> {
                              final Map<String, Object> dataMap = ClientContextProvider.getClientContextMap();
                              dataMap.put("resetKey", user.getResetKey());
                              return sendMail(EmailTemplate.PASSWORD_RESET,
                                              LocalDateTime.now(ClockProvider.getClock()).plusDays(1),
                                              dataMap,
                                              user);
                          }
                          ).orElseThrow(() -> new OperationNotAllowedException("The given data or account state does not permit the reset of the password.",
                                                                               Map.of("email", changePasswordDto.getCurrentEmail(),
                                                                                      "loginId", changePasswordDto.getLoginId(),
                                                                                      "dob", changePasswordDto.getDob()),
                                                                               PWD_RESET_REJECTED));
    }

    public User finishPasswordReset(final KeyAndPasswordDto keyAndPassword) {
        return passwordResetService.completePasswordReset(keyAndPassword.password(), keyAndPassword.key())
                          .orElseThrow(() -> new OperationNotAllowedException("The account state does not permit the reset of the password.",
                                                                              Map.of("key", keyAndPassword.key()),
                                                                              PWD_RESET_REJECTED));
    }

    /**
     * Registers a new user account and sends appropriate notification via email or SMS.
     * <p>
     * This method handles both email-based and phone-based registration by using
     * the Strategy pattern to delegate notification logic to the appropriate channel.
     * <p>
     * <b>Security Considerations:</b>
     * <ul>
     *   <li>When user already exists, sends security alert notification</li>
     *   <li>Uses timing-safe comparison to prevent user enumeration</li>
     *   <li>Delegates to strategies that implement rate limiting</li>
     * </ul>
     * <p>
     * <b>TODO:</b> Implement rate limiting - if called above threshold, blacklist client
     * and short-circuit (delayed to avoid timing attacks).
     * <p>
     * <b>TODO:</b> Messaging module should avoid sending duplicate notifications to
     * the same user within 5 minutes.
     *
     * @param userDTO the user registration data
     * @return a tracking code for the notification sent
     */
    public String registerAccount(final UserDto userDTO) {
        final Optional<User> optionalUser = userRegistrationService.findUserByEmailOrLogin(
            userDTO.getEmail() != null ? userDTO.getEmail().toLowerCase() : null,
            userDTO.getLogin() != null ? userDTO.getLogin().toLowerCase() : null
        );

        final RegistrationNotificationStrategy strategy;

        if (StringUtils.isNotBlank(userDTO.getEmail())) {
            userDTO.setLoginIdType(LoginIdType.EMAIL);
            userDTO.setLogin(userDTO.getEmail());
            strategy = emailStrategy;
        } else {
            userDTO.setLoginIdType(LoginIdType.PHONE);
            userDTO.setLogin(userDTO.getPhone());
            strategy = smsStrategy;
        }

        if (optionalUser.isPresent()) {
            return strategy.notifyUserExists(optionalUser.get());
        } else {
            final User user = persistUser(userDTO);
            return strategy.notifyNewUser(user);
        }
        // Note: The notification works only when login matches the notification channel
        // (e.g., email for EMAIL type). Using login names with email notifications could
        // expose valid logins to attackers.
    }

    public String resendRegistrationLink(String login, String password) {
        final Optional<User> userByLogin = userService.findUserByLogin(login);
        if(userByLogin.isPresent()) {
            final User user = userByLogin.get();
            if(user.getActivationDate() == null && passwordResetService.isPasswordMatch(password, user.getPassword())) {
                // Use email strategy for resending activation link
                return emailStrategy.notifyNewUser(user);
            }
        }
        // TODO: Special log to indicate security issue. Output the login used
        return null;
    }

    public String changeEmail(String oldEmail, String newEmail, String password) {
        final Optional<User> userOpt = userProfileService.updateUserEmail(oldEmail, newEmail, password);
        if(userOpt.isPresent()) {
            final User user = userOpt.get();
            Map<String, Object> dataMap = ClientContextProvider.getClientContextMap();
            dataMap.put("action", "EMAIL_CHANGED");
            dataMap.put("oldValue", oldEmail);
            dataMap.put("newValue", newEmail);
            return sendMail(EmailTemplate.PROFILE_CHANGE,
                            LocalDateTime.now(ClockProvider.getClock()).plusDays(7),
                            dataMap,
                            user);
        }
        // Should actually not reach here
        return null;
    }

    private String sendMail(EmailTemplate emailTemplate,
                          LocalDateTime deadline,
                          Map<String, Object> dataMap,
                          User user) {
        final String shortCode = ShortCode.shortenInt(UUID.randomUUID().hashCode());
        dataMap.put("helpCode", shortCode);
        dataMap.put("baseUrl", host);
        final Recipient recipient = buildRecipient(user);
        final Envelope envelope = new Envelope(List.of(recipient),
                                               emailTemplate,
                                               deadline,
                                               dataMap,
                                               shortCode);
        publisher.publishEvent(envelope);

        if(LOGGER.isInfoEnabled()) {
            LOGGER.info(user.getLogin());
        }
        return shortCode;
    }

    private User persistUser(final UserDto userDTO){
        return userRegistrationService.createUser(toUser(userDTO), userDTO.getPassword());
    }

    private Recipient buildRecipient(final User user) {
        final Recipient recipient = new Recipient();
        recipient.setFirstname(user.getFirstName());
        recipient.setLastname(user.getLastName());
        recipient.setEmail(user.getEmail());
        recipient.setLangKey(user.getLangKey());
        recipient.setTitle(user.getTitle());
        recipient.setGender(Objects.toString(user.getGender()));
        return recipient;
    }

    private User toUser(final UserDto userDTO) {
        final User user = new User();
        user.setLogin(userDTO.getLogin());
        user.setLoginIdType(userDTO.getLoginIdType());
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setEmail(userDTO.getEmail().toLowerCase());
        user.setPhone(toPhoneNumber(userDTO.getPhone()));
        user.setLangKey(userDTO.getLangKey());
        user.setGender(userDTO.getGender());
        user.setTitle(userDTO.getTitle());
        user.setDateOfBirth(userDTO.getDob());
        user.setOtpEnabled(userDTO.isOtpEnabled());
        user.setNationalId(userDTO.getNationalId());
        return user;
    }

    protected PhoneNumber toPhoneNumber(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }

        PhoneNumber phoneNumber = new PhoneNumber();
        //TODO elegantly extract the phone number building logic from CamMobileValidator to a dedicated class
        final PhoneNumberDto phoneNoDto = CamMobileValidator.validate(phone);

        phoneNumber.setPhone(phoneNoDto.getPhone());
        phoneNumber.setIso2Country(phoneNoDto.getIso2Country());
        phoneNumber.setPhoneType(Objects.equals(phoneNoDto.getPhoneType(), PhoneNumberDto.PhoneType.MOBILE) ? PhoneNumber.PhoneType.MOBILE : PhoneNumber.PhoneType.FIXED);
        phoneNumber.setProvider(phoneNoDto.getProvider());

        return phoneNumber;
    }


}
