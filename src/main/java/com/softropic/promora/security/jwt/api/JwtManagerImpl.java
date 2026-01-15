package com.softropic.promora.security.jwt.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.softropic.promora.common.ClockProvider;
import com.softropic.promora.common.Gender;
import com.softropic.promora.common.exception.AppSetupException;
import com.softropic.promora.security.SimpleGrantedAuthorityMixin;
import com.softropic.promora.security.common.service.LoginTokenManager;
import com.softropic.promora.security.common.util.CookieUtil;
import com.softropic.promora.security.exposed.Principal;
import com.softropic.promora.security.exposed.exception.AuthorizationException;
import com.softropic.promora.security.exposed.exception.InvalidJWTDataException;
import com.softropic.promora.security.exposed.exception.JWTExpiredException;
import com.softropic.promora.security.exposed.exception.MissingClientIdException;
import com.softropic.promora.security.exposed.util.RequestMetadata;
import com.softropic.promora.security.exposed.util.RequestMetadataProvider;
import com.softropic.promora.security.secret.SecretService;
import com.softropic.promora.security.secret.repository.Secret;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import static com.softropic.promora.security.common.util.SecurityConstants.ADMIN_COOKIE;
import static com.softropic.promora.security.common.util.SecurityConstants.ANONYMOUS_SESSION_COOKIE;
import static com.softropic.promora.security.common.util.SecurityConstants.BUS_ID;
import static com.softropic.promora.security.common.util.SecurityConstants.B_COOKIE;
import static com.softropic.promora.security.common.util.SecurityConstants.DB_REFRESH_TOKEN;
import static com.softropic.promora.security.common.util.SecurityConstants.DB_REFRESH_TOKEN_INTERVAL;
import static com.softropic.promora.security.common.util.SecurityConstants.DISPLAY_NAME;
import static com.softropic.promora.security.common.util.SecurityConstants.GENDER;
import static com.softropic.promora.security.common.util.SecurityConstants.JWT_BUS_NAME;
import static com.softropic.promora.security.common.util.SecurityConstants.JWT_COOKIE_NAME;
import static com.softropic.promora.security.common.util.SecurityConstants.JWT_SESSION_COOKIE;
import static com.softropic.promora.security.common.util.SecurityConstants.JWT_TTL;
import static com.softropic.promora.security.common.util.SecurityConstants.JWT_VERSION;
import static com.softropic.promora.security.common.util.SecurityConstants.LOGIN_INFO_ID;
import static com.softropic.promora.security.common.util.SecurityConstants.OTP_ENABLED;
import static com.softropic.promora.security.common.util.SecurityConstants.OTP_TTL;
import static com.softropic.promora.security.common.util.SecurityConstants.ROLES;
import static com.softropic.promora.security.common.util.SecurityConstants.SESSION_ID;
import static com.softropic.promora.security.common.util.SecurityConstants.SESSION_REFRESH_COUNTDOWN;
import static com.softropic.promora.security.common.util.SecurityConstants.USER_COOKIE;
import static com.softropic.promora.security.exposed.exception.SecurityError.FAULTY_JWT_ROLES_CLAIMS;
import static com.softropic.promora.security.exposed.exception.SecurityError.JWT_EXPIRED;
import static com.softropic.promora.security.exposed.exception.SecurityError.JWT_PARSE_ERROR;
import static com.softropic.promora.security.exposed.exception.SecurityError.MISSING_JWT_DB_REFRESH_TOKEN;
import static com.softropic.promora.security.exposed.exception.SecurityError.MISSING_JWT_PRINCIPAL;
import static com.softropic.promora.security.exposed.exception.SecurityError.MISSING_JWT_SECRET;
import static com.softropic.promora.security.exposed.util.AuthoritiesConstants.ANONYMOUS;
import static java.lang.String.format;

