package com.github.thomasdarimont.keycloak.custom.security.password.hibp;

import com.google.auto.service.AutoService;
import lombok.extern.jbosslog.JBossLog;
import org.apache.http.client.config.RequestConfig;
import org.keycloak.Config;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.policy.PasswordPolicyConfigException;
import org.keycloak.policy.PasswordPolicyProvider;
import org.keycloak.policy.PasswordPolicyProviderFactory;
import org.keycloak.policy.PolicyError;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * {@link PasswordPolicyProvider} that rejects passwords which appeared in known data breaches according to
 * <a href="https://haveibeenpwned.com/Passwords">Have I Been Pwned</a>.
 * <p>
 * Passwords are checked via the k-anonymity range API, so only the first 5 characters of the SHA-1 hash
 * of the password ever leave the server.
 * <p>
 * Usage: add {@code acme-hibp(0)} to the realm password policy. The policy value is the maximum number
 * of breaches a password may have appeared in to still be accepted, {@code 0} rejects every known password.
 * <p>
 * Behaviour when the HIBP API is unavailable is controlled by the {@code failOpen} SPI setting,
 * see {@link HibpConfig}.
 */
@JBossLog
public class HibpPasswordPolicyProvider implements PasswordPolicyProvider {

    public static final String ID = "acme-hibp";

    public static final String ERROR_MESSAGE_BREACHED = "invalidPasswordHibpBreachedMessage";

    public static final String ERROR_MESSAGE_UNAVAILABLE = "invalidPasswordHibpUnavailableMessage";

    public static final int DEFAULT_MAX_ALLOWED_BREACH_COUNT = 0;

    private final KeycloakContext context;

    private final HibpClient client;

    private final boolean failOpen;

    public HibpPasswordPolicyProvider(KeycloakContext context, HibpClient client, boolean failOpen) {
        this.context = context;
        this.client = client;
        this.failOpen = failOpen;
    }

    @Override
    public PolicyError validate(RealmModel realm, UserModel user, String password) {
        return validate(user.getUsername(), password);
    }

    @Override
    public PolicyError validate(String username, String password) {

        if (password == null || password.isEmpty()) {
            return null;
        }

        int maxAllowedBreachCount = getMaxAllowedBreachCount();

        int breachCount;
        try {
            breachCount = client.lookupBreachCount(password);
        } catch (IOException e) {
            if (failOpen) {
                log.warnf(e, "Could not check password against HIBP, accepting password. username=%s", username);
                return null;
            }
            log.errorf(e, "Could not check password against HIBP, rejecting password. username=%s", username);
            return new PolicyError(ERROR_MESSAGE_UNAVAILABLE);
        }

        if (breachCount > maxAllowedBreachCount) {
            log.debugf("Rejecting breached password. username=%s breachCount=%d maxAllowedBreachCount=%d", //
                    username, breachCount, maxAllowedBreachCount);
            return new PolicyError(ERROR_MESSAGE_BREACHED, breachCount);
        }

        return null;
    }

    protected int getMaxAllowedBreachCount() {

        RealmModel realm = context.getRealm();
        if (realm == null) {
            return DEFAULT_MAX_ALLOWED_BREACH_COUNT;
        }

        PasswordPolicy passwordPolicy = realm.getPasswordPolicy();
        if (passwordPolicy == null) {
            return DEFAULT_MAX_ALLOWED_BREACH_COUNT;
        }

        Object policyConfig = passwordPolicy.getPolicyConfig(ID);
        if (policyConfig instanceof Integer maxAllowedBreachCount) {
            return maxAllowedBreachCount;
        }

        return DEFAULT_MAX_ALLOWED_BREACH_COUNT;
    }

    /**
     * Parses the policy value, which is the maximum number of breaches a password may have appeared in.
     * Example: {@code acme-hibp(0)}.
     */
    @Override
    public Object parseConfig(String value) {

        Integer maxAllowedBreachCount = parseInteger(value, DEFAULT_MAX_ALLOWED_BREACH_COUNT);
        if (maxAllowedBreachCount < 0) {
            throw new PasswordPolicyConfigException("Maximum allowed breach count must not be negative");
        }

        return maxAllowedBreachCount;
    }

