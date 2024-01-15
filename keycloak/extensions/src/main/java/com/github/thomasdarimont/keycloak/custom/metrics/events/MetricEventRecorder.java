package com.github.thomasdarimont.keycloak.custom.metrics.events;

import com.github.thomasdarimont.keycloak.custom.metrics.KeycloakMetrics;
import com.github.thomasdarimont.keycloak.custom.support.KeycloakSessionLookup;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.events.Details;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

import static org.keycloak.events.EventType.CLIENT_LOGIN;
import static org.keycloak.events.EventType.CLIENT_LOGIN_ERROR;
import static org.keycloak.events.EventType.CODE_TO_TOKEN;
import static org.keycloak.events.EventType.CODE_TO_TOKEN_ERROR;
import static org.keycloak.events.EventType.LOGIN;
import static org.keycloak.events.EventType.LOGIN_ERROR;
import static org.keycloak.events.EventType.LOGOUT;
import static org.keycloak.events.EventType.LOGOUT_ERROR;
import static org.keycloak.events.EventType.REFRESH_TOKEN;
import static org.keycloak.events.EventType.REFRESH_TOKEN_ERROR;
import static org.keycloak.events.EventType.REGISTER;
import static org.keycloak.events.EventType.REGISTER_ERROR;
import static org.keycloak.events.EventType.TOKEN_EXCHANGE;
import static org.keycloak.events.EventType.TOKEN_EXCHANGE_ERROR;
import static org.keycloak.events.EventType.USER_INFO_REQUEST;
import static org.keycloak.events.EventType.USER_INFO_REQUEST_ERROR;

@JBossLog
@RequiredArgsConstructor
public class MetricEventRecorder {

    private static final String USER_EVENT_METRIC_NAME = "keycloak_user_event";

    private static final String ADMIN_EVENT_METRIC_NAME = "keycloak_admin_event";

    private final MeterRegistry metricRegistry;

    private final Map<EventType, Consumer<Event>> customUserEventHandlers;

    private final ConcurrentMap<String, String> realmNameCache = new ConcurrentHashMap<>();

    public MetricEventRecorder(KeycloakMetrics keycloakMetrics) {
        this.metricRegistry = keycloakMetrics.getMeterRegistry();
        this.customUserEventHandlers = registerCustomUserEventHandlers();
    }

    private Map<EventType, Consumer<Event>> registerCustomUserEventHandlers() {
        Map<EventType, Consumer<Event>> map = new HashMap<>();
        map.put(LOGIN, this::recordUserLogin);
        map.put(LOGIN_ERROR, this::recordUserLoginError);
        map.put(LOGOUT, this::recordUserLogout);
        map.put(LOGOUT_ERROR, this::recordUserLogoutError);
        map.put(CLIENT_LOGIN, this::recordClientLogin);
        map.put(CLIENT_LOGIN_ERROR, this::recordClientLoginError);
        map.put(REGISTER, this::recordUserRegistration);
        map.put(REGISTER_ERROR, this::recordUserRegistrationError);
        map.put(REFRESH_TOKEN, this::recordOauthTokenRefresh);
        map.put(REFRESH_TOKEN_ERROR, this::recordOauthTokenRefreshError);
        map.put(CODE_TO_TOKEN, this::recordOauthCodeToToken);
        map.put(CODE_TO_TOKEN_ERROR, this::recordOauthCodeToTokenError);
        map.put(USER_INFO_REQUEST, this::recordOauthUserInfoRequest);
        map.put(USER_INFO_REQUEST_ERROR, this::recordOauthUserInfoRequestError);
        map.put(TOKEN_EXCHANGE, this::recordOauthTokenExchange);
        map.put(TOKEN_EXCHANGE_ERROR, this::recordOauthTokenExchangeError);
        return map;
    }

    public void recordEvent(Event event) {
        lookupUserEventHandler(event).accept(event);
    }

    public void recordEvent(AdminEvent event, boolean includeRepresentation) {

        // TODO add capability to ignore certain admin events

        recordGenericAdminEvent(event);
    }

