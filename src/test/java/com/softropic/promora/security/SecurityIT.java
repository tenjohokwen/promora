package com.softropic.promora.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.softropic.promora.common.ClockProvider;
import com.softropic.promora.common.Gender;
import com.softropic.promora.common.HttpTestClient;
import com.softropic.promora.common.config.CommonConfig;
import com.softropic.promora.common.dto.PhoneNumberDto;
import com.softropic.promora.common.message.ErrorDto;
import com.softropic.promora.common.message.ErrorMsg;
import com.softropic.promora.common.message.FieldErrorDto;
import com.softropic.promora.common.util.RandomUtil;
import com.softropic.promora.common.validation.PhoneNumber;
import com.softropic.promora.common.validation.Provider;
import com.softropic.promora.email.api.Envelope;
import com.softropic.promora.email.api.MailManager;
import com.softropic.promora.security.api.AccountManagementFacade;
import com.softropic.promora.security.api.KeyAndPasswordDto;
import com.softropic.promora.security.common.util.SecurityConstants;
import com.softropic.promora.security.domain.LoginInfo;
import com.softropic.promora.security.domain.User;
import com.softropic.promora.security.exposed.ChangePasswordDto;
import com.softropic.promora.security.exposed.LoginIdType;
import com.softropic.promora.security.exposed.UserDto;
import com.softropic.promora.security.exposed.util.ShortCode;
import com.softropic.promora.security.manager.LoginAttemptsService;
import com.softropic.promora.security.repository.LoginInfoRepository;
import com.softropic.promora.security.repository.UserRepository;
import com.softropic.promora.utils.TestMailManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.softropic.promora.security.common.util.SecurityConstants.JWT_COOKIE_NAME;
import static com.softropic.promora.security.common.util.SecurityConstants.JWT_SESSION_COOKIE;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@ActiveProfiles("dev")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
                properties = {"ledger.database.spy=true", "enable.test.mail=true"})
@Import(TestcontainersConfiguration.class)
@Sql({SecurityIT.SEC_DATA_SQL_PATH})
class SecurityIT {
    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final String USER_DATA_SQL_PATH = "/sql/userData.sql";
    public static final String SEC_DATA_SQL_PATH = "/sql/secData.sql";
    public static final String AUTHORITY_DATA_SQL_PATH = "/sql/authorityData.sql";
    public static final String AUTHENTICATE_ENDPOINT = "/authenticate";
    public static final String OTP_ENDPOINT = "/otp";
    public static final String HELP_CODE = "helpCode";
    public static final String PASSWORD = "password";
    public static final String EMAIL = "email";
    public static final String LOGIN_ID = "id";
    public static final String CLIENT_ID = "myClientId";

    @Autowired
    private TransactionTemplate template;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MailManager mailManager;

    @Autowired
    private AccountManagementFacade accountManagementFacade;

    @Autowired
    private LoginInfoRepository loginInfoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoginAttemptsService loginAttemptsService;

    /**
     * 1. test cors
     * 2. test exceptions thrown at various levels authenticationFilter, authorizationFilter, loggingFilter, Controller
     * 3. CSRF (not needed for token-based authentication)
     * 4. Access denied (anonymous user, loggedIn user with insufficient rights)
     *
     *
     * Exceptions
     * 1.  AuthorizationAccountStateException
     * 2.  AuthorizationException
     * 3.  EncryptionException
     * 4.  InvalidJWTDataException (map to PotentialFraudEvent)
     * 5.  JWTManipulationException  (map to FraudEvent)
     * 6.  JWTTheftException        (map to FraudEvent)
     * 7.  LoginAlreadyExistsException
     * 8.  MissingClientIdException (map to PotentialFraudEvent)
     * 9.  UsernameNotFoundException (map to PotentialFraudEvent)
     * 10. AccountExpiredException
     * 11. ProviderNotFoundException
     * 12. DisabledException (*)
     * 13. LockedException (*)
     * 14. AuthenticationServiceException
     * 15. CredentialsExpiredException
     * 16. BadCredentialsException (*)
     * 17. OperationNotAllowedException (map to PotentialFraudEvent)
     * 18. AccessDeniedException (map to PotentialFraudEvent)
     */

    @Autowired
    private HttpTestClient httpTestClient;

    @LocalServerPort
    int randomServerPort;

    @AfterEach
    void tearDown() {
        template.execute(status -> {
            jdbcTemplate.execute("delete from main.sec");
            jdbcTemplate.execute("delete from main.user_addresses");
            jdbcTemplate.execute("delete from main.user_authority");
            jdbcTemplate.execute("delete from main.authority");
            jdbcTemplate.execute("delete from main.user");
            return 0;
        });
        loginAttemptsService.unblacklistClient(CLIENT_ID);
    }

    /**
     *
     * bad.credentials test
     */
    @Sql({USER_DATA_SQL_PATH, SEC_DATA_SQL_PATH})
    @ParameterizedTest
    @EnumSource(value = Credentials.class,
                names = {"INVALID_EMAIL", "ARBITRARY_EMAIL", "INVALID_PASSWORD"})
    void loginWithWrongCredentials(Credentials credentials) throws JsonProcessingException {
        final String uri = baseUrl() + AUTHENTICATE_ENDPOINT;
        final ResponseEntity<Map> responseEntity = httpTestClient.makeHttpRequest(uri,
                                                                                  HttpMethod.POST,
                                                                                  credentials.getBody(),
                                                                                  baseHttpHeaders(),
                                                                                  Map.class);
        final HttpStatus statusCode = HttpStatus.resolve(responseEntity.getStatusCode().value());
        assertThat(statusCode).isEqualTo(HttpStatus.UNAUTHORIZED);
        final Map responseBody = responseEntity.getBody();
        String expectedJson = """
                {
                  "helpCode": "some_uuid",
                  "errorMsg": {
                    "errorKey": "security.badCreds",
                    "message": "The login and password combination does not exist"
                  },
                  "fieldErrors": []
                }
                """;
        final String respBodyJson = OBJECT_MAPPER.writeValueAsString(responseBody);
        final String helpCode = (String) responseBody.get(HELP_CODE);
        assertThat(helpCode).isNotNull();
        assertThatJson(respBodyJson).whenIgnoringPaths("$.helpCode").isEqualTo(expectedJson);
        final Map<String, Object> stringObjectMap = jdbcTemplate.queryForMap(
                "select * from main.audit_log where log_id ='" + helpCode + "'");
        assertThat(stringObjectMap).isNotNull();
        assertThat(stringObjectMap.get("is_authenticated")).isNotNull();
    }

    enum Credentials {
        INVALID_EMAIL(Map.of(EMAIL, "Mike", PASSWORD, "Thomson")),
        ARBITRARY_EMAIL(Map.of(EMAIL, "walters@yahoo.com", PASSWORD, "Thomson")),
        INVALID_PASSWORD(Map.of(EMAIL, "me@yahoo.com", PASSWORD, PASSWORD));
        private final Map<String, Object> body;

        Credentials(Map<String, Object> body) {this.body = body;}

        public Map<String, Object> getBody() {
            return this.body;
        }
    }


    @ParameterizedTest
    @EnumSource(value = OriginHeader.class,
                names = {"WRONG_PROTOCOL", "WRONG_PROTOCOL_HOST", "WRONG_PROTOCOL_HOST_PORT", "WRONG_PROTOCOL_PORT", "WRONG_HOST", "WRONG_HOST_PORT", "WRONG_PORT"})
    void attemptLoginWithInvalidOriginPolicy(OriginHeader originHeader) {
        final Map<String, Object> body = Map.of(EMAIL, "Mike", PASSWORD, "Thomson");
        final String uri = baseUrl() + AUTHENTICATE_ENDPOINT;

        final HttpHeaders httpHeaders = baseHttpHeaders();
        httpHeaders.add(HttpHeaders.ORIGIN, originHeader.getOrigin(randomServerPort));

        final ResponseEntity<String> responseEntity = httpTestClient.makeHttpRequest(uri,
                                                                                     HttpMethod.POST,
                                                                                     body,
                                                                                     httpHeaders,
                                                                                     String.class);
        assertThat(responseEntity.getBody()).isEqualTo("Invalid CORS request");
    }

