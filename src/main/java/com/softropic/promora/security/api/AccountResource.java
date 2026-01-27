package com.softropic.promora.security.api;




import com.softropic.promora.common.message.Failure;
import com.softropic.promora.common.message.Response;
import com.softropic.promora.common.message.Success;
import com.softropic.promora.security.api.dto.AddressDto;
import com.softropic.promora.security.api.dto.ChangePasswordRequestDto;
import com.softropic.promora.security.api.dto.ChangePhoneDto;
import com.softropic.promora.security.api.dto.Toggle2faDto;
import com.softropic.promora.security.api.dto.UpdateUserInfoDto;
import com.softropic.promora.security.domain.Address;
import com.softropic.promora.security.exposed.ChangePasswordDto;
import com.softropic.promora.security.exposed.UserDto;
import com.softropic.promora.security.exposed.exception.AuthorizationException;
import com.softropic.promora.security.exposed.exception.SecurityError;
import com.softropic.promora.security.service.UserProfileService;
import com.softropic.promora.security.service.UserService;
import com.softropic.promora.security.service.UserRegistrationService;
import com.softropic.promora.security.core.mapper.UserMapper;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;


import io.micrometer.core.annotation.Timed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * REST controller for managing the current user's account.
 */
@RestController
@RequestMapping("/v1/account")
public class AccountResource {

    private final Logger log = LoggerFactory.getLogger(AccountResource.class);

    @Autowired
    private UserService userService;

    @Autowired
    private UserRegistrationService userRegistrationService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AccountManagementFacade accountManagementFacade;

    @Autowired
    private UserProfileService userProfileService;