    private void recordGenericAdminEvent(AdminEvent event) {

        var operationType = event.getOperationType();
        var resourceType = event.getResourceType();
        var realmName = resolveRealmName(event.getRealmId());
        var resourceTypeName = resourceType.name();
        var operationTypeName = operationType.name();
        var tags = Tags.of("realm", realmName, "resource", resourceTypeName, "operation_type", operationTypeName);

        metricRegistry.counter(ADMIN_EVENT_METRIC_NAME, tags).increment();
    }

    public Consumer<Event> lookupUserEventHandler(Event event) {
        return customUserEventHandlers.getOrDefault(event.getType(), this::recordGenericUserEvent);
    }

    protected void recordOauthUserInfoRequestError(Event event) {

        var realmName = resolveRealmName(event.getRealmId());
        var clientId = event.getClientId();
        var error = event.getError();
        var tags = Tags.of("realm", realmName, "client_id", resolveClientId(clientId), "error", error);

        metricRegistry.counter(KeycloakMetrics.OAUTH_USERINFO_REQUEST_ERROR_TOTAL.getName(), tags).increment();
        metricRegistry.counter(KeycloakMetrics.OAUTH_USERINFO_REQUEST_ATTEMPT_TOTAL.getName(), tags).increment();
    }

    protected void recordOauthUserInfoRequest(Event event) {

        var realmName = resolveRealmName(event.getRealmId());
        var clientId = event.getClientId();
        var tags = Tags.of("realm", realmName, "client_id", resolveClientId(clientId));

        metricRegistry.counter(KeycloakMetrics.OAUTH_USERINFO_REQUEST_SUCCESS_TOTAL.getName(), tags).increment();
        metricRegistry.counter(KeycloakMetrics.OAUTH_USERINFO_REQUEST_ATTEMPT_TOTAL.getName(), tags).increment();
    }

    protected void recordOauthTokenExchange(Event event) {

        var realmName = resolveRealmName(event.getRealmId());
        var clientId = event.getClientId();
        var tags = Tags.of("realm", realmName, "client_id", resolveClientId(clientId));

        metricRegistry.counter(KeycloakMetrics.OAUTH_TOKEN_EXCHANGE_SUCCESS_TOTAL.getName(), tags).increment();
        metricRegistry.counter(KeycloakMetrics.OAUTH_TOKEN_EXCHANGE_ATTEMPT_TOTAL.getName(), tags).increment();
    }

    protected void recordOauthTokenExchangeError(Event event) {

        var realmName = resolveRealmName(event.getRealmId());
        var clientId = event.getClientId();
        var tags = Tags.of("realm", realmName, "client_id", resolveClientId(clientId), "error", event.getError());

        metricRegistry.counter(KeycloakMetrics.OAUTH_TOKEN_EXCHANGE_ERROR_TOTAL.getName(), tags).increment();
        metricRegistry.counter(KeycloakMetrics.OAUTH_TOKEN_EXCHANGE_ATTEMPT_TOTAL.getName(), tags).increment();
    }

    protected void recordUserLogout(Event event) {

        var provider = getIdentityProvider(event);
        var realmName = resolveRealmName(event.getRealmId());
        // String clientId = event.getClientId();
        var tags = Tags.of("realm", realmName, "provider", provider);

        metricRegistry.counter(KeycloakMetrics.AUTH_USER_LOGOUT_SUCCESS_TOTAL.getName(), tags).increment();
    }

    protected void recordUserLogoutError(Event event) {

        var provider = getIdentityProvider(event);
        var realmName = resolveRealmName(event.getRealmId());
        var clientId = event.getClientId();
        var error = event.getError();
        var tags = Tags.of("realm", realmName, "provider", provider, "client_id", resolveClientId(clientId), "error", error);

        metricRegistry.counter(KeycloakMetrics.AUTH_USER_LOGOUT_ERROR_TOTAL.getName(), tags).increment();
    }