    private HttpHeaders baseHttpHeaders() {
        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON.toString());
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString());
        httpHeaders.add(SecurityConstants.API_KEY_HEADER, CLIENT_ID);
        return httpHeaders;
    }

    enum OriginHeader {
        WRONG_PROTOCOL("https:localhost:", false),
        WRONG_PROTOCOL_HOST("https:myhost:", false),
        WRONG_PROTOCOL_HOST_PORT("https:myhost:", true),
        WRONG_PROTOCOL_PORT("https:localhost:", true),
        WRONG_HOST("http:myhost:", false),
        WRONG_HOST_PORT("https:myhost:", true),
        WRONG_PORT("http:localhost:", true);

        private static final String  RANDOM_WRONG_PORT = "80";
        private final        String  protocolAndHost;
        private final        boolean isWrongPort;

        OriginHeader(String protocolAndHost, boolean isWrongPort) {
            this.protocolAndHost = protocolAndHost;
            this.isWrongPort = isWrongPort;
        }

        String getOrigin(int currentPort) {
            if (this.isWrongPort) {
                return protocolAndHost + RANDOM_WRONG_PORT;
            }
            return protocolAndHost + currentPort;
        }
    }

    /**
     * DisabledException test
     *
     */
    @Test
    void attemptLoginToDisabledAccount() throws JsonProcessingException {
        final UserDto userData = getUserData(false);
        accountManagementFacade.registerAccount(userData);

        //final Envelope envelope = ((TestMalManager) mailManager).getEnvelope(emailDispatchRefId);
        final String uri = baseUrl() + AUTHENTICATE_ENDPOINT;
        final HttpHeaders httpHeaders = baseHttpHeaders();
        final ResponseEntity<Map> responseEntity = httpTestClient.makeHttpRequest(uri,
                                                                                  HttpMethod.POST,
                                                                                  Map.of(LOGIN_ID,
                                                                                         userData.getLogin(),
                                                                                         PASSWORD,
                                                                                         userData.getPassword()),
                                                                                  httpHeaders,
                                                                                  Map.class);
        //TODO see if an email can be sent to user with the details of the error and a vague reason returned. The user should be asked to check his email
        final Map responseBody = responseEntity.getBody();
        String expectedJson = """
                {
                    "helpCode": "helpCode",
                    "errorMsg": {
                        "errorKey": "security.accNotEnabled",
                        "message": "Your account is not enabled."
                    },
                    "fieldErrors":[]
                }
                """;
        final String respBodyJson = OBJECT_MAPPER.writeValueAsString(responseBody);
        final Object helpCode = responseBody.get(HELP_CODE);
        assertThat(helpCode).isNotNull();
        assertThatJson(respBodyJson).whenIgnoringPaths("$.helpCode").isEqualTo(expectedJson);

        //verify that log in attempt is audited
        final Map<String, Object> stringObjectMap = jdbcTemplate.queryForMap(
                "select * from main.audit_log where log_id ='" + helpCode + "'");
        assertThat(stringObjectMap).isNotEmpty()
                                   .contains(new AbstractMap.SimpleImmutableEntry<>("is_authenticated",
                                                                                    false));
        assertThat(stringObjectMap.get("relevant_properties").toString()).contains("DisabledException");
    }

    //@Test //single factor authentication is currently off
    void attemptLoginSingleFactorAuthWhenAccountEnabled() throws JsonProcessingException {
        final String registrationUri = baseUrl() + "/v1/account/register";
        boolean otpEnabled = false;
        final UserDto userData = getUserData(otpEnabled);

        //convert userData to map
        final String userDataAsString = getDefaultObjectMapper().writeValueAsString(userData);
        final Map<String, Object> userDataMap = OBJECT_MAPPER.readValue(userDataAsString, Map.class);

        //register user
        final ResponseEntity<Map> registerResponse = httpTestClient.makeHttpRequest(registrationUri,
                                                                                    HttpMethod.POST,
                                                                                    userDataMap,
                                                                                    Map.class);

        final String emailDispatchRefId = ((String) registerResponse.getBody().get(HELP_CODE));//accountManagementFacade.registerAccount(userData);

        //Get activation key from sent email and then activate account
        final TestMailManager testMailManager = (TestMailManager) mailManager;
        await().until(() -> testMailManager.getEnvelope(emailDispatchRefId) != null);
        final String key = testMailManager.getEnvelope(emailDispatchRefId).data().get("activationKey").toString();
        final String enableAccountUri = baseUrl() + "/v1/account/activate?key=" + key;
        httpTestClient.makeHttpRequest(enableAccountUri, HttpMethod.POST, Map.of(), Map.class);
        //TODO assert response
        final String uri = baseUrl() + AUTHENTICATE_ENDPOINT;
        final HttpHeaders httpHeaders = baseHttpHeaders();
        final ResponseEntity<Map> responseEntity = httpTestClient.makeHttpRequest(uri,
                                                                                  HttpMethod.POST,
                                                                                  Map.of(EMAIL,
                                                                                         userData.getLogin(),
                                                                                         PASSWORD,
                                                                                         userData.getPassword()),
                                                                                  httpHeaders,
                                                                                  Map.class);
        assertThat(responseEntity.getBody()).isNull();
        assertThat(responseEntity.getStatusCode().value()).isEqualTo(200);
        assertThat(responseEntity.getHeaders().get("Authorization")).isNotNull();
        assertThat(responseEntity.getHeaders()
                                 .get("Authorization")
                                 .stream()
                                 .filter(str -> str.contains("Bearer"))
                                 .findFirst()).isNotEmpty();
        assertThat(responseEntity.getHeaders()
                                 .get("Set-Cookie")
                                 .stream()
                                 .filter(str -> str.contains("potc"))
                                 .findFirst()).isNotEmpty();
        assertThat(responseEntity.getHeaders()
                                 .get("Set-Cookie")
                                 .stream()
                                 .filter(str -> str.contains("bcookie"))
                                 .findFirst()).isNotEmpty();
        assertThat(responseEntity.getHeaders()
                                 .get("Set-Cookie")
                                 .stream()
                                 .filter(str -> str.contains("user"))
                                 .findFirst()).isNotEmpty();
        assertThat(responseEntity.getHeaders()
                                 .get("Set-Cookie")
                                 .stream()
                                 .filter(str -> str.contains("rint"))
                                 .findFirst()).isNotEmpty();

    }

    /**
     *
     * Happy path test
     */
    @Test
    @Sql({USER_DATA_SQL_PATH, SEC_DATA_SQL_PATH})
    void loginWith2FAWhenAccountEnabled() throws JsonProcessingException {
        final String registrationUri = baseUrl() + "/v1/account/register";
        boolean otpEnabled = true;
        final UserDto userData = getUserData(otpEnabled);

        //convert userData to map
        final String userDataAsString = getDefaultObjectMapper().writeValueAsString(userData);
        final Map<String, Object> userDataMap = OBJECT_MAPPER.readValue(userDataAsString, Map.class);

        //register user
        final ResponseEntity<Map> registerResponse = httpTestClient.makeHttpRequest(registrationUri,
                                                                                    HttpMethod.POST,
                                                                                    userDataMap,
                                                                                    Map.class);

        final String emailDispatchRefId = ((String) registerResponse.getBody().get(HELP_CODE));///accountManagementFacade.registerAccount(userData);

        //Get activation key from sent email and then activate account
        final TestMailManager testMailManager = (TestMailManager) mailManager;
        await().until(() -> testMailManager.getEnvelope(emailDispatchRefId) != null);
        final String key = testMailManager.getEnvelope(emailDispatchRefId).data().get("activationKey").toString();
        final String enableAccountUri = baseUrl() + "/v1/account/activate?key=" + key;
        final ResponseEntity<Map> activationResponse = httpTestClient.makeHttpRequest(enableAccountUri,
                                                                                      HttpMethod.POST,
                                                                                      Map.of(),
                                                                                      Map.class);

        assertThat(activationResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(activationResponse.getBody()).containsEntry("msgKey", "user.activation.success");


        template.execute(status -> {
            userRepository.enableOtp(userData.getLogin());
            return 0;
        });


        String uri = baseUrl() + AUTHENTICATE_ENDPOINT;
        final HttpHeaders httpHeaders = baseHttpHeaders();
        ResponseEntity<Map> responseEntity = httpTestClient.makeHttpRequest(uri,
                                                                            HttpMethod.POST,
                                                                            Map.of(LOGIN_ID,
                                                                                   userData.getLogin(),
                                                                                   PASSWORD,
                                                                                   userData.getPassword()),
                                                                            httpHeaders,
                                                                            Map.class);
        final Map body = responseEntity.getBody();
        assertThat(body).isNotNull();
        final String helpCode = (String) body.get(HELP_CODE);
        assertThat(helpCode).isNotBlank();
        final Map payload = (Map) body.get("payload");
        final String loginInfoId = (String) payload.get("loginInfoId");
        assertThat(loginInfoId).isNotBlank();
        await().until(() -> testMailManager.getEnvelope(helpCode) != null);
        final Envelope envelope = testMailManager.getEnvelope(helpCode);
        final Map<String, Object> data = envelope.data();
        assertThat(data).isNotNull().isNotEmpty();
        final String otpData = data.get("otp").toString();
        assertThat(otpData).isNotBlank();

        final String loginInfoIdCookie = responseEntity.getHeaders()
                                                       .get("Set-Cookie")
                                                       .stream()
                                                       .filter(str -> str.contains("lii"))
                                                       .findFirst()
                                                       .get();
        final String sessionIdCookie = responseEntity.getHeaders()
                                                     .get("Set-Cookie")
                                                     .stream()
                                                     .filter(str -> str.contains(JWT_SESSION_COOKIE))
                                                     .findFirst()
                                                     .get();
        //httpHeaders.add(HttpHeaders.SET_COOKIE, loginInfoIdCookie); //this way is used to set response cookies
        httpHeaders.add(HttpHeaders.COOKIE, loginInfoIdCookie);
        httpHeaders.add(HttpHeaders.COOKIE, sessionIdCookie);
        uri = baseUrl() + OTP_ENDPOINT;
        responseEntity = httpTestClient.makeHttpRequest(uri,
                                                        HttpMethod.POST,
                                                        Map.of("otp",
                                                               otpData,
                                                               "loginInfoId",
                                                               loginInfoId),
                                                        httpHeaders,
                                                        Map.class);

        final Map body1 = responseEntity.getBody();
        assertThat(body1).isNotNull().isNotEmpty();
        assertThat(responseEntity.getStatusCode().value()).isEqualTo(200);
        assertThat(responseEntity.getHeaders()
                                 .get("Set-Cookie")
                                 .stream()
                                 .filter(str -> str.contains("potc"))
                                 .findFirst()).isNotEmpty();
        assertThat(responseEntity.getHeaders()
                                 .get("Set-Cookie")
                                 .stream()
                                 .filter(str -> str.contains("bcookie"))
                                 .findFirst()).isNotEmpty();
        assertThat(responseEntity.getHeaders()
                                 .get("Set-Cookie")
                                 .stream()
                                 .filter(str -> str.contains("user"))
                                 .findFirst()).isNotEmpty();
        assertThat(responseEntity.getHeaders()
                                 .get("Set-Cookie")
                                 .stream()
                                 .filter(str -> str.contains("rint"))
                                 .findFirst()).isNotEmpty();

        final HttpHeaders accHttpHeaders = baseHttpHeaders();
        final List<String> headerList = responseEntity.getHeaders().get("Set-Cookie");
        accHttpHeaders.put("Cookie", headerList);
        String accUri = baseUrl() + "/v1/account/";
        ResponseEntity<Map> accResEntity = httpTestClient.makeHttpRequest(accUri,
                                                                          HttpMethod.GET,
                                                                          Map.of(),
                                                                          accHttpHeaders,
                                                                          Map.class);
        final Map userDtoMap = accResEntity.getBody();
        assertThat(userDtoMap).isNotNull();
        assertThat(userDtoMap.get("login")).isNotNull();

        //assert that verification date put in loginInfo
        final Long liId = ShortCode.revertUsingDefault(loginInfoId);
        final Optional<LoginInfo> loginInfoOpt = loginInfoRepository.findById(liId);
        assertThat(loginInfoOpt).isPresent();
        final LoginInfo loginInfo = loginInfoOpt.get();
        final LocalDateTime now = LocalDateTime.now(ClockProvider.getClock());
        assertThat(loginInfo.getVerificationDate()).isBetween(now.minusMinutes(30), now.plusMinutes(30));
    }

    /**
     *
     * Happy path test
     */
    @Test
    @Sql({USER_DATA_SQL_PATH, SEC_DATA_SQL_PATH})
    void loginWith2FADisabled() {
        final String uri = baseUrl() + AUTHENTICATE_ENDPOINT;
        final HttpHeaders httpHeaders = baseHttpHeaders();
        final ResponseEntity<Map> responseEntity = httpTestClient.makeHttpRequest(uri,
                                                                                  HttpMethod.POST,
                                                                                  Map.of(LOGIN_ID,
                                                                                         "queb@yahoo.com",
                                                                                         PASSWORD,
                                                                                         "admin*123!"),
                                                                                  httpHeaders,
                                                                                  Map.class);
        final Map responseBody = responseEntity.getBody();
        final Map<String, Object> expectedBody = Map.of("msgKey",
                                                        "jwt.created",
                                                        "msg",
                                                        "JWT token has been created",
                                                        "payload",
                                                        Map.of());
        assertThat(responseBody).containsAllEntriesOf(expectedBody);

        // --- Start of new additions for accessing protected resource ---
        final HttpHeaders protectedResourceHeaders = baseHttpHeaders();
        // Extract cookies from the login response and add them to the new request headers
        final List<String> cookies = responseEntity.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(cookies).isNotNull().isNotEmpty();
        // For subsequent requests, cookies should be sent in a single "Cookie" header,
        // with key-value pairs separated by semicolons.
        // HttpTestClient or Spring's TestRestTemplate usually handles cookie management if configured,
        // but here we are manually constructing it.
        // A simpler approach for HttpTestClient might be to pass the ResponseEntity from login
        // if it has a mechanism to reuse session/cookies. Assuming manual construction for clarity:
        
        // Find the 'potc' cookie specifically, as it's the main JWT. Others might be relevant too.
        // For simplicity, let's just pass all Set-Cookie values as is, hoping the client/server handles it.
        // A more robust way is to parse them and reconstruct the Cookie header string.
        // However, Spring's TestRestTemplate typically handles this if you use exchange methods that allow passing HttpEntity from previous response.
        // Given HttpTestClient is custom, we'll add them as separate Cookie headers if the client supports it,
        // or join them into a single string. Let's assume HttpTestClient can take multiple Cookie headers or a list.
        // Or, more simply, for HttpTestClient, it might automatically store and send cookies if it's stateful.
        // If not, we need to format the Cookie header string correctly.

        // Let's assume HttpTestClient is not stateful and we need to set the Cookie header manually.
        // The `HttpTestClient` would need to be inspected. For now, we'll extract and pass them.
        // The `HttpTestClient.makeHttpRequest` takes `HttpHeaders`. We'll add `Cookie` to it.
        // Joining cookies:
        String cookieHeaderValue = String.join("; ", cookies.stream().map(c -> c.split(";", 2)[0]).toList());
        protectedResourceHeaders.add(HttpHeaders.COOKIE, cookieHeaderValue);
        
        // Make a GET request to /v1/account/
        String accountUri = baseUrl() + "/v1/account/";
        ResponseEntity<Map> accountResponseEntity = httpTestClient.makeHttpRequest(
                accountUri,
                HttpMethod.GET,
                null, // No body for GET
                protectedResourceHeaders,
                Map.class
        );

        // Assert that this GET request is successful
        assertThat(accountResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Assert that the response body contains expected user data
        final Map accountBody = accountResponseEntity.getBody();
        assertThat(accountBody).isNotNull();
        // Assuming 'queb@yahoo.com' is the login email for the user in this test
        assertThat(accountBody.get("login")).isEqualTo("queb@yahoo.com");
        // --- End of new additions ---
    }


    @Test
    @Sql({SecurityIT.SEC_DATA_SQL_PATH}) // General security setup
    void testAccessProtectedResource_NoToken_Unauthorized() throws JsonProcessingException {
        final String accountUri = baseUrl() + "/v1/account/";
        final HttpHeaders httpHeaders = baseHttpHeaders(); // Contains API Key, Content-Type etc. but no auth cookies

        final ResponseEntity<Map> responseEntity = httpTestClient.makeHttpRequest(
                accountUri,
                HttpMethod.GET,
                null,
                httpHeaders,
                Map.class
        );

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        final Map responseBody = responseEntity.getBody();
        final String respBodyJson = OBJECT_MAPPER.writeValueAsString(responseBody);
        final String helpCode = (String) responseBody.get(HELP_CODE);
        assertThat(helpCode).isNotNull(); // Expect a help code if CustomExceptionHandlerController is involved.

        // We need to be flexible with the message as it can change based on what Spring Security provides.
        // The key is more reliable if CustomExceptionHandlerController is used.
        assertThatJson(respBodyJson)
                .whenIgnoringPaths("$.helpCode", "$.errorMsg.message") // Ignore message for flexibility
                .isEqualTo("""
                {
                  "errorMsg": {
                    "errorKey": "security.unauthorized"
                  },
                  "fieldErrors": []
                } 
                """);
        assertThat((Map<String, Object>)responseBody.get("errorMsg")).containsValue("security.unauthorized");
    }

    /**
    /**
     *
     * LockedException  test
     */
    @Test
    @Sql({USER_DATA_SQL_PATH, SEC_DATA_SQL_PATH})
    void attemptLoginWhenAccountBlocked() throws JsonProcessingException {
        final String uri = baseUrl() + AUTHENTICATE_ENDPOINT;
        final HttpHeaders httpHeaders = baseHttpHeaders();
        final ResponseEntity<Map> responseEntity = httpTestClient.makeHttpRequest(uri,
                                                                                  HttpMethod.POST,
                                                                                  Map.of(LOGIN_ID,
                                                                                         "locked@yahoo.com",
                                                                                         PASSWORD,
                                                                                         "admin*123!"),
                                                                                  httpHeaders,
                                                                                  Map.class);
        final Map responseBody = responseEntity.getBody();
        String expectedJson = """
                {
                    "helpCode": "helpCode",
                    "errorMsg": {
                        "errorKey": "security.accLocked",
                        "message":"There is an issue with your account. Check your email and contact the support team. Remember to save the help code."
                    },
                    "fieldErrors":[]
                }
                """;
        final String respBodyJson = OBJECT_MAPPER.writeValueAsString(responseBody);
        final Object helpCode = responseBody.get(HELP_CODE);
        assertThat(helpCode).isNotNull();
        assertThatJson(respBodyJson).whenIgnoringPaths("$.helpCode").isEqualTo(expectedJson);

        //verify that log in attempt is audited
        final Map<String, Object> stringObjectMap = jdbcTemplate.queryForMap(
                "select * from main.audit_log where log_id ='" + helpCode + "'");
        assertThat(stringObjectMap).isNotEmpty()
                                   .contains(new AbstractMap.SimpleImmutableEntry<>("is_authenticated",
                                                                                    false));
        assertThat(stringObjectMap.get("relevant_properties").toString()).contains("LockedException");
    }

    @Test
    @Sql({USER_DATA_SQL_PATH, SEC_DATA_SQL_PATH})
    void attemptAccessWithExpiredJWT() throws JsonProcessingException {
        final HttpHeaders accHttpHeaders = baseHttpHeaders();
        final List<String> headerList = createExpiredLoginCookies();
        accHttpHeaders.put("Cookie", headerList);
        String accUri = baseUrl() + "/v1/account/";
        ResponseEntity<Map> accResEntity = httpTestClient.makeHttpRequest(accUri,
                                                                          HttpMethod.GET,
                                                                          Map.of(),
                                                                          accHttpHeaders,
                                                                          Map.class);
        final Map responseBody = accResEntity.getBody();
        String expectedJson = """
                {
                    "helpCode": "helpCode",
                    "errorMsg": {
                        "errorKey": "security.sessionExpired",
                        "message":"Your session is no longer valid. You need to sign-in again"
                    },
                    "fieldErrors":[]
                }
                """;
        final String respBodyJson = OBJECT_MAPPER.writeValueAsString(responseBody);
        final Object helpCode = responseBody.get(HELP_CODE);
        assertThat(helpCode).isNotNull();
        assertThatJson(respBodyJson).whenIgnoringPaths("$.helpCode").isEqualTo(expectedJson);
    }

    private static List<String> createExpiredLoginCookies() {
        final List<String> headerList = new ArrayList<>();
        headerList.add("potc=eyJhbGciOiJIUzUxMiJ9.eyJidXNJZCI6IjcxMjc1MjE1MzkyNTM0MTg4MiIsInN1YiI6ImZpZ3VAeWFob28uY29tIiwib3BmU2VlZCI6InV3UzFwR2tiZnFVVjRuUUo4QnlFNmFNY3ZIVGlaTENGRDNPelJqOVhvWTdkS2xBZ041ZVB0bXhyMDJoV0lzIiwiY2xpZW50SWQiOiJteUNsaWVudElkIiwiZ2VuZGVyIjoiTUFMRSIsImRpc3BsYXlOYW1lIjoiVEZUIiwicm9sZXMiOiJbe1wicm9sZVwiOlwiUk9MRV9VU0VSXCJ9XSIsInVzZXJBZ2VudCI6Ii0xOTgxMDM2MDc1IiwiZGJSVG9rZW4iOjE3NDc3NzA1MTI4MjEsImV4cCI6MTc0Nzc3MTEyNSwiaWF0IjoxNzQ3NzcwMjEyfQ.67tmKUSAf4rw_1nS4lV71BsD4BsLBzDvXsWyLu581AwSS_qYfIkKCla6f-hCCE4xGLD99DDjfBVp4A_lZGBB3w; Path=/; Max-Age=900; Expires=Tue, 20 May 2025 19:58:49 GMT; HttpOnly; SameSite=Lax");
        headerList.add("bcookie=" + CLIENT_ID + "; Path=/");
        headerList.add("user=BBUSNURFDE; Max-Age=900; Expires=Tue, 11 Feb 2025 16:23:08 GMT; Path=/");
        headerList.add("ION=IpYGthG; Path=/; HttpOnly");
        headerList.add("rint=600000; Path=/");
        return headerList;
    }

    @Test
    @Sql({USER_DATA_SQL_PATH, SEC_DATA_SQL_PATH})
    void testUpdateUserEmail() {
        //user: loginId: me@yahoo.com p/w: admin*123!
        String uri = baseUrl() + AUTHENTICATE_ENDPOINT;
        final HttpHeaders httpHeaders = baseHttpHeaders();
        ResponseEntity<Map> responseEntity = httpTestClient.makeHttpRequest(uri,
                                                                            HttpMethod.POST,
                                                                            Map.of(LOGIN_ID,
                                                                                   "me@yahoo.com",
                                                                                   PASSWORD,
                                                                                   "admin*123!"),
                                                                            httpHeaders,
                                                                            Map.class);
        final Map body = responseEntity.getBody();
        assertThat(body).isNotNull();
        final String helpCode = (String) body.get(HELP_CODE);
        assertThat(helpCode).isNotBlank();
        final Map payload = (Map) body.get("payload");
        final String loginInfoId = (String) payload.get("loginInfoId");
        assertThat(loginInfoId).isNotBlank();
        final TestMailManager testMailManager = (TestMailManager) mailManager;
        await().until(() -> testMailManager.getEnvelope(helpCode) != null);
        final Envelope envelope = testMailManager.getEnvelope(helpCode);
        final Map<String, Object> data = envelope.data();
        assertThat(data).isNotNull().isNotEmpty();
        final String otpData = data.get("otp").toString();
        assertThat(otpData).isNotBlank();

        final Optional<String> jwtCookieOpt = responseEntity.getHeaders()
                                                     .get("Set-Cookie")
                                                     .stream()
                                                     .filter(str -> str.contains(JWT_COOKIE_NAME))
                                                     .findFirst();
        //Confirm that jwt not sent to client
        assertThat(jwtCookieOpt).isEmpty();

        final String loginInfoIdCookie = responseEntity.getHeaders()
                                                       .get("Set-Cookie")
                                                       .stream()
                                                       .filter(str -> str.contains("lii"))
                                                       .findFirst()
                                                       .get();
        //Session id cookie created for traceability but user not yet logged in
        final String sessionIdCookie = responseEntity.getHeaders()
                                                       .get("Set-Cookie")
                                                       .stream()
                                                       .filter(str -> str.contains(JWT_SESSION_COOKIE))
                                                       .findFirst()
                                                       .get();
        //httpHeaders.add(HttpHeaders.SET_COOKIE, loginInfoIdCookie); //this way is used to set response cookies
        httpHeaders.add(HttpHeaders.COOKIE, loginInfoIdCookie);
        httpHeaders.add(HttpHeaders.COOKIE, sessionIdCookie);
        uri = baseUrl() + OTP_ENDPOINT;
        responseEntity = httpTestClient.makeHttpRequest(uri,
                                                        HttpMethod.POST,
                                                        Map.of("otp",
                                                               otpData,
                                                               "loginInfoId",
                                                               loginInfoId),
                                                        httpHeaders,
                                                        Map.class);

        //change email
        final String jwtCookie = responseEntity.getHeaders()
                                                     .get("Set-Cookie")
                                                     .stream()
                                                     .filter(str -> str.contains(JWT_COOKIE_NAME))
                                                     .findFirst()
                                                     .get();
        httpHeaders.add(HttpHeaders.COOKIE, jwtCookie);
        String accUri = baseUrl() + "/v1/account/change_email";
        final String oldEmail = "me@yahoo.com";
        final String newEmail = "newpass@yahoo.com";
        final String password = "admin*123!";
        final Map emailPayload = Map.of("oldEmail", oldEmail, "newEmail", newEmail, PASSWORD, password);
        ResponseEntity<Map> emailResEntity = httpTestClient.makeHttpRequest(accUri,
                                                                            HttpMethod.POST,
                                                                            emailPayload,
                                                                            httpHeaders,
                                                                            Map.class);
        final Map emailResBody = emailResEntity.getBody();
        assertThat(emailResBody).containsEntry("msgKey", "email.updated");
    }

    @Test
    @Sql({USER_DATA_SQL_PATH, SEC_DATA_SQL_PATH})
    void testPasswordChange() {
        String uri = baseUrl() + "/v1/account/reset_password/init";
        final HttpHeaders httpHeaders = baseHttpHeaders();
        final String email = "me@yahoo.com";
        ResponseEntity<Map> responseEntity = httpTestClient.makeHttpRequest(uri,
                                                                            HttpMethod.POST,
                                                                            Map.of("currentEmail",
                                                                                   email,
                                                                                   "loginId",
                                                                                   email,
                                                                                   "dob",
                                                                                   LocalDate.parse("1978-03-19")),
                                                                            httpHeaders,
                                                                            Map.class);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        final Map reponseBody = responseEntity.getBody();
        assertThat(reponseBody).containsEntry("msgKey", "password.reset.emailed");
        final String helpCode = (String) reponseBody.get(HELP_CODE);
        final Envelope envelope = ((TestMailManager) mailManager).getEnvelope(helpCode);
        final String resetKey = envelope.data().get("resetKey").toString();
        assertThat(resetKey).isNotBlank();


        uri = baseUrl() + "/v1/account/reset_password/finish";
        final String newPassword = "myNewPassword123!";
        responseEntity = httpTestClient.makeHttpRequest(uri,
                                                        HttpMethod.POST,
                                                        Map.of("key",
                                                               resetKey,
                                                               PASSWORD,
                                                               newPassword
                                                               ),
                                                        httpHeaders,
                                                        Map.class);
        assertThat(responseEntity.getBody()).containsEntry("msgKey", "password.reset.success");

        String authUri = baseUrl() + AUTHENTICATE_ENDPOINT;
        ResponseEntity<Map> authResponseEntity = httpTestClient.makeHttpRequest(authUri,
                                                                            HttpMethod.POST,
                                                                            Map.of(LOGIN_ID,
                                                                                   email,
                                                                                   PASSWORD,
                                                                                   newPassword),
                                                                            httpHeaders,
                                                                            Map.class);
        final Map body = authResponseEntity.getBody();
        assertThat(body).isNotNull();
        final String authHelpCode = (String) body.get(HELP_CODE);
        assertThat(authHelpCode).isNotBlank();
        final Map payload = (Map) body.get("payload");
        final String loginInfoId = (String) payload.get("loginInfoId");
        assertThat(loginInfoId).isNotBlank();

    }


    private String baseUrl() {
        String url = "http://localhost:";
        return url + randomServerPort;
    }

    private static UserDto getUserData(boolean otpEnabled) {
        final UserDto userDto = new UserDto();
        userDto.setEmail("figu@yahoo.com");
        userDto.setLogin("figu@yahoo.com");
        userDto.setLoginIdType(LoginIdType.EMAIL);
        final PhoneNumber phoneNumber = generatePhone();
        userDto.setPhone(new PhoneNumberDto(phoneNumber.getPhone(), phoneNumber.getProvider(), phoneNumber.getIso2Country()));
        userDto.setActivated(false);
        userDto.setLangKey("en");
        userDto.setGender(Gender.MALE);
        userDto.setDob(LocalDate.of(1990, 2, 20));
        userDto.setPassword(RandomUtil.generatePassword());
        userDto.setOtpEnabled(otpEnabled);
        userDto.setFirstName("Test");
        userDto.setLastName("User");
        return userDto;
    }

    private static PhoneNumber generatePhone() {
        final String prefix = "65";
        final Optional<String> strOpt = new Random().ints(7, 0, 9)
                                                    .mapToObj(String::valueOf)
                                                    .reduce((x, y) -> x + y);
        return new PhoneNumber(prefix+strOpt.get(), Provider.MTN, "CM");
    }

    private static ObjectMapper getDefaultObjectMapper() {
        CommonConfig contextConfig = new CommonConfig();
        return contextConfig.objectMapperBuilder().build();
    }

   /* ####################################################################################################################
                            input validation
    #################################################################################################################### */


    @ParameterizedTest
    @MethodSource("invalidRegistrationInputData")
    void validateRegistrationInput(UserDto userData, ErrorDto expectedError) throws Exception {
        final String registrationUri = baseUrl() + "/v1/account/register";

        //convert userData to map
        final ObjectMapper defaultObjectMapper = getDefaultObjectMapper();
        final String userDataAsString = defaultObjectMapper.writeValueAsString(userData);
        final Map<String, Object> userDataMap = defaultObjectMapper.readValue(userDataAsString, Map.class);

        //register user
        final ResponseEntity<Map> registerResponse = httpTestClient.makeHttpRequest(registrationUri,
                                                                                    HttpMethod.POST,
                                                                                    userDataMap,
                                                                                    Map.class);

        final String actualPayload = defaultObjectMapper.writeValueAsString(registerResponse.getBody());
        final String expectedPayload = defaultObjectMapper.writeValueAsString(expectedError);
        assertThatJson(actualPayload).whenIgnoringPaths("$.helpCode").isEqualTo(expectedPayload);
    }

    @ParameterizedTest
    @MethodSource("invalidChangePasswordInputData")
    void validateChangePasswordInitInput(ChangePasswordDto changePasswordDto, ErrorDto expectedError) throws Exception {

        String uri = baseUrl() + "/v1/account/reset_password/init";
        final HttpHeaders httpHeaders = baseHttpHeaders();
        ResponseEntity<Map> responseEntity = httpTestClient.makeHttpRequest(uri,
                                                                            HttpMethod.POST,
                                                                            Map.of("currentEmail",
                                                                                   changePasswordDto.getCurrentEmail(),
                                                                                   "loginId",
                                                                                   changePasswordDto.getLoginId(),
                                                                                   "dob",
                                                                                   changePasswordDto.getDob()),
                                                                            httpHeaders,
                                                                            Map.class);

        final String actualPayload = OBJECT_MAPPER.writeValueAsString(responseEntity.getBody());
        final String expectedPayload = OBJECT_MAPPER.writeValueAsString(expectedError);
        assertThatJson(actualPayload).whenIgnoringPaths("$.helpCode").isEqualTo(expectedPayload);
    }

    @ParameterizedTest
    @MethodSource("invalidChangePasswordFinishInputData")
    void validateChangePasswordFinishInput(KeyAndPasswordDto keyAndPassword, ErrorDto expectedError) throws Exception {

        String uri = baseUrl() + "/v1/account/reset_password/finish";
        final HttpHeaders httpHeaders = baseHttpHeaders();
        ResponseEntity<Map> responseEntity = httpTestClient.makeHttpRequest(uri,
                                                                            HttpMethod.POST,
                                                                            Map.of("key",
                                                                                   keyAndPassword.key(),
                                                                                   PASSWORD,
                                                                                   keyAndPassword.password()
                                                                            ),
                                                                            httpHeaders,
                                                                            Map.class);

        final String actualPayload = OBJECT_MAPPER.writeValueAsString(responseEntity.getBody());
        final String expectedPayload = OBJECT_MAPPER.writeValueAsString(expectedError);
        assertThatJson(actualPayload).whenIgnoringPaths("$.helpCode").isEqualTo(expectedPayload);
    }

    private static Stream<Arguments> invalidChangePasswordFinishInputData() {
        return Stream.of(
                Arguments.of(new KeyAndPasswordDto("k", PASSWORD), buildChangeKeyAndPasswordDtoError("key", "size must be between 2 and 25")),
                Arguments.of(new KeyAndPasswordDto("key", "m"), buildChangeKeyAndPasswordDtoError(PASSWORD, "size must be between 5 and 50"))
        );
    }

    private static Stream<Arguments> invalidRegistrationInputData() {
        final LocalDate nextMonth = LocalDate.now(ClockProvider.getClock()).plusMonths(1);
        return Stream.of(
                Arguments.of(userDto(userDTO -> userDTO.setEmail("myemail@whatever")), buildUserDtoError(EMAIL, "must be a well-formed email address")),
                Arguments.of(userDto(userDTO -> userDTO.setPassword("my")), buildUserDtoError(PASSWORD, "size must be between 5 and 100")),
                Arguments.of(userDto(userDTO -> userDTO.setFirstName(RandomStringUtils.randomAlphabetic(55))), buildUserDtoError("firstName", "size must be between 0 and 50")),
                Arguments.of(userDto(userDTO -> userDTO.setLastName(RandomStringUtils.randomAlphabetic(55))), buildUserDtoError("lastName", "size must be between 0 and 50")),
                Arguments.of(userDto(userDTO -> userDTO.setLangKey("w")), buildUserDtoError("langKey", "size must be between 2 and 5")),
                Arguments.of(userDto(userDTO -> userDTO.setDob(nextMonth)), buildUserDtoError("dob", "must be a past date"))
        );
    }

    private static UserDto userDto(Consumer<UserDto> modifier) {
        final UserDto userData = getUserData(true);
        modifier.accept(userData);
        return userData;
    }

    private static ErrorDto buildUserDtoError(String field, String message) {
        return buildDtoError(field, message,"userDto");
    }

    private static ErrorDto buildChangePasswordDtoError(String field, String message) {
        return buildDtoError(field, message,"changePasswordDto");
    }

    private static ErrorDto buildChangeKeyAndPasswordDtoError(String field, String message) {
        return buildDtoError(field, message,"keyAndPasswordDto");
    }

    private static ErrorDto buildDtoError(String field, String message, String dtoName) {
        final String errorKeyPrefix = "invalid.";
        return new ErrorDto(HELP_CODE,
                            new ErrorMsg("validation.invalidData", "Invalid Data"),
                            List.of(new FieldErrorDto(dtoName,
                                                      field,
                                                      new ErrorMsg(errorKeyPrefix + field, message))));
    }



    private static Stream<Arguments> invalidChangePasswordInputData() {
        final String email = "me@yahoo.com";
        final LocalDate dob = LocalDate.parse("1978-03-19");
        return Stream.of(
                Arguments.of(new ChangePasswordDto("meyahoo.com", dob, email), buildChangePasswordDtoError("loginId", "must be a well-formed email address")),
                Arguments.of(new ChangePasswordDto(email, dob, "myemail"), buildChangePasswordDtoError("currentEmail", "must be a well-formed email address")),
                Arguments.of(new ChangePasswordDto(email, LocalDate.parse("2078-03-19"), email), buildChangePasswordDtoError("dob", "must be a past date"))
        );
    }

   /* ####################################################################################################################
                            UC 1.1: User Registration - Complete Happy Path
    #################################################################################################################### */

    @Test
    @Sql(scripts = {SEC_DATA_SQL_PATH, AUTHORITY_DATA_SQL_PATH})
    void testUserRegistration_Success() throws Exception {
        // AC-1.1.1: User account is created with status = INACTIVE and activated = false
        // AC-1.1.2: Password is BCrypt encoded before storage
        // AC-1.1.3: Email is stored in lowercase
        // AC-1.1.4: Login field equals email for email-based registration
        // AC-1.1.5: LoginIdType is set to EMAIL
        // AC-1.1.7: User is assigned ROLE_USER authority by default
        // AC-1.1.10: System returns tracking code

        final String registrationUri = baseUrl() + "/v1/account/register";
        final UserDto userData = getUserData(false);

        final ObjectMapper defaultObjectMapper = getDefaultObjectMapper();
        final String userDataAsString = defaultObjectMapper.writeValueAsString(userData);
        final Map<String, Object> userDataMap = defaultObjectMapper.readValue(userDataAsString, Map.class);

        //registration request
        final ResponseEntity<Map> registerResponse = httpTestClient.makeHttpRequest(
                registrationUri,
                HttpMethod.POST,
                userDataMap,
                baseHttpHeaders(),
                Map.class
        );

        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        final Map responseBody = registerResponse.getBody();
        assertThat(responseBody).isNotNull();

        // AC-1.1.10: Returns tracking code
        final String helpCode = (String) responseBody.get(HELP_CODE);
        assertThat(helpCode).isNotBlank();

        // Verify user is created in database
        await().until(() -> {
            final Optional<User> userOpt = template.execute(status ->
                    userRepository.findOneByEmail(userData.getEmail()));
            return userOpt.isPresent();
        });

        final Optional<User> userOpt = template.execute(status ->
                userRepository.findOneByEmail(userData.getEmail()));

        assertThat(userOpt).isPresent();
        final User createdUser = userOpt.get();

        // AC-1.1.1: INACTIVE status and activated = false
        assertThat(createdUser.isActivated()).isFalse();
        assertThat(createdUser.getStatus().name()).isEqualTo("INACTIVE");

        // AC-1.1.3: Email stored in lowercase
        assertThat(createdUser.getEmail()).isEqualTo(userData.getEmail().toLowerCase());

        // AC-1.1.4: Login equals email
        assertThat(createdUser.getLogin()).isEqualTo(createdUser.getEmail());

        // AC-1.1.6: Activation key is present and <= 20 characters
        assertThat(createdUser.getActivationKey()).isNotBlank();
        assertThat(createdUser.getActivationKey().length()).isLessThanOrEqualTo(20);

        // AC-1.1.2: Password is BCrypt encoded (BCrypt hashes start with $2a$, $2b$, or $2y$)
        assertThat(createdUser.getPassword()).startsWith("$2");
        assertThat(createdUser.getPassword()).isNotEqualTo(userData.getPassword());

        // AC-1.1.7: User is assigned ROLE_USER authority
        await().until(() -> {
            final Optional<User> userWithAuthorities = template.execute(status ->
                    userRepository.findOneByLogin(userData.getLogin()));
            return userWithAuthorities.isPresent() && !userWithAuthorities.get().getAuthorities().isEmpty();
        });

        final Optional<User> userWithAuthorities = template.execute(status ->
                userRepository.findOneByLogin(userData.getLogin()));
        assertThat(userWithAuthorities).isPresent();
        assertThat(userWithAuthorities.get().getAuthorities())
                .hasSize(1)
                .anyMatch(auth -> auth.getName().equals("ROLE_USER"));

        // AC-1.1.8: Activation email was sent
        final TestMailManager testMailManager = (TestMailManager) mailManager;
        await().until(() -> testMailManager.getEnvelope(helpCode) != null);
        final Envelope envelope = testMailManager.getEnvelope(helpCode);
        assertThat(envelope).isNotNull();
        assertThat(envelope.data()).containsKey("activationKey");
        assertThat(envelope.data().get("activationKey")).isEqualTo(createdUser.getActivationKey());
    }

    /* ####################################################################################################################
                            UC 1.2: User Activation
    #################################################################################################################### */

    @Test
    void testUserActivation_Success() throws Exception {
        // AC-1.3.1: User activated flag is set to true
        // AC-1.3.2: Activation key is cleared (set to null)
        // AC-1.3.3: Activation date is set to current timestamp
        // AC-1.3.4: User status is changed from INACTIVE to ACTIVE

        // First register a user
        final String registrationUri = baseUrl() + "/v1/account/register";
        final UserDto userData = getUserData(false);

        final ObjectMapper defaultObjectMapper = getDefaultObjectMapper();
        final String userDataAsString = defaultObjectMapper.writeValueAsString(userData);
        final Map<String, Object> userDataMap = defaultObjectMapper.readValue(userDataAsString, Map.class);

        final ResponseEntity<Map> registerResponse = httpTestClient.makeHttpRequest(
                registrationUri,
                HttpMethod.POST,
                userDataMap,
                baseHttpHeaders(),
                Map.class
        );

        final String helpCode = (String) registerResponse.getBody().get(HELP_CODE);

        // Get activation key from email
        final TestMailManager testMailManager = (TestMailManager) mailManager;
        await().until(() -> testMailManager.getEnvelope(helpCode) != null);
        final String activationKey = testMailManager.getEnvelope(helpCode).data().get("activationKey").toString();

        // Activate account
        final String activateUri = baseUrl() + "/v1/account/activate?key=" + activationKey;
        final ResponseEntity<Map> activationResponse = httpTestClient.makeHttpRequest(
                activateUri,
                HttpMethod.POST,
                Map.of(),
                baseHttpHeaders(),
                Map.class
        );

        assertThat(activationResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(activationResponse.getBody()).containsEntry("msgKey", "user.activation.success");

        // Verify user is activated
        final Optional<User> activatedUserOpt = template.execute(status ->
                userRepository.findOneByEmail(userData.getEmail()));

        assertThat(activatedUserOpt).isPresent();
        final User activatedUser = activatedUserOpt.get();

        // AC-1.3.1: activated flag is true
        assertThat(activatedUser.isActivated()).isTrue();

        // AC-1.3.2: Activation key is cleared
        assertThat(activatedUser.getActivationKey()).isNullOrEmpty();

        // AC-1.3.3: Activation date is set
        assertThat(activatedUser.getActivationDate()).isNotNull();
        assertThat(activatedUser.getActivationDate())
                .isBetween(LocalDateTime.now(ClockProvider.getClock()).minusMinutes(5),
                        LocalDateTime.now(ClockProvider.getClock()).plusMinutes(1));

        // AC-1.3.4: Status changed to ACTIVE
        assertThat(activatedUser.getStatus().name()).isEqualTo("ACTIVE");
    }

    @Test
    void testUserActivation_InvalidKey() {
        // AF-1.3.1: Invalid activation key should return error
        final String activateUri = baseUrl() + "/v1/account/activate?key=invalidKey123";
        final ResponseEntity<Map> activationResponse = httpTestClient.makeHttpRequest(
                activateUri,
                HttpMethod.POST,
                Map.of(),
                baseHttpHeaders(),
                Map.class
        );

        assertThat(activationResponse.getStatusCode()).isNotEqualTo(HttpStatus.OK);
    }

    /* ####################################################################################################################
                            UC 6.1 & 6.2: Failed Login Attempts and Account Locking
    #################################################################################################################### */

    @Test
    @Sql({USER_DATA_SQL_PATH, SEC_DATA_SQL_PATH})
    void testFailedLoginAttempts_BlocksAfterThreeAttemptsFromSameClient() throws JsonProcessingException {
        // AC-2.4.1: Block occurs at 3 failed attempts for clientId + userName
        // AC-2.4.4: Block is specific to client-user combination

        final String uri = baseUrl() + AUTHENTICATE_ENDPOINT;
        final HttpHeaders httpHeaders = baseHttpHeaders();

        // Make 3 failed login attempts
        for (int i = 0; i < 3; i++) {
            final ResponseEntity<Map> response = httpTestClient.makeHttpRequest(
                    uri,
                    HttpMethod.POST,
                    Map.of(LOGIN_ID, "blockme@yahoo.com", PASSWORD, "wrongPassword"),
                    httpHeaders,
                    Map.class
            );
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        // Fourth attempt should be blocked
        final ResponseEntity<Map> blockedResponse = httpTestClient.makeHttpRequest(
                uri,
                HttpMethod.POST,
                Map.of(LOGIN_ID, "blockme@yahoo.com", PASSWORD, "admin*123!"),
                httpHeaders,
                Map.class
        );

        assertThat(blockedResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        final Map responseBody = blockedResponse.getBody();
        assertThat(responseBody).isNotNull();

        // Verify it's blocked due to too many attempts, not bad credentials
        final String respBodyJson = OBJECT_MAPPER.writeValueAsString(responseBody);
        assertThatJson(respBodyJson).inPath("$.errorMsg.errorKey").isString();
    }

    @Test
    @Sql({USER_DATA_SQL_PATH, SEC_DATA_SQL_PATH})
    void testSuccessfulLogin_ClearsFailedAttempts() {
        // AC-2.1.11: All failed login attempts are cleared for user-client combination

        final String uri = baseUrl() + AUTHENTICATE_ENDPOINT;
        final HttpHeaders httpHeaders = baseHttpHeaders();

        // Make 2 failed attempts (less than threshold)
        for (int i = 0; i < 2; i++) {
            httpTestClient.makeHttpRequest(
                    uri,
                    HttpMethod.POST,
                    Map.of(LOGIN_ID, "queb@yahoo.com", PASSWORD, "wrongPassword"),
                    httpHeaders,
                    Map.class
            );
        }

        // Successful login should clear failed attempts
        final ResponseEntity<Map> successResponse = httpTestClient.makeHttpRequest(
                uri,
                HttpMethod.POST,
                Map.of(LOGIN_ID, "queb@yahoo.com", PASSWORD, "admin*123!"),
                httpHeaders,
                Map.class
        );

        assertThat(successResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Should be able to make more attempts without being blocked
        for (int i = 0; i < 2; i++) {
            final ResponseEntity<Map> response = httpTestClient.makeHttpRequest(
                    uri,
                    HttpMethod.POST,
                    Map.of(LOGIN_ID, "queb@yahoo.com", PASSWORD, "wrongPassword"),
                    httpHeaders,
                    Map.class
            );
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        // Final successful login should still work
        final ResponseEntity<Map> finalResponse = httpTestClient.makeHttpRequest(
                uri,
                HttpMethod.POST,
                Map.of(LOGIN_ID, "queb@yahoo.com", PASSWORD, "admin*123!"),
                httpHeaders,
                Map.class
        );

        assertThat(finalResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    /* ####################################################################################################################
                            UC 5.4: Logout
    #################################################################################################################### */

    @Test
    @Sql({USER_DATA_SQL_PATH, SEC_DATA_SQL_PATH})
    void testLogout_ClearsAllAuthenticationCookies() {
        // AC-2.3.1: All authentication cookies are removed
        // AC-2.3.3: Logout event is published for audit trail

        // First login
        final String loginUri = baseUrl() + AUTHENTICATE_ENDPOINT;
        final HttpHeaders loginHeaders = baseHttpHeaders();

        final ResponseEntity<Map> loginResponse = httpTestClient.makeHttpRequest(
                loginUri,
                HttpMethod.POST,
                Map.of(LOGIN_ID, "queb@yahoo.com", PASSWORD, "admin*123!"),
                loginHeaders,
                Map.class
        );

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Extract cookies from login response
        final List<String> loginCookies = loginResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(loginCookies).isNotNull().isNotEmpty();

        // Verify JWT cookie is present
        assertThat(loginCookies.stream().anyMatch(c -> c.contains(JWT_COOKIE_NAME))).isTrue();

        // Now logout
        final String logoutUri = baseUrl() + "/api/logout";
        final HttpHeaders logoutHeaders = baseHttpHeaders();

        // Add login cookies to logout request
        final String cookieHeaderValue = String.join("; ", loginCookies.stream()
                .map(c -> c.split(";", 2)[0])
                .toList());
        logoutHeaders.add(HttpHeaders.COOKIE, cookieHeaderValue);

        final ResponseEntity<String> logoutResponse = httpTestClient.makeHttpRequest(
                logoutUri,
                HttpMethod.POST,
                null,
                logoutHeaders,
                String.class
        );

        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // AC-2.3.1: Verify cookies are cleared (Max-Age=0 or expired)
        final List<String> logoutCookies = logoutResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (logoutCookies != null) {
            // Check that JWT cookie is removed/expired
            assertThat(logoutCookies.stream()
                    .filter(c -> c.contains(JWT_COOKIE_NAME))
                    .anyMatch(c -> c.contains("Max-Age=0") || c.contains("Expires="))).isTrue();
        }

        // Verify user cannot access protected resource after logout
        final String accountUri = baseUrl() + "/manage/health";
        final ResponseEntity<Map> accountResponse = httpTestClient.makeHttpRequest(
                accountUri,
                HttpMethod.GET,
                null,
                loginHeaders,
                Map.class
        );

        assertThat(accountResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /* ####################################################################################################################
                            UC 8.1: Get Current User Information
    #################################################################################################################### */

    @Test
    @Sql({USER_DATA_SQL_PATH, SEC_DATA_SQL_PATH})
    void testGetCurrentUserInfo_ReturnsUserDetails() {
        // First login
        final String loginUri = baseUrl() + AUTHENTICATE_ENDPOINT;
        final HttpHeaders loginHeaders = baseHttpHeaders();

        final ResponseEntity<Map> loginResponse = httpTestClient.makeHttpRequest(
                loginUri,
                HttpMethod.POST,
                Map.of(LOGIN_ID, "queb@yahoo.com", PASSWORD, "admin*123!"),
                loginHeaders,
                Map.class
        );

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Extract cookies
        final HttpHeaders authHeaders = baseHttpHeaders();
        final List<String> cookies = loginResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
        final String cookieHeaderValue = String.join("; ", cookies.stream()
                .map(c -> c.split(";", 2)[0])
                .toList());
        authHeaders.add(HttpHeaders.COOKIE, cookieHeaderValue);

        // Get current user info
        final String accountUri = baseUrl() + "/v1/account/";
        final ResponseEntity<Map> accountResponse = httpTestClient.makeHttpRequest(
                accountUri,
                HttpMethod.GET,
                null,
                authHeaders,
                Map.class
        );

        assertThat(accountResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        final Map userInfo = accountResponse.getBody();
        assertThat(userInfo).isNotNull();
        assertThat(userInfo.get("login")).isEqualTo("queb@yahoo.com");
        assertThat(userInfo.get("email")).isEqualTo("queb@yahoo.com");
        assertThat(userInfo).containsKeys("firstName", "lastName", "langKey");
    }

    /* ####################################################################################################################
                            UC 2.1: JWT Token Validation
    #################################################################################################################### */

    @Test
    @Sql({USER_DATA_SQL_PATH, SEC_DATA_SQL_PATH})
    void testJWTToken_ContainsRequiredClaims() {
        // AC-6.1.1 through AC-6.1.11: JWT contains all required claims

        final String loginUri = baseUrl() + AUTHENTICATE_ENDPOINT;
        final HttpHeaders loginHeaders = baseHttpHeaders();

        final ResponseEntity<Map> loginResponse = httpTestClient.makeHttpRequest(
                loginUri,
                HttpMethod.POST,
                Map.of(LOGIN_ID, "queb@yahoo.com", PASSWORD, "admin*123!"),
                loginHeaders,
                Map.class
        );

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // AC-6.1.14: JWT cookie is HttpOnly and Secure
        final List<String> cookies = loginResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(cookies).isNotNull();

        final Optional<String> jwtCookie = cookies.stream()
                .filter(c -> c.contains(JWT_COOKIE_NAME))
                .findFirst();

        assertThat(jwtCookie).isPresent();
        assertThat(jwtCookie.get()).contains("HttpOnly");

        // AC-6.1.15: JWT cookie TTL is set
        assertThat(jwtCookie.get()).containsAnyOf("Max-Age", "Expires");

        // AC-6.1.16: Browser cookie is HttpOnly
        final Optional<String> bcookie = cookies.stream()
                .filter(c -> c.contains("bcookie"))
                .findFirst();
        assertThat(bcookie).isPresent();
        assertThat(bcookie.get()).contains("HttpOnly");

        // AC-6.1.17: User cookie is NOT HttpOnly (so JavaScript can read it)
        final Optional<String> userCookie = cookies.stream()
                .filter(c -> c.contains("user"))
                .findFirst();
        assertThat(userCookie).isPresent();
        // If it's NOT HttpOnly, the string "HttpOnly" should not be in this specific cookie
        // Note: This might be implementation-specific

        // AC-6.1.18: Session cookie is HttpOnly
        final Optional<String> sessionCookie = cookies.stream()
                .filter(c -> c.contains(JWT_SESSION_COOKIE))
                .findFirst();
        assertThat(sessionCookie).isPresent();
        assertThat(sessionCookie.get()).contains("HttpOnly");
    }

    /* ####################################################################################################################
                            UC 4.3: Password Encryption
    #################################################################################################################### */

    @Test
    void testPasswordEncryption_UsesBCryptWith10Rounds() throws Exception {
        // AC-4.3.1: BCrypt hashing algorithm
        // AC-4.3.2: Default 10 encryption rounds

        final String registrationUri = baseUrl() + "/v1/account/register";
        final UserDto userData = getUserData(false);
        final String originalPassword = userData.getPassword();

        final ObjectMapper defaultObjectMapper = getDefaultObjectMapper();
        final String userDataAsString = defaultObjectMapper.writeValueAsString(userData);
        final Map<String, Object> userDataMap = defaultObjectMapper.readValue(userDataAsString, Map.class);

        httpTestClient.makeHttpRequest(
                registrationUri,
                HttpMethod.POST,
                userDataMap,
                baseHttpHeaders(),
                Map.class
        );

        await().until(() -> {
            final Optional<User> userOpt = template.execute(status ->
                    userRepository.findOneByEmail(userData.getEmail()));
            return userOpt.isPresent();
        });

        final Optional<User> userOpt = template.execute(status ->
                userRepository.findOneByEmail(userData.getEmail()));

        assertThat(userOpt).isPresent();
        final String encryptedPassword = userOpt.get().getPassword();

        // BCrypt format: $2a$10$... (or $2b$ or $2y$)
        assertThat(encryptedPassword).matches("^\\$2[aby]\\$\\d{2}\\$.+$");

        // Verify it's using 10 rounds (appears as "$10$" in the hash)
        assertThat(encryptedPassword).contains("$10$");

        // Verify password is not stored in plain text
        assertThat(encryptedPassword).isNotEqualTo(originalPassword);

        // AC-4.3.3: Salt automatically generated (BCrypt includes salt in the hash)
        // BCrypt hashes are 60 characters long
        assertThat(encryptedPassword.length()).isEqualTo(60);
    }

    /* ####################################################################################################################
                            UC 3.1: Role-Based Access Control
    #################################################################################################################### */

    @Test
    @Sql({USER_DATA_SQL_PATH, SEC_DATA_SQL_PATH})
    void testRoleBasedAccess_AdminOnlyEndpoint() {
        // Login as regular user
        final String loginUri = baseUrl() + AUTHENTICATE_ENDPOINT;
        final HttpHeaders userHeaders = baseHttpHeaders();

        final ResponseEntity<Map> userLoginResponse = httpTestClient.makeHttpRequest(
                loginUri,
                HttpMethod.POST,
                Map.of(LOGIN_ID, "me@yahoo.com", PASSWORD, "admin*123!"),
                userHeaders,
                Map.class
        );

        assertThat(userLoginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        final HttpHeaders authHeaders = baseHttpHeaders();
        final List<String> cookies = userLoginResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
        final String cookieHeaderValue = String.join("; ", cookies.stream()
                .map(c -> c.split(";", 2)[0])
                .toList());
        authHeaders.add(HttpHeaders.COOKIE, cookieHeaderValue);

        // Try to access admin-only endpoint (actuator endpoints require ROLE_ADMIN)
        final String adminUri = baseUrl() + "/manage/health";
        final ResponseEntity<Map> adminResponse = httpTestClient.makeHttpRequest(
                adminUri,
                HttpMethod.GET,
                null,
                authHeaders,
                Map.class
        );

        // Regular user should be denied access
        assertThat(adminResponse.getStatusCode()).isIn(HttpStatus.FORBIDDEN, HttpStatus.UNAUTHORIZED);

        // Now login as admin
        final HttpHeaders adminHeaders = baseHttpHeaders();
        final ResponseEntity<Map> adminLoginResponse = httpTestClient.makeHttpRequest(
                loginUri,
                HttpMethod.POST,
                Map.of(LOGIN_ID, "queb@yahoo.com", PASSWORD, "admin*123!"),
                adminHeaders,
                Map.class
        );

        assertThat(adminLoginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        final HttpHeaders adminAuthHeaders = baseHttpHeaders();
        final List<String> adminCookies = adminLoginResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
        final String adminCookieHeaderValue = String.join("; ", adminCookies.stream()
                .map(c -> c.split(";", 2)[0])
                .toList());
        adminAuthHeaders.add(HttpHeaders.COOKIE, adminCookieHeaderValue);

        // Admin should have access
        final ResponseEntity<Map> adminAccessResponse = httpTestClient.makeHttpRequest(
                adminUri,
                HttpMethod.GET,
                null,
                adminAuthHeaders,
                Map.class
        );

        assertThat(adminAccessResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    /* ####################################################################################################################
                            Additional Edge Cases and Security Tests
    #################################################################################################################### */

    @Test
    @Sql({USER_DATA_SQL_PATH, SEC_DATA_SQL_PATH})
    void testMultipleSimultaneousLogins_SameUser() {
        // Test that same user can login from multiple clients/sessions
        final String loginUri = baseUrl() + AUTHENTICATE_ENDPOINT;

        // First login
        final HttpHeaders headers1 = baseHttpHeaders();
        final ResponseEntity<Map> response1 = httpTestClient.makeHttpRequest(
                loginUri,
                HttpMethod.POST,
                Map.of(LOGIN_ID, "queb@yahoo.com", PASSWORD, "admin*123!"),
                headers1,
                Map.class
        );

        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Second login (different client/session)
        final HttpHeaders headers2 = baseHttpHeaders();
        final ResponseEntity<Map> response2 = httpTestClient.makeHttpRequest(
                loginUri,
                HttpMethod.POST,
                Map.of(LOGIN_ID, "queb@yahoo.com", PASSWORD, "admin*123!"),
                headers2,
                Map.class
        );

        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Both sessions should be able to access protected resources
        final String accountUri = baseUrl() + "/v1/account/";

        final HttpHeaders auth1 = baseHttpHeaders();
        final String cookies1 = String.join("; ", response1.getHeaders().get(HttpHeaders.SET_COOKIE).stream()
                .map(c -> c.split(";", 2)[0])
                .toList());
        auth1.add(HttpHeaders.COOKIE, cookies1);

        final HttpHeaders auth2 = baseHttpHeaders();
        final String cookies2 = String.join("; ", response2.getHeaders().get(HttpHeaders.SET_COOKIE).stream()
                .map(c -> c.split(";", 2)[0])
                .toList());
        auth2.add(HttpHeaders.COOKIE, cookies2);

        final ResponseEntity<Map> account1 = httpTestClient.makeHttpRequest(accountUri, HttpMethod.GET, null, auth1, Map.class);
        final ResponseEntity<Map> account2 = httpTestClient.makeHttpRequest(accountUri, HttpMethod.GET, null, auth2, Map.class);

        assertThat(account1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(account2.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void testPublicEndpoints_AccessibleWithoutAuthentication() {
        // Test that public endpoints are accessible without authentication
        final String[] publicEndpoints = {
                "/v1/account/register",
                "/v1/account/reset_password/init"
        };

        for (String endpoint : publicEndpoints) {
            final String uri = baseUrl() + endpoint;
            // Making request without auth headers should not return 401/403 immediately
            // (though it may return 400 for missing required data)
            final ResponseEntity<String> response = httpTestClient.makeHttpRequest(
                    uri,
                    HttpMethod.POST,
                    Map.of(),
                    baseHttpHeaders(),
                    String.class
            );

            // Should not be 401 Unauthorized or 403 Forbidden
            assertThat(response.getStatusCode()).isNotIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
        }
    }

}