    @Override
    public void close() {
        // NOOP
    }

    @AutoService(PasswordPolicyProviderFactory.class)
    public static class Factory implements PasswordPolicyProviderFactory, ServerInfoAwareProviderFactory {

        private HibpConfig config = HibpConfig.defaults();

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public PasswordPolicyProvider create(KeycloakSession session) {
            return new HibpPasswordPolicyProvider(session.getContext(), new HibpClient(session, config), config.failOpen());
        }

        @Override
        public String getDisplayName() {
            return "Acme: Not Pwned Password (HIBP)";
        }

        @Override
        public String getConfigType() {
            return PasswordPolicyProvider.INT_CONFIG_TYPE;
        }

        @Override
        public String getDefaultConfigValue() {
            return String.valueOf(DEFAULT_MAX_ALLOWED_BREACH_COUNT);
        }

        @Override
        public boolean isMultiplSupported() {
            return false;
        }

        @Override
        public void init(Config.Scope config) {
            // spi-password-policy--acme-hibp--<property>
            this.config = HibpConfig.from(config);
            log.debugf("Initialized HIBP password policy. apiUrl=%s failOpen=%s addPadding=%s", //
                    this.config.apiUrl(), this.config.failOpen(), this.config.addPadding());
        }

        @Override
        public void postInit(KeycloakSessionFactory factory) {
            // NOOP
        }

        @Override
        public void close() {
            // NOOP
        }

        @Override
        public List<ProviderConfigProperty> getConfigMetadata() {
            return ProviderConfigurationBuilder.create() //
                    .property().name(HibpConfig.API_URL_PROPERTY).type(ProviderConfigProperty.STRING_TYPE) //
                    .helpText("Base URL of the HIBP Pwned Passwords range API.") //
                    .defaultValue(HibpConfig.DEFAULT_API_URL).add() //

                    .property().name(HibpConfig.USER_AGENT_PROPERTY).type(ProviderConfigProperty.STRING_TYPE) //
                    .helpText("User-Agent header sent to the HIBP API.") //
                    .defaultValue(HibpConfig.DEFAULT_USER_AGENT).add() //

                    .property().name(HibpConfig.ADD_PADDING_PROPERTY).type(ProviderConfigProperty.BOOLEAN_TYPE) //
                    .helpText("Request padded responses to hide the queried range from network observers.") //
                    .defaultValue(HibpConfig.DEFAULT_ADD_PADDING).add() //

                    .property().name(HibpConfig.FAIL_OPEN_PROPERTY).type(ProviderConfigProperty.BOOLEAN_TYPE) //
                    .helpText("Accept passwords if the HIBP API cannot be reached. If false, passwords are rejected while the API is unavailable.") //
                    .defaultValue(HibpConfig.DEFAULT_FAIL_OPEN).add() //

                    .property().name(HibpConfig.CONNECT_TIMEOUT_MILLIS_PROPERTY).type(ProviderConfigProperty.STRING_TYPE) //
                    .helpText("Connect timeout in milliseconds for requests to the HIBP API.") //
                    .defaultValue(HibpConfig.DEFAULT_CONNECT_TIMEOUT_MILLIS).add() //

                    .property().name(HibpConfig.SOCKET_TIMEOUT_MILLIS_PROPERTY).type(ProviderConfigProperty.STRING_TYPE) //
                    .helpText("Socket timeout in milliseconds for requests to the HIBP API.") //
                    .defaultValue(HibpConfig.DEFAULT_SOCKET_TIMEOUT_MILLIS).add() //

                    .build();
        }

        @Override
        public Map<String, String> getOperationalInfo() {
            return Map.of( //
                    HibpConfig.API_URL_PROPERTY, config.apiUrl(), //
                    HibpConfig.FAIL_OPEN_PROPERTY, String.valueOf(config.failOpen()), //
                    HibpConfig.ADD_PADDING_PROPERTY, String.valueOf(config.addPadding()));
        }
    }