    protected void recordOauthCodeToTokenError(Event event) {

        var provider = getIdentityProvider(event);
        var realmName = resolveRealmName(event.getRealmId());
        var clientId = event.getClientId();
        var error = event.getError();
        var tags = Tags.of("realm", realmName, "provider", provider, "client_id", resolveClientId(clientId), "error", error);

        metricRegistry.counter(KeycloakMetrics.OAUTH_CODE_TO_TOKEN_ERROR_TOTAL.getName(), tags).increment();
        metricRegistry.counter(KeycloakMetrics.OAUTH_CODE_TO_TOKEN_ATTEMPT_TOTAL.getName(), tags).increment();
    }

    protected void recordOauthCodeToToken(Event event) {

        var provider = getIdentityProvider(event);
        var realmName = resolveRealmName(event.getRealmId());
        var clientId = event.getClientId();
        var tags = Tags.of("realm", realmName, "provider", provider, "client_id", resolveClientId(clientId));

        metricRegistry.counter(KeycloakMetrics.OAUTH_CODE_TO_TOKEN_SUCCESS_TOTAL.getName(), tags).increment();
        metricRegistry.counter(KeycloakMetrics.OAUTH_CODE_TO_TOKEN_ATTEMPT_TOTAL.getName(), tags).increment();
    }

    protected void recordClientLogin(Event event) {

        var realmName = resolveRealmName(event.getRealmId());
        var clientId = event.getClientId();
        var tags = Tags.of("realm", realmName, "client_id", resolveClientId(clientId));

        metricRegistry.counter(KeycloakMetrics.AUTH_CLIENT_LOGIN_SUCCESS_TOTAL.getName(), tags).increment();
        metricRegistry.counter(KeycloakMetrics.AUTH_CLIENT_LOGIN_ATTEMPT_TOTAL.getName(), tags).increment();
    }

    protected void recordClientLoginError(Event event) {

        var realmName = resolveRealmName(event.getRealmId());
        var clientId = event.getClientId();
        var error = event.getError();
        var tags = Tags.of("realm", realmName, "client_id", resolveClientId(clientId), "error", error);

        metricRegistry.counter(KeycloakMetrics.AUTH_CLIENT_LOGIN_ERROR_TOTAL.getName(), tags).increment();
        metricRegistry.counter(KeycloakMetrics.AUTH_CLIENT_LOGIN_ATTEMPT_TOTAL.getName(), tags).increment();
    }

    protected void recordOauthTokenRefreshError(Event event) {

        var provider = getIdentityProvider(event);
        var realmName = resolveRealmName(event.getRealmId());
        var clientId = event.getClientId();
        var error = event.getError();
        var tags = Tags.of("realm", realmName, "client_id", resolveClientId(clientId), "error", error, "provider", provider);

        metricRegistry.counter(KeycloakMetrics.OAUTH_TOKEN_REFRESH_ERROR_TOTAL.getName(), tags).increment();
        metricRegistry.counter(KeycloakMetrics.OAUTH_TOKEN_REFRESH_ATTEMPT_TOTAL.getName(), tags).increment();
    }

    protected void recordOauthTokenRefresh(Event event) {

        var realmName = resolveRealmName(event.getRealmId());
        var clientId = event.getClientId();
        var tags = Tags.of("realm", realmName, "client_id", resolveClientId(clientId));

        metricRegistry.counter(KeycloakMetrics.OAUTH_TOKEN_REFRESH_SUCCESS_TOTAL.getName(), tags).increment();
        metricRegistry.counter(KeycloakMetrics.OAUTH_TOKEN_REFRESH_ATTEMPT_TOTAL.getName(), tags).increment();
    }

