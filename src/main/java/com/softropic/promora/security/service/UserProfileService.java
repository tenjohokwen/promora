package com.softropic.promora.security.service;

import com.softropic.promora.common.Gender;
import com.softropic.promora.security.common.util.SecurityConstants;
import com.softropic.promora.security.domain.Address;
import com.softropic.promora.security.domain.User;
import com.softropic.promora.security.exposed.exception.SecException;
import com.softropic.promora.security.exposed.exception.SecurityError;
import com.softropic.promora.security.exposed.util.SecurityUtil;
import com.softropic.promora.security.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

/**
 * Service for handling user profile management operations.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final SecurityUtil securityUtil;
    private final PasswordEncoder passwordEncoder;

    /**
     * Updates the current user's basic information (name and language preference).
     *
     * @param firstname the first name
     * @param lastname the last name
     * @param langKey the language key
     * @return the updated user if found, empty otherwise
     */
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    public Optional<User> updateUserInformation(String firstname, String lastname, String langKey, String nationalId, Gender gender, String title) {
        return userRepository.findOneByLogin(securityUtil.getCurrentUser().getUsername()).map(u -> {
            u.setFirstName(firstname);
            u.setLastName(lastname);
            u.setLangKey(langKey);
            u.setNationalId(nationalId);
            u.setGender(gender);
            u.setTitle(title);
            log.debug("Changed Information for User: {}", u);
            return u;
        });
    }

    /**
     * Updates the current user's postal address.
     *
     * @param address the new address
     * @return the updated user if found, empty otherwise
     */
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    public Optional<User> updatePostalAddress(Address address) {
        return userRepository.findOneByLogin(securityUtil.getCurrentUser().getUsername())
                .map(u -> {
                    u.addOrReplaceAddress(address);
                    return u;
                });
    }

    /**
     * Updates the current user's email address.
     * Requires the old email and current password for verification.
     *
     * @param oldEmail the current email address
     * @param newEmail the new email address
     * @param password the current password (for verification)
     * @return the updated user if successful
     * @throws SecException if email or password doesn't match
     */
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    public Optional<User> updateUserEmail(String oldEmail, final String newEmail, String password) {
        return userRepository.findOneByLogin(securityUtil.getCurrentUser().getUsername()).map(u -> {
            if (passwordEncoder.matches(password, u.getPassword()) && StringUtils.equals(oldEmail, u.getEmail())) {
                u.setEmail(newEmail);
                u.setLogin(newEmail); //best pracs recommend this change
                log.debug("Changed email for User: {}", u);
                return u;
            }
            final Map<String, Object> ctx = Map.of("oldEmail", oldEmail,
                    "newEmail", newEmail,
                    "passwordMatch", passwordEncoder.matches(password, u.getPassword()),
                    "emailMatch", StringUtils.equals(oldEmail, u.getEmail()));
            throw new SecException("Cannot update email address", ctx, SecurityError.EMAIL_OR_PW_MISMATCH);
        });
    }
}