    /**
     * Configuration for the {@link HibpPasswordPolicyProvider} that is derived from the SPI configuration.
     * <p>
     * All settings can be provided via the standard Keycloak SPI configuration mechanism, e.g.
     * <pre>
     * --spi-password-policy--acme-hibp--fail-open=false
     * --spi-password-policy--acme-hibp--socket-timeout-millis=5000
     * </pre>
     * or via environment variables, e.g. {@code KC_SPI_PASSWORD_POLICY__ACME_HIBP__FAIL_OPEN=false}.
     *
     * @param apiUrl               base URL of the HIBP range API, the 5 character SHA-1 prefix is appended to this URL
     * @param userAgent            user agent sent to the HIBP API (HIBP requires a User-Agent header)
     * @param addPadding           whether to request response padding to prevent inference of the queried range via the response size
     * @param failOpen             whether to accept a password if the HIBP API cannot be reached
     * @param connectTimeoutMillis connect timeout for requests to the HIBP API
     * @param socketTimeoutMillis  socket (read) timeout for requests to the HIBP API
     */
    public record HibpConfig(String apiUrl, String userAgent, boolean addPadding, boolean failOpen, int connectTimeoutMillis,
                             int socketTimeoutMillis) {

        public static final String DEFAULT_API_URL = "https://api.pwnedpasswords.com/range/";

        public static final String DEFAULT_USER_AGENT = "Keycloak-Acme-HIBP-PasswordPolicy";

        public static final boolean DEFAULT_ADD_PADDING = true;

        public static final boolean DEFAULT_FAIL_OPEN = true;

        public static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 5000;

        public static final int DEFAULT_SOCKET_TIMEOUT_MILLIS = 10000;

        public static final String API_URL_PROPERTY = "apiUrl";

        public static final String USER_AGENT_PROPERTY = "userAgent";

        public static final String ADD_PADDING_PROPERTY = "addPadding";

        public static final String FAIL_OPEN_PROPERTY = "failOpen";

        public static final String CONNECT_TIMEOUT_MILLIS_PROPERTY = "connectTimeoutMillis";

        public static final String SOCKET_TIMEOUT_MILLIS_PROPERTY = "socketTimeoutMillis";

        public HibpConfig {
            if (apiUrl == null || apiUrl.isBlank()) {
                throw new IllegalArgumentException("apiUrl must not be blank");
            }
            if (!apiUrl.endsWith("/")) {
                apiUrl = apiUrl + "/";
            }
            if (connectTimeoutMillis < 0 || socketTimeoutMillis < 0) {
                throw new IllegalArgumentException("timeouts must not be negative");
            }
        }

        public static HibpConfig defaults() {
            return new HibpConfig(DEFAULT_API_URL, DEFAULT_USER_AGENT, DEFAULT_ADD_PADDING, DEFAULT_FAIL_OPEN, //
                    DEFAULT_CONNECT_TIMEOUT_MILLIS, DEFAULT_SOCKET_TIMEOUT_MILLIS);
        }

        public static HibpConfig from(Config.Scope scope) {

            if (scope == null) {
                return defaults();
            }

            return new HibpConfig( //
                    scope.get(API_URL_PROPERTY, DEFAULT_API_URL), //
                    scope.get(USER_AGENT_PROPERTY, DEFAULT_USER_AGENT), //
                    scope.getBoolean(ADD_PADDING_PROPERTY, DEFAULT_ADD_PADDING), //
                    scope.getBoolean(FAIL_OPEN_PROPERTY, DEFAULT_FAIL_OPEN), //
                    scope.getInt(CONNECT_TIMEOUT_MILLIS_PROPERTY, DEFAULT_CONNECT_TIMEOUT_MILLIS), //
                    scope.getInt(SOCKET_TIMEOUT_MILLIS_PROPERTY, DEFAULT_SOCKET_TIMEOUT_MILLIS));
        }
    }

