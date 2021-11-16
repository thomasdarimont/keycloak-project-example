package com.github.thomasdarimont.keycloak.custom.metrics.events;

import com.github.thomasdarimont.keycloak.custom.metrics.KeycloakMetrics;
import com.github.thomasdarimont.keycloak.custom.support.KeycloakSessionLookup;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Tag;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.RealmModel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

import static com.github.thomasdarimont.keycloak.custom.metrics.KeycloakMetrics.tag;
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
import static org.keycloak.events.EventType.USER_INFO_REQUEST;
import static org.keycloak.events.EventType.USER_INFO_REQUEST_ERROR;

@RequiredArgsConstructor
public class MetricEventRecorder {

    private static final Logger log = Logger.getLogger(MetricEventRecorder.class);

    private static final String USER_EVENT_METRIC_NAME = "keycloak_user_event";

    private static final String ADMIN_EVENT_METRIC_NAME = "keycloak_admin_event";

    private final Map<String, Metadata> genericCounters;

    protected final MetricRegistry metricRegistry;

    protected final Map<EventType, Consumer<Event>> customUserEventHandlers;

    protected final ConcurrentMap<String, String> realmNameCache = new ConcurrentHashMap<>();

    public MetricEventRecorder() {
        this(KeycloakMetrics.lookupMetricRegistry());
    }

    public MetricEventRecorder(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
        this.customUserEventHandlers = registerCustomUserEventHandlers();
        this.genericCounters = registerGenericEventCounters();
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
        return map;
    }

    private Map<String, Metadata> registerGenericEventCounters() {
        Map<String, Metadata> initCounters = new HashMap<>();
        registerUserEventCounters(initCounters);
        registerAdminEventCounters(initCounters);
        return Collections.unmodifiableMap(initCounters);
    }

    public Consumer<Event> lookupUserEventHandler(Event event) {
        return customUserEventHandlers.getOrDefault(event.getType(), this::recordGenericUserEvent);
    }

    public void onEvent(AdminEvent event, boolean includeRepresentation) {

        // TODO add capability to ignore certain admin events

        OperationType operationType = event.getOperationType();
        String counterName = ADMIN_EVENT_METRIC_NAME;
        Metadata counterMetadata = genericCounters.get(counterName);
        ResourceType resourceType = event.getResourceType();
        String realmName = resolveRealmName(event.getRealmId());

        if (counterMetadata == null) {
            log.warnf("Counter %s for admin event operation type %s does not exist. Resource type: %s, realm: %s", counterName, operationType.name(), resourceType.name(), realmName);
            return;
        }

        Tag[] tags = {
                tag("realm", realmName),
                tag("resource", resourceType.name()),
                tag("operation_type", operationType.name()),
        };

        metricRegistry.counter(counterMetadata, tags).inc();
    }

    /**
     * Counters for all user events
     */
    private void registerUserEventCounters(Map<String, Metadata> counters) {

        String counterName = USER_EVENT_METRIC_NAME;
        Metadata counter = createCounter(counterName, false);
        counters.put(counterName, counter);
    }

    /**
     * Counters for all admin events
     */
    protected void registerAdminEventCounters(Map<String, Metadata> counters) {

        String counterName = ADMIN_EVENT_METRIC_NAME;
        Metadata counter = createCounter(counterName, true);
        counters.put(counterName, counter);
    }

    protected void recordOauthUserInfoRequestError(Event event) {
        String realmName = resolveRealmName(event.getRealmId());
        String clientId = event.getClientId();
        String error = event.getError();
        Tag[] tags = {
                tag("realm", realmName),
                tag("client_id", clientId),
                tag("error", error),
        };
        metricRegistry.counter(KeycloakMetrics.OAUTH_USERINFO_REQUEST_ERROR_TOTAL.getMetadata(), tags).inc();
    }

    protected void recordOauthUserInfoRequest(Event event) {
        String realmName = resolveRealmName(event.getRealmId());
        String clientId = event.getClientId();
        Tag[] tags = {
                tag("realm", realmName),
                tag("client_id", clientId),
        };
        metricRegistry.counter(KeycloakMetrics.OAUTH_USERINFO_REQUEST_SUCCESS_TOTAL.getMetadata(), tags).inc();
    }

    protected void recordUserLogout(Event event) {
        String provider = getIdentityProvider(event);
        String realmName = resolveRealmName(event.getRealmId());
        Tag[] tags = {
                tag("realm", realmName),
                tag("provider", provider),
//                tag("client_id", event.getClientId()),
        };
        metricRegistry.counter(KeycloakMetrics.AUTH_USER_LOGOUT_SUCCESS_TOTAL.getMetadata(), tags).inc();
    }

    protected void recordUserLogoutError(Event event) {
        String provider = getIdentityProvider(event);
        String realmName = resolveRealmName(event.getRealmId());
        String clientId = event.getClientId();
        String error = event.getError();
        Tag[] tags = {
                tag("realm", realmName),
                tag("provider", provider),
                tag("client_id", clientId),
                tag("error", error),
        };
        metricRegistry.counter(KeycloakMetrics.AUTH_USER_LOGOUT_ERROR_TOTAL.getMetadata(), tags).inc();
    }

    protected void recordOauthCodeToTokenError(Event event) {
        String provider = getIdentityProvider(event);
        String realmName = resolveRealmName(event.getRealmId());
        String clientId = event.getClientId();
        String error = event.getError();
        Tag[] tags = {
                tag("realm", realmName),
                tag("client_id", clientId),
                tag("error", error),
                tag("provider", provider),
        };
        metricRegistry.counter(KeycloakMetrics.OAUTH_CODE_TO_TOKEN_ERROR_TOTAL.getMetadata(), tags).inc();
    }