    protected void recordUserRegistrationError(Event event) {

        var provider = getIdentityProvider(event);
        var realmName = resolveRealmName(event.getRealmId());
        var clientId = event.getClientId();
        var error = event.getError();
        var tags = Tags.of("realm", realmName, "client_id", resolveClientId(clientId), "error", error, "provider", provider);

        metricRegistry.counter(KeycloakMetrics.AUTH_USER_REGISTER_ERROR_TOTAL.getName(), tags).increment();
        metricRegistry.counter(KeycloakMetrics.AUTH_USER_REGISTER_ATTEMPT_TOTAL.getName(), tags).increment();
    }

    protected void recordUserRegistration(Event event) {

        var realmName = resolveRealmName(event.getRealmId());
        var clientId = event.getClientId();
        var tags = Tags.of("realm", realmName, "client_id", resolveClientId(clientId));

        metricRegistry.counter(KeycloakMetrics.AUTH_USER_REGISTER_SUCCESS_TOTAL.getName(), tags).increment();
        metricRegistry.counter(KeycloakMetrics.AUTH_USER_REGISTER_ATTEMPT_TOTAL.getName(), tags).increment();
    }

    protected void recordUserLoginError(Event event) {

        var provider = getIdentityProvider(event);
        var realmName = resolveRealmName(event.getRealmId());
        var clientId = event.getClientId();
        var error = event.getError();
        var tags = Tags.of("realm", realmName, "client_id", resolveClientId(clientId), "error", error, "provider", provider);

        metricRegistry.counter(KeycloakMetrics.AUTH_USER_LOGIN_ERROR_TOTAL.getName(), tags).increment();
        metricRegistry.counter(KeycloakMetrics.AUTH_USER_LOGIN_ATTEMPT_TOTAL.getName(), tags).increment();
    }

    protected void recordUserLogin(Event event) {

        var provider = getIdentityProvider(event);
        var realmName = resolveRealmName(event.getRealmId());
        var clientId = resolveClientId(event.getClientId());
        var tags = Tags.of("realm", realmName, "client_id", clientId, "provider", provider);

        metricRegistry.counter(KeycloakMetrics.AUTH_USER_LOGIN_SUCCESS_TOTAL.getName(), tags).increment();
        metricRegistry.counter(KeycloakMetrics.AUTH_USER_LOGIN_ATTEMPT_TOTAL.getName(), tags).increment();
    }

    /**
     * Count generic user event
     *
     * @param event User event
     */
    protected void recordGenericUserEvent(Event event) {

        var eventType = event.getType();
        var realmName = resolveRealmName(event.getRealmId());
        var eventTypeName = eventType.name();
        var tags = Tags.of("realm", realmName, "event_type", eventTypeName);

        if (eventType == EventType.CUSTOM_REQUIRED_ACTION && event.getDetails().get(Details.CUSTOM_REQUIRED_ACTION) != null) {
            tags = Tags.concat(tags, Tags.of("custom_required_action", event.getDetails().get(Details.CUSTOM_REQUIRED_ACTION)));
        }

        metricRegistry.counter(USER_EVENT_METRIC_NAME, tags).increment();
    }

    /**
     * Retrieve the identity provider name from event details or
     * <p>
     * default to {@value "keycloak"}.
     *
     * @param event User event
     * @return Identity provider name
     */
    private String getIdentityProvider(Event event) {

        String identityProvider = null;
        if (event.getDetails() != null) {
            identityProvider = event.getDetails().get("identity_provider");
        }

        if (identityProvider == null) {
            identityProvider = "@realm";
        }

        return identityProvider;
    }

    /**
     * Creates a counter based on a event name
     */
    private Counter createCounter(String name, boolean isAdmin) {
        var description = isAdmin ? "Generic KeyCloak Admin event" : "Generic KeyCloak User event";
        return Counter.builder(name).description(description).register(metricRegistry);
    }


    private String resolveClientId(String clientId) {
        if (clientId == null) {
            return "missing";
        }
        return clientId;
    }

    private String resolveRealmName(String realmId) {
        return realmNameCache.computeIfAbsent(realmId, key -> {
            var realm = KeycloakSessionLookup.currentSession().realms().getRealm(key);
            return realm.getName();
        });
    }
}