    /**
     * Client for the <a href="https://haveibeenpwned.com/API/v3#PwnedPasswords">Have I Been Pwned Passwords API</a>.
     * <p>
     * The client uses the k-anonymity model of the range API: only the first 5 characters of the SHA-1 hash of the
     * password are sent to the API. The API responds with all breached hash suffixes for that prefix together with
     * their breach counts, and the lookup of the actual hash suffix happens locally.
     */
    public static class HibpClient {

        static final int PREFIX_LENGTH = 5;

        static final String ADD_PADDING_HEADER = "Add-Padding";

        static final String USER_AGENT_HEADER = "User-Agent";

        private final KeycloakSession session;

        private final HibpConfig config;

        public HibpClient(KeycloakSession session, HibpConfig config) {
            this.session = session;
            this.config = config;
        }

        /**
         * Looks up how often the given password appeared in known data breaches.
         *
         * @return the breach count, {@literal 0} if the password was not found in any known breach
         * @throws IOException if the HIBP API could not be reached or returned an unexpected response
         */
        public int lookupBreachCount(String password) throws IOException {

            String sha1Hex = sha1Hex(password);
            String prefix = sha1Hex.substring(0, PREFIX_LENGTH);
            String suffix = sha1Hex.substring(PREFIX_LENGTH);

            Map<String, Integer> hashRanges = fetchRange(prefix);
            return hashRanges.getOrDefault(suffix, 0);
        }

        /**
         * Fetches all breached hash suffixes for the given SHA-1 hash prefix.
         *
         * @return map of uppercase hash suffix to breach count
         */
        protected Map<String, Integer> fetchRange(String prefix) throws IOException {

            String url = config.apiUrl() + prefix;
            log.debugf("Fetching HIBP range. prefix=%s", prefix);

            var requestConfig = RequestConfig.custom() //
                    .setConnectTimeout(config.connectTimeoutMillis()) //
                    .setConnectionRequestTimeout(config.connectTimeoutMillis()) //
                    .setSocketTimeout(config.socketTimeoutMillis()) //
                    .build();

            var request = SimpleHttp.create(session).withRequestConfig(requestConfig) //
                    .doGet(url) //
                    .header(USER_AGENT_HEADER, config.userAgent());

            if (config.addPadding()) {
                // see: https://haveibeenpwned.com/API/v3#PwnedPasswordsPadding
                request.header(ADD_PADDING_HEADER, "true");
            }

            try (var response = request.asResponse()) {
                int status = response.getStatus();
                if (status != 200) {
                    throw new IOException("Unexpected response from HIBP API. status=" + status + " prefix=" + prefix);
                }
                return parseRange(response.asString());
            }
        }

        /**
         * Parses a HIBP range API response of the form {@code SUFFIX:COUNT} per line.
         * Padded entries with a count of {@literal 0} are ignored.
         *
         * @return map of uppercase hash suffix to breach count
         */
        static Map<String, Integer> parseRange(String responseBody) {

            var range = new HashMap<String, Integer>();
            if (responseBody == null || responseBody.isBlank()) {
                return range;
            }

            for (String line : responseBody.split("\n")) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                int separatorIndex = line.indexOf(':');
                if (separatorIndex <= 0) {
                    log.debugf("Ignoring malformed HIBP range line: %s", line);
                    continue;
                }

                String suffix = line.substring(0, separatorIndex).toUpperCase();
                String countValue = line.substring(separatorIndex + 1).trim().replace(",", "");

                int count;
                try {
                    count = Integer.parseInt(countValue);
                } catch (NumberFormatException e) {
                    log.debugf("Ignoring HIBP range line with invalid count: %s", line);
                    continue;
                }

                if (count > 0) {
                    range.put(suffix, count);
                }
            }

            return range;
        }

        /**
         * @return uppercase hex encoded SHA-1 hash of the given value
         */
        static String sha1Hex(String value) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-1");
                byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
                return HexFormat.of().withUpperCase().formatHex(hash);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("SHA-1 MessageDigest not available", e);
            }
        }
    }
}