    protected void recordOauthCodeToToken(Event event) {
        String provider = getIdentityProvider(event);
        String realmName = resolveRealmName(event.getRealmId());
        String clientId = event.getClientId();
        Tag[] tags = {
                tag("realm", realmName),
                tag("client_id", clientId),
                tag("provider", provider),
        };
        metricRegistry.counter(KeycloakMetrics.OAUTH_CODE_TO_TOKEN_SUCCESS_TOTAL.getMetadata(), tags).inc();
    }

    protected void recordClientLogin(Event event) {
        String realmName = resolveRealmName(event.getRealmId());
        String clientId = event.getClientId();
        Tag[] tags = {
                tag("realm", realmName),
                tag("client_id", clientId),
        };
        metricRegistry.counter(KeycloakMetrics.AUTH_CLIENT_LOGIN_SUCCESS_TOTAL.getMetadata(), tags).inc();
    }

    protected void recordClientLoginError(Event event) {
        String realmName = resolveRealmName(event.getRealmId());
        String clientId = event.getClientId();
        String error = event.getError();
        Tag[] tags = {
                tag("realm", realmName),
                tag("client_id", clientId),
                tag("error", error),
        };
        metricRegistry.counter(KeycloakMetrics.AUTH_CLIENT_LOGIN_ERROR_TOTAL.getMetadata(), tags).inc();
    }

    protected void recordOauthTokenRefreshError(Event event) {
        String provider = getIdentityProvider(event);
        String realmName = resolveRealmName(event.getRealmId());
        String clientId = event.getClientId();
        String error = event.getError();
        Tag[] tags = {
                tag("realm", realmName),
                tag("client_id", clientId),
                tag("error", error),
                tag("provider", provider),
        };
        metricRegistry.counter(KeycloakMetrics.OAUTH_TOKEN_REFRESH_ERROR_TOTAL.getMetadata(), tags).inc();
    }

    protected void recordOauthTokenRefresh(Event event) {
        String realmName = resolveRealmName(event.getRealmId());
        String clientId = event.getClientId();
        Tag[] tags = {
                tag("realm", realmName),
                tag("client_id", clientId),
        };
        metricRegistry.counter(KeycloakMetrics.OAUTH_TOKEN_REFRESH_SUCCESS_TOTAL.getMetadata(), tags).inc();
    }

    protected void recordUserRegistrationError(Event event) {
        String provider = getIdentityProvider(event);
        String realmName = resolveRealmName(event.getRealmId());
        String clientId = event.getClientId();
        String error = event.getError();
        Tag[] tags = {
                tag("realm", realmName),
                tag("client_id", clientId),
                tag("error", error),
                tag("provider", provider),
        };
        metricRegistry.counter(KeycloakMetrics.AUTH_USER_REGISTER_ERROR_TOTAL.getMetadata(), tags).inc();
    }

    protected void recordUserRegistration(Event event) {
        String realmName = resolveRealmName(event.getRealmId());
        String clientId = event.getClientId();
        Tag[] tags = {
                tag("realm", realmName),
                tag("client_id", clientId),
        };
        metricRegistry.counter(KeycloakMetrics.AUTH_USER_REGISTER_SUCCESS_TOTAL.getMetadata(), tags).inc();
    }

    protected void recordUserLoginError(Event event) {
        String provider = getIdentityProvider(event);
        String realmName = resolveRealmName(event.getRealmId());
        Tag[] tags = {
                tag("realm", realmName),
                tag("client_id", event.getClientId()),
                tag("error", event.getError()),
                tag("provider", provider),
        };
        metricRegistry.counter(KeycloakMetrics.AUTH_USER_LOGIN_ERROR_TOTAL.getMetadata(), tags).inc();
    }

    protected void recordUserLogin(Event event) {
        String provider = getIdentityProvider(event);
        String realmName = resolveRealmName(event.getRealmId());
        Tag[] tags = {
                tag("realm", realmName),
                tag("provider", provider),
                tag("client_id", event.getClientId()),
        };
        metricRegistry.counter(KeycloakMetrics.AUTH_USER_LOGIN_SUCCESS_TOTAL.getMetadata(), tags).inc();
    }

    /**
     * Count generic user event
     *
     * @param event User event
     */
    protected void recordGenericUserEvent(Event event) {

        EventType eventType = event.getType();
        String counterName = USER_EVENT_METRIC_NAME;
        Metadata counterMetadata = genericCounters.get(counterName);
        String realmName = resolveRealmName(event.getRealmId());

        if (counterMetadata == null) {
            log.warnf("Counter %s for event type %s does not exist. Realm: %s", counterName, eventType.name(), realmName);
            return;
        }

        Tag[] tags = {
                tag("realm", realmName),
                tag("event_type", eventType.name()),
        };

        metricRegistry.counter(counterMetadata, tags).inc();
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
    private Metadata createCounter(String name, boolean isAdmin) {
        String description = isAdmin ? "Generic KeyCloak Admin event" : "Generic KeyCloak User event";
        return Metadata.builder().withName(name).withDescription(description).withType(MetricType.COUNTER).build();
    }

    private String resolveRealmName(String realmId) {
        return realmNameCache.computeIfAbsent(realmId, key -> {
            RealmModel realm = KeycloakSessionLookup.currentSession().realms().getRealm(key);
            return realm.getName();
        });
    }
}