@Slf4j
@Component
public class JwtManagerImpl implements LoginTokenManager {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    public static final String CLIENT_ID = "clientId";
    private static final String USER_AGENT_HASH = "userAgent";
    private static final String OPF_SEED = "opfSeed"; // TODO: The goal was to obfuscate ids using the seed of the user after login. The issue here is that we will have issues if we want a user to say put stuff in a shopping cart and then login at checkout
    // TODO: Or if we want to relog a user and continue after his token had expired. This would complicate stuff

    private volatile Secret secret;

    private final SecretService secretService;

    static {
        MAPPER.setVisibility(MAPPER.getSerializationConfig()
                                   .getDefaultVisibilityChecker()
                                   .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                                   .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                                   .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                                   .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
        MAPPER.addMixIn(SimpleGrantedAuthority.class, SimpleGrantedAuthorityMixin.class);
    }

    public JwtManagerImpl(SecretService secretService) {this.secretService = secretService;}

    @Override
    public void createLoginToken(HttpServletResponse res, Principal principal) {
        final long dbRefreshToken = Instant.now(ClockProvider.getClock())
                                           .plus(DB_REFRESH_TOKEN_INTERVAL.toMillis(), ChronoUnit.MILLIS)
                                           .toEpochMilli();
        final Map<String, Object> claims = toClaims(principal, dbRefreshToken, false, null);
        createAndSetJwt(res, claims);
        //log.info("Login token created successfully for user: {}", principal.getUsername());
    }

    @Override
    public void renewLoginToken(HttpServletResponse res, Principal principal) {
        //TODO is there a difference between 'renewLoginToken' and 'refreshLoginToken'? When is each called?
        final long dbRefreshToken = Instant.now(ClockProvider.getClock())
                                           .plus(DB_REFRESH_TOKEN_INTERVAL.toMillis(), ChronoUnit.MILLIS)
                                           .toEpochMilli();

        final Map<String, Object> claims = toClaims(principal, dbRefreshToken, true, null);
        createAndSetJwt(res, claims);
        //log.info("Login token renewed successfully for user: {}", principal.getUsername());
    }

    @Override
    public Authentication refreshLoginToken(HttpServletResponse res, String token) {
        final Optional<Claims> claimsOpt = extractClaimsFromTokenSilently(token);
        if (claimsOpt.isPresent()) {
            final Claims extractedClaims = claimsOpt.get();
            Map<String, Object> claims = new HashMap<>(extractedClaims);
            claims.put(Claims.ISSUED_AT, Date.from(Instant.now(ClockProvider.getClock())).getTime() / 1000);
            final long dbRefreshToken = Instant.now(ClockProvider.getClock())
                                               .plus(DB_REFRESH_TOKEN_INTERVAL.toMillis(), ChronoUnit.MILLIS)
                                               .toEpochMilli();
            claims.put(DB_REFRESH_TOKEN, dbRefreshToken);

            createAndSetJwt(res, claims);
            //log.info("Login token refreshed successfully for login: {}", claims.get(Claims.SUBJECT));
            return authentication(claims);
        }
        //MAybe an event is needed here for the audit trail
        throw new InvalidJWTDataException("Claims not present in token or invalid token.", JWT_PARSE_ERROR);
    }

    private Authentication authentication(Map<String, Object> claims) {
        final boolean otpEnabled = Objects.nonNull(claims.get(OTP_ENABLED)) && (boolean) claims.get(OTP_ENABLED);
        final Principal principal = new Principal.Builder().otpEnabled(otpEnabled).enabled(true).password("N/A")
                                                           .username((String) claims.get(Claims.SUBJECT))
                                                           .businessId((String) claims.get(BUS_ID))
                                                           .displayName((String) claims.get(DISPLAY_NAME))
                                                           .gender(Gender.valueOf((String) claims.get(GENDER)))
                                                           .authorities(getAuthoritiesSilently(claims))
                                                           .build();
        return UsernamePasswordAuthenticationToken.authenticated(principal,"", principal.getAuthorities());
    }

    private static Collection getAuthoritiesSilently(Map<String, Object> claims)  {
        try {
            final String roles = (String) claims.get(ROLES);
            return MAPPER.readValue(roles, new TypeReference<List<SimpleGrantedAuthority>>() {});
        }
        catch (JsonProcessingException e) {
            //log.error("Could not parse authorities gotten from claims: {}", claims.get(ROLES), e);
        }
        return List.of();
    }

    @Override
    public String generateToken(Principal principal, String seed) {
        final long dbRefreshToken = Instant.now(ClockProvider.getClock())
                                           .plus(DB_REFRESH_TOKEN_INTERVAL.toMillis(), ChronoUnit.MILLIS)
                                           .toEpochMilli();
        final Map<String, Object> claims = toClaims(principal, dbRefreshToken, false, seed);
        final String token = generateTokenFromClaims(claims);
        //log.info("Login token generated successfully for user: {}", claims.get(Claims.SUBJECT));
        return token;
    }

    @Override
    public String generateAnonymousSession(HttpServletResponse response) {
        final String sessionId = RequestMetadataProvider.getClientInfo().getSessionId();
        CookieUtil.addCookie(response, ANONYMOUS_SESSION_COOKIE, sessionId, true, -1);
        return sessionId;
    }

    @Override
    public void removeTwoFactorCookie(HttpServletResponse response) {
        CookieUtil.removeCookie(LOGIN_INFO_ID, response, true);
    }

    @Override
    public void extendTtlOfToken(HttpServletRequest req, HttpServletResponse res) {
        final Principal principal = extractPrincipal(req);
        if (principal != null) {
            //reuse the db refresh token. Do NOT renew it
            final Long dbRefreshToken = extractDbRefreshToken(req);
            if (dbRefreshToken != null) {
                final Map<String, Object> claims = toClaims(principal, dbRefreshToken, true, null);
                createAndSetJwt(res, claims);
            }
            else {
                throw new InvalidJWTDataException("Cannot find dbRefreshToken in JWT.", MISSING_JWT_DB_REFRESH_TOKEN);
            }
        }
        else {
            throw new InvalidJWTDataException("Cannot find principal in JWT.", MISSING_JWT_PRINCIPAL);
        }
    }

    @Override
    public void deleteLoginToken(HttpServletResponse response) {
        CookieUtil.removeCookie(JWT_COOKIE_NAME, response, true);
        CookieUtil.removeCookie(B_COOKIE, response, true);
        CookieUtil.removeCookie(USER_COOKIE, response, false);
        CookieUtil.removeCookie(ADMIN_COOKIE, response, false);
        CookieUtil.removeCookie(JWT_SESSION_COOKIE, response, true);
        CookieUtil.removeCookie(SESSION_REFRESH_COUNTDOWN, response, false);
    }

    @Override
    public Principal extractPrincipal(HttpServletRequest request) {
        final String token = CookieUtil.getCookieValue(request, JWT_COOKIE_NAME);
        if (StringUtils.isNotBlank(token)) {
            // parse the token.
            final Claims claims = extractClaims(token);
            final Set<SimpleGrantedAuthority> authorities = getRoles(claims);
            final boolean otpEnabled = Objects.nonNull(claims.get(OTP_ENABLED)) && (boolean) claims.get(OTP_ENABLED);
            return new Principal.Builder().username(claims.getSubject())
                                          .password("protectedPassword")
                                          .enabled(true)
                                          .otpEnabled(otpEnabled)
                                          .authorities(authorities)
                                          .gender(Gender.fromString(claims.get(GENDER, String.class)))
                                          .businessId(claims.get(BUS_ID, String.class))
                                          .phone(" ")
                                          .displayName(claims.get(DISPLAY_NAME, String.class))
                                          .build();
        }
        return new Principal.Builder().username(ANONYMOUS)
                                      .password("N/A")
                                      .enabled(false)
                                      .otpEnabled(false)
                                      .authorities(List.of())
                                      .businessId("anon1")
                                      .phone(" ")
                                      .displayName("")
                                      .build();
    }

    @Override
    public boolean isTokenFixed(HttpServletRequest request) {
        //TODO if bcookie exists but client is not a browser, assume token fixation
        final String token = CookieUtil.getCookieValue(request, JWT_COOKIE_NAME);
        if (StringUtils.isNotBlank(token)) {
            final Claims claims = extractClaims(token);
            final String clientId = claims.get(CLIENT_ID, String.class);
            String claimsUA = claims.get(USER_AGENT_HASH, String.class);
            RequestMetadata clientInfo = RequestMetadataProvider.getClientInfo();
            final String currentUA = clientInfo.getUserAgent() == null ? null : String.valueOf(clientInfo.getUserAgent()
                                                                                                         .hashCode());
            final String sessionId = claims.get(SESSION_ID, String.class);
            return !StringUtils.equals(claimsUA, currentUA) ||
                    !Objects.equals(clientId, clientInfo.getClientIdentifier()) ||
                    !Objects.equals(clientInfo.getSessionId(), sessionId);
        }
        //TODO ensure this is a valid flow
        return false;
    }

    @Override
    public boolean hasDbRefreshTokenExpired(HttpServletRequest request) {
        final Long dbRefreshToken = extractDbRefreshToken(request);
        if (dbRefreshToken != null) {
            return Instant.ofEpochMilli(dbRefreshToken).isBefore(Instant.now(ClockProvider.getClock()));
        }

        throw new InvalidJWTDataException("Cannot find dbRefreshToken in JWT.", MISSING_JWT_DB_REFRESH_TOKEN);
    }

    @Override
    public String extractUserNameSilently(HttpServletRequest request) {
        return extractClaimsSilently(request).map(Claims::getSubject).orElse(null);
    }

    @Override
    public void addLoginInfoIdCookie(HttpServletResponse res, String value) {
        CookieUtil.addCookie(res, LOGIN_INFO_ID, value, true, (int) OTP_TTL.toSeconds());
    }

    @Override
    public String extractLoginInfoIdCookie(HttpServletRequest req) {
        return CookieUtil.getCookieValue(req, LOGIN_INFO_ID);
    }

    @Override
    public void ensureClientHasPreLoginId() {
        final RequestMetadata requestMetadata = RequestMetadataProvider.getClientInfo();
        if (!requestMetadata.isMachineClient() &&
                StringUtils.isBlank(requestMetadata.getFingerprintCookie())) {
            String msg = "Missing pre-login client identifier. Expected a 'fcookie' or 'apikey'";
            throw new MissingClientIdException(msg);
        }
    }

    @Override
    public void ensureClientHasPostLoginId() {
        final RequestMetadata requestMetadata = RequestMetadataProvider.getClientInfo();
        if (StringUtils.isBlank(requestMetadata.getBrowserCookie()) && StringUtils.isBlank(requestMetadata.getApiKey())) {
            String msg = "Missing post-login client identifier. Expected 'bcookie or 'apikey'";
            throw new MissingClientIdException(msg);
        }
    }

    @Override
    public void ensureAuthTokenPresent(HttpServletRequest request) throws AuthorizationException {
        final String token = CookieUtil.getCookieValue(request, JWT_COOKIE_NAME);
        if (StringUtils.isBlank(token)) {
            //this exception will just cause a security trail log as opposed to others that could cause a user to be locked out
            throw new AccessDeniedException("Missing auth token. Expected 'jwt' cookie.");
        }
    }

    @Override
    public Optional<String> extractSessionIdSilently(final HttpServletRequest request) {
        final Optional<Claims> claimsOpt = extractClaimsSilently(request);
        if (claimsOpt.isPresent()) {
            try {
                return Optional.ofNullable(claimsOpt.get().get(SESSION_ID, String.class));
            }
            catch (IllegalArgumentException iae) {
                //log.warn("Could not extract sessionId from request", iae);
            }
        }
        return Optional.empty();
    }

    private void createAndSetJwt(HttpServletResponse res,
                                 Map<String, Object> claims) throws InvalidJWTDataException {
        createLoginCookies(res, claims);
        final String token = generateTokenFromClaims(claims);
        //Setting JWT as header is now deprecated set as cookie
        //res.addHeader(HEADER_STRING, TOKEN_PREFIX + token);
        CookieUtil.addCookie(res, JWT_COOKIE_NAME, token, true, (int) JWT_TTL.toSeconds());
    }

    private void createLoginCookies(HttpServletResponse res, Map<String, Object> claims) {
        final int browserSessionTtl = -1;

        final String clientId = (String) claims.get(CLIENT_ID);
        CookieUtil.addCookie(res, B_COOKIE, clientId, true, browserSessionTtl);

        //TODO document this cookie as the way of exposing the username in the UI
        final String subject = (String) claims.get(DISPLAY_NAME);
        CookieUtil.addCookie(res, USER_COOKIE, subject, false, (int) JWT_TTL.toSeconds());

        final String sessionId = (String) claims.get(SESSION_ID);
        CookieUtil.addCookie(res, JWT_SESSION_COOKIE, sessionId, true, browserSessionTtl);

        final String roles = (String) claims.get(ROLES);
        if(StringUtils.containsIgnoreCase(roles, "ADMIN")) {
            CookieUtil.addCookie(res, ADMIN_COOKIE, "admin", false, (int) JWT_TTL.toSeconds());
        }
        //The plan is to use it to renew requests from client
        CookieUtil.addCookie(res,
                             SESSION_REFRESH_COUNTDOWN,
                             String.valueOf(JWT_TTL.minusMinutes(5).toMillis()),
                             false,
                             browserSessionTtl);
    }

    private SecretKey getSecretKey() {
        final byte[] secretBytes = getSecret().getSecretBytes();
        if (secretBytes == null) {
            throw new InvalidJWTDataException("Secret for jwt not provided", MISSING_JWT_SECRET);
        }
        try {
            return Keys.hmacShaKeyFor(secretBytes.clone());
        }
        finally {
            Arrays.fill(secretBytes, (byte) 0);
        }
    }

    private Secret getSecret() {
        if(secret == null) {
            synchronized (this) {
                if(secret == null) {
                    secret = Optional.ofNullable(secretService.fetchSecret(JWT_VERSION, JWT_BUS_NAME))
                                     .orElseThrow(() -> new AppSetupException("JWT secret key has not been set in DB"));
                }
            }
        }
        return secret;
    }

    /**
     * Gets the clientId. Before client is logged in, the fcookie and APIKey apply.
     * The bcookie is used as the client id ONLY after the client is logged-in is
     * @return
     */
    private static String getClientId(final boolean isLoggedIn) {
        final RequestMetadata requestMetadata = RequestMetadataProvider.getClientInfo();
        if (requestMetadata.isMachineClient()) {
            return requestMetadata.getApiKey();
        }
        if (StringUtils.isNotBlank(requestMetadata.getFingerprintCookie())) {
            return requestMetadata.getFingerprintCookie();
        }
        String msg = "Missing pre-login client identifier. Expected an 'fcookie' or 'apikey'";
        if (isLoggedIn) {
            if (StringUtils.isNotBlank(requestMetadata.getBrowserCookie())) {
                return requestMetadata.getBrowserCookie();
            }
            msg = "Missing client identifier. Expected 'bcookie or 'apikey'";
        }

        throw new MissingClientIdException(msg);
    }


    /**
     * This creates a JWS. The assumption is that HTTPS is used so no need for a JWE token.
     * @param claims
     * @return
     */
    private  String generateTokenFromClaims(Map<String, Object> claims) {
        final Date exp = new Date(ClockProvider.getClock().millis() + JWT_TTL.toMillis());
        return Jwts.builder()
                   .claims(claims)
                   .expiration(exp)
                   .signWith(getSecretKey()) //the secretKey already recommends the algorithm
                   .compact();
    }

    Map<String, Object> toClaims(Principal principal, Long refreshToken, boolean isLoggedIn, String seed) throws InvalidJWTDataException {
        final Map<String, Object> claims = new HashMap<>();
        claims.put(Claims.SUBJECT, principal.getUsername());
        claims.put(ROLES, toJson(principal.getAuthorities(), "Attempt to serialize roles"));
        //Claims.ISSUED_AT (iat) has to be a numeric value. see https://tools.ietf.org/html/rfc7519#section-4.1.6
        //store issuedAt in seconds. JJWT lib expects this value to be in secs NOT millisecs.
        claims.put(Claims.ISSUED_AT, Date.from(Instant.now(ClockProvider.getClock())).getTime() / 1000);
        claims.put(DB_REFRESH_TOKEN, refreshToken);
        if (StringUtils.isNotBlank(RequestMetadataProvider.getClientInfo().getUserAgent())) {
            //The UA is too long. Just store the hash
            final String uaHash = String.valueOf(RequestMetadataProvider.getClientInfo().getUserAgent().hashCode());
            claims.put(USER_AGENT_HASH, uaHash);
        }
        claims.put(SESSION_ID, RequestMetadataProvider.getClientInfo().getSessionId());
        claims.put(CLIENT_ID, getClientId(isLoggedIn));
        claims.put(DISPLAY_NAME, principal.getDisplayName());
        claims.put(GENDER, String.valueOf(principal.getGender()));
        claims.put(BUS_ID, principal.getBusinessId());
        claims.put(OTP_ENABLED, principal.isOtpEnabled());
        if (StringUtils.isNotBlank(seed)) {
            claims.put(OPF_SEED, seed);
        }
        return claims;
    }

    private static String toJson(Object object, String context) throws InvalidJWTDataException {
        try {
            return MAPPER.writeValueAsString(object);
        }
        catch (JsonProcessingException e) {
            throw new InvalidJWTDataException(context, e, FAULTY_JWT_ROLES_CLAIMS);
        }
    }

    private Long extractDbRefreshToken(final HttpServletRequest request) throws InvalidJWTDataException {
        final String token = CookieUtil.getCookieValue(request, JWT_COOKIE_NAME);
        if (StringUtils.isNotBlank(token)) {
            final Claims claims = extractClaims(token);
            return Instant.ofEpochMilli(((Number) claims.get(DB_REFRESH_TOKEN)).longValue()).toEpochMilli();
        }
        return null;
    }

    private static Set<SimpleGrantedAuthority> getRoles(final Claims claims) throws InvalidJWTDataException {
        final String roles = (String) claims.get("roles");
        try {
            return MAPPER.readValue(roles, new TypeReference<>() {
            });
        }
        catch (IOException e) {
            throw new InvalidJWTDataException(format("Attempt to read roles from claims failed. Got %s", roles),
                                              e,
                                              FAULTY_JWT_ROLES_CLAIMS);
        }
    }

    private Optional<Claims> extractClaimsSilently(final HttpServletRequest request) {
        final String token = CookieUtil.getCookieValue(request, JWT_COOKIE_NAME);
        return extractClaimsFromTokenSilently(token);
    }

    private Optional<Claims> extractClaimsFromTokenSilently(String token) {
        if (StringUtils.isNotBlank(token)) {
            try {
                return Optional.ofNullable(extractClaims(token));
            }
            catch (Exception e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    /**
     * Extract claims from the jws
     * Does the actual extraction of claims from token but lets various exceptions bubble up
     * @param token string from which claims are extracted. This is a signed JWT. So JWS
     * @return extracted claims
     */
    Claims extractClaims(final String token) {
        try {
            return Jwts.parser().verifyWith(getSecretKey()).build().parse(token).accept(Jws.CLAIMS).getPayload();
        }
        catch (SignatureException | MalformedJwtException | SecurityException | IllegalArgumentException e) {
            throw new InvalidJWTDataException("The Jwt token could not be parsed.", e, JWT_PARSE_ERROR);
        }
        catch (ExpiredJwtException e) {
            throw new JWTExpiredException("The JWT has expired", e, JWT_EXPIRED);
        }
    }

}