    /**
     * POST  /register to register the user.
     * @param userDTO holds the user's data
     * @return ResponseEntity
     */
    @PostMapping(value="/register", produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public Success registerAccount(@Valid @RequestBody final UserDto userDTO)  {
        final String emailSendId = accountManagementFacade.registerAccount(userDTO);
        final String msg = "You will receive an email shortly with an activation key or else contact support with the help code";
        return new Success(emailSendId, "user.creation.feedback", msg, Map.of());
    }

    /**
     * POST  /regislink to register the user.
     *
     * @return ResponseEntity
     */
    @PostMapping(value="/regislink", produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public Success registerLinkResend(@NotNull String login, @NotNull String password)  {
        final String emailSendId = accountManagementFacade.resendRegistrationLink(login, password);
        final String msg = "You will receive an email shortly with an activation key or else contact support with the help code";
        return new Success(emailSendId, "user.creation.feedback", msg, Map.of());
    }

    /**
     * GET  /activate to activate the registered user.
     * @param key is the string used for activation
     * @return response status
     */
    @PostMapping(value = "/activate",  produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public Success activateAccount(@RequestParam("key")final  String key) {
        return userRegistrationService.activateUser(key)
                          .map(u -> new Success(null,
                                                "user.activation.success",
                                                "Account has been activated",
                                                Map.of()))
                          .orElseThrow(() -> new AuthorizationException("Activation key invalid or already used",
                                                                        SecurityError.INVALID_ACTIVATION_KEY));
    }

    /**
     * GET  /authenticate to check if the user is authenticated, and return its login.
     * @param request httpServletRequest
     * @return remote user
     */
    @GetMapping(value = "/authenticate", produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public String getAuthenticatedUser(final HttpServletRequest request) {
        log.debug("REST request to check if the current user is authenticated");
        return request.getRemoteUser();
    }

    /**
     * GET  / to get the current user.
     * @return Http status code
     */
    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<UserDto> getAccount() {
        return Optional.ofNullable(userService.getUserWithAuthorities())
            .map(user -> new ResponseEntity<>(userMapper.toUserDto(user), HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
    }


    @PostMapping(value = "/reset_password/init", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Response> requestPasswordReset(@RequestBody @Valid ChangePasswordDto changePasswordDto) {
        final String code = accountManagementFacade.sendPasswordResetMail(changePasswordDto);
        return new ResponseEntity<>(new Success(code,
                                                "password.reset.emailed",
                                                "Check your email for a link to reset your password",
                                                Map.of()), HttpStatus.ACCEPTED);
    }

    @PostMapping(value = "/reset_password/finish", produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public Success finishPasswordReset(@RequestBody @Valid KeyAndPasswordDto keyAndPassword) {//password.reset.success
        accountManagementFacade.finishPasswordReset(keyAndPassword);
        return new Success(null,
                           "password.reset.success",
                           "Your password has now been reset. You can now login with your new password.",
                           Map.of());
    }

    @PostMapping(value = "/change_email", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public Response changeEmail(@Valid @RequestBody ChangeEmailDto changeEmailDto) {
        final String code = accountManagementFacade.changeEmail(changeEmailDto.getOldEmail(),
                                                              changeEmailDto.getNewEmail(),
                                                              changeEmailDto.getPassword());
        if(StringUtils.isNotBlank(code)) {
            return  new Success(code,
                                "email.updated",
                                "Your email address has been updated",
                                Map.of());
        }
        return new Failure(UUID.randomUUID().toString(), "email.change.failure", "Your email cannot be changed");
    }

    @GetMapping(value = "/ping", produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public Map<String, String> pong() {
        return Map.of("server", "up");
    }

    // =========================================================================
    // Profile Management Endpoints (/api/account/*)
    // =========================================================================

    /**
     * GET /api/account/profile to get the current user's profile.
     * @return user profile data
     */
    @GetMapping(value = "/api/account/profile", produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<UserDto> getProfile() {
        return Optional.ofNullable(userService.getUserWithAuthorities())
            .map(user -> new ResponseEntity<>(userMapper.toUserDto(user), HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    /**
     * PUT /api/account/email to update the current user's email.
     * Sends verification email to the new address.
     * @param changeEmailDto contains old email, new email, and password for verification
     * @return success or failure response
     */
    @PutMapping(value = "/api/account/email", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public Response updateEmail(@Valid @RequestBody ChangeEmailDto changeEmailDto) {
        final String code = accountManagementFacade.changeEmail(
            changeEmailDto.getOldEmail(),
            changeEmailDto.getNewEmail(),
            changeEmailDto.getPassword());
        if (StringUtils.isNotBlank(code)) {
            return new Success(code, "email.updated", "Your email address has been updated", Map.of());
        }
        return new Failure(UUID.randomUUID().toString(), "email.change.failure", "Your email cannot be changed");
    }

    /**
     * PUT /api/account/password to change the current user's password.
     * @param dto contains current password and new password
     * @return success response
     */
    @PutMapping(value = "/api/account/password", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public Success updatePassword(@Valid @RequestBody ChangePasswordRequestDto dto) {
        userProfileService.changePassword(dto.getCurrentPassword(), dto.getNewPassword())
            .orElseThrow(() -> new AuthorizationException("User not found", SecurityError.USER_NOT_FOUND));
        return new Success(null, "password.changed", "Your password has been changed", Map.of());
    }

    /**
     * PUT /api/account/phone to update the current user's phone number.
     * @param dto contains the new phone number
     * @return success response
     */
    @PutMapping(value = "/api/account/phone", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public Success updatePhone(@Valid @RequestBody ChangePhoneDto dto) {
        userProfileService.updatePhone(dto.getPhone())
            .orElseThrow(() -> new AuthorizationException("User not found", SecurityError.USER_NOT_FOUND));
        return new Success(null, "phone.changed", "Your phone number has been updated", Map.of());
    }

    /**
     * PUT /api/account/address to update the current user's address.
     * @param dto contains all address fields
     * @return success response
     */
    @PutMapping(value = "/api/account/address", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public Success updateAddress(@Valid @RequestBody AddressDto dto) {
        Address address = new Address();
        address.setName(dto.getName());
        address.setCompanyName(dto.getCompanyName());
        address.setAddressLine1(dto.getAddressLine1());
        address.setAddressLine2(dto.getAddressLine2());
        address.setAddressLine3(dto.getAddressLine3());
        address.setCity(dto.getCity());
        address.setStateProvince(dto.getStateProvince());
        address.setPostalCode(dto.getPostalCode());
        address.setCountry(dto.getCountry());

        userProfileService.updatePostalAddress(address)
            .orElseThrow(() -> new AuthorizationException("User not found", SecurityError.USER_NOT_FOUND));
        return new Success(null, "address.changed", "Your address has been updated", Map.of());
    }

    /**
     * PUT /api/account/info to update the current user's core information.
     * @param dto contains firstName, lastName, langKey, nationalId, gender, title
     * @return success response
     */
    @PutMapping(value = "/api/account/info", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public Success updateInfo(@Valid @RequestBody UpdateUserInfoDto dto) {
        userProfileService.updateUserInformation(
            dto.getFirstName(),
            dto.getLastName(),
            dto.getLangKey(),
            dto.getNationalId(),
            dto.getGender(),
            dto.getTitle())
            .orElseThrow(() -> new AuthorizationException("User not found", SecurityError.USER_NOT_FOUND));
        return new Success(null, "info.changed", "Your profile information has been updated", Map.of());
    }

    /**
     * PUT /api/account/2fa to toggle two-factor authentication.
     * @param dto contains enabled flag and password for verification
     * @return success response
     */
    @PutMapping(value = "/api/account/2fa", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public Success toggle2fa(@Valid @RequestBody Toggle2faDto dto) {
        userProfileService.toggle2fa(dto.getEnabled(), dto.getPassword())
            .orElseThrow(() -> new AuthorizationException("User not found or password mismatch", SecurityError.EMAIL_OR_PW_MISMATCH));
        String status = dto.getEnabled() ? "enabled" : "disabled";
        return new Success(null, "2fa.changed", "Two-factor authentication has been " + status, Map.of());
    }

}
