package com.github.thomasdarimont.keycloak.custom.metrics;

import com.github.thomasdarimont.keycloak.custom.metrics.RealmMetricUpdater.MetricUpdateValue;
import com.github.thomasdarimont.keycloak.custom.metrics.RealmMetricUpdater.MultiMetricUpdateValues;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.Data;
import lombok.extern.jbosslog.JBossLog;
import org.hibernate.jpa.QueryHints;
import org.keycloak.common.Version;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@JBossLog
public class KeycloakMetrics {

    private static final ConcurrentMap<String, KeycloakMetric> keycloakMetrics = new ConcurrentHashMap<>();

    public static final KeycloakMetric INSTANCE_METADATA = newKeycloakMetric("keycloak_instance_metadata", "Keycloak Instance Metadata", Level.INSTANCE);

    public static final KeycloakMetric INSTANCE_METRICS_REFRESH = newKeycloakMetric("keycloak_instance_metrics_refresh_total_milliseconds", "Duration of Keycloak Metrics refresh in milliseconds.", Level.INSTANCE);

    public static final KeycloakMetric INVENTORY_REALMS_TOTAL = newKeycloakMetric("keycloak_inventory_realms_total", "Total realms per instance", Level.INSTANCE);

    public static final KeycloakMetric INVENTORY_SESSIONS_TOTAL = newKeycloakMetric("keycloak_inventory_sessions_total", "Total sessions per realm", Level.REALM);

    public static final KeycloakMetric INVENTORY_USERS_TOTAL = newKeycloakMetric("keycloak_inventory_users_total", "Total users per realm", Level.REALM);

    public static final KeycloakMetric INVENTORY_CLIENTS_TOTAL = newKeycloakMetric("keycloak_inventory_clients_total", "Total clients per realm", Level.REALM);

    public static final KeycloakMetric INVENTORY_GROUPS_TOTAL = newKeycloakMetric("keycloak_inventory_groups_total", "Total groups per realm", Level.REALM);

    public static final KeycloakMetric AUTH_CLIENT_LOGIN_ATTEMPT_TOTAL = newKeycloakMetric("keycloak_auth_client_login_attempt_total", "Total attempted client logins", Level.REALM);

    public static final KeycloakMetric AUTH_CLIENT_LOGIN_SUCCESS_TOTAL = newKeycloakMetric("keycloak_auth_client_login_success_total", "Total successful client logins", Level.REALM);

    public static final KeycloakMetric AUTH_CLIENT_LOGIN_ERROR_TOTAL = newKeycloakMetric("keycloak_auth_client_login_error_total", "Total errors during client logins", Level.REALM);

    public static final KeycloakMetric AUTH_USER_LOGIN_ATTEMPT_TOTAL = newKeycloakMetric("keycloak_auth_user_login_attempt_total", "Total attempted user logins", Level.REALM);

    public static final KeycloakMetric AUTH_USER_LOGIN_SUCCESS_TOTAL = newKeycloakMetric("keycloak_auth_user_login_success_total", "Total successful user logins", Level.REALM);

    public static final KeycloakMetric AUTH_USER_LOGIN_ERROR_TOTAL = newKeycloakMetric("keycloak_auth_user_login_error_total", "Total errors during user logins", Level.REALM);

    public static final KeycloakMetric AUTH_USER_LOGOUT_SUCCESS_TOTAL = newKeycloakMetric("keycloak_auth_user_logout_success_total", "Total successful user logouts", Level.REALM);

    public static final KeycloakMetric AUTH_USER_LOGOUT_ERROR_TOTAL = newKeycloakMetric("keycloak_auth_user_logout_error_total", "Total errors during user logouts", Level.REALM);

    public static final KeycloakMetric AUTH_USER_REGISTER_ATTEMPT_TOTAL = newKeycloakMetric("keycloak_auth_user_register_attempt_total", "Total attempted user registrations", Level.REALM);

    public static final KeycloakMetric AUTH_USER_REGISTER_SUCCESS_TOTAL = newKeycloakMetric("keycloak_auth_user_register_success_total", "Total user registrations", Level.REALM);

    public static final KeycloakMetric AUTH_USER_REGISTER_ERROR_TOTAL = newKeycloakMetric("keycloak_auth_user_register_error_total", "Total errors during user registrations", Level.REALM);

    public static final KeycloakMetric OAUTH_TOKEN_REFRESH_ATTEMPT_TOTAL = newKeycloakMetric("keycloak_oauth_token_refresh_attempt_total", "Total attempted token refreshes", Level.REALM);

    public static final KeycloakMetric OAUTH_TOKEN_REFRESH_SUCCESS_TOTAL = newKeycloakMetric("keycloak_oauth_token_refresh_success_total", "Total token refreshes", Level.REALM);

    public static final KeycloakMetric OAUTH_TOKEN_REFRESH_ERROR_TOTAL = newKeycloakMetric("keycloak_oauth_token_refresh_error_total", "Total errors during token refreshes", Level.REALM);

    public static final KeycloakMetric OAUTH_CODE_TO_TOKEN_ATTEMPT_TOTAL = newKeycloakMetric("keycloak_oauth_code_to_token_attempts_total", "Total attempts for code to token exchanges", Level.REALM);

    public static final KeycloakMetric OAUTH_CODE_TO_TOKEN_SUCCESS_TOTAL = newKeycloakMetric("keycloak_oauth_code_to_token_success_total", "Total code to token exchanges", Level.REALM);

    public static final KeycloakMetric OAUTH_CODE_TO_TOKEN_ERROR_TOTAL = newKeycloakMetric("keycloak_oauth_code_to_token_error_total", "Total errors during code to token exchanges", Level.REALM);

    public static final KeycloakMetric OAUTH_USERINFO_REQUEST_ATTEMPT_TOTAL = newKeycloakMetric("keycloak_oauth_userinfo_request_attempt_total", "Total attempted user info requests", Level.REALM);

    public static final KeycloakMetric OAUTH_USERINFO_REQUEST_SUCCESS_TOTAL = newKeycloakMetric("keycloak_oauth_userinfo_request_success_total", "Total user info requests", Level.REALM);

    public static final KeycloakMetric OAUTH_USERINFO_REQUEST_ERROR_TOTAL = newKeycloakMetric("keycloak_oauth_userinfo_request_error_total", "Total errors during user info requests", Level.REALM);

    public static final KeycloakMetric OAUTH_TOKEN_EXCHANGE_ATTEMPT_TOTAL = newKeycloakMetric("keycloak_oauth_token_exchange_attempt_total", "Total attempted token refreshes", Level.REALM);

    public static final KeycloakMetric OAUTH_TOKEN_EXCHANGE_SUCCESS_TOTAL = newKeycloakMetric("keycloak_oauth_token_exchange_success_total", "Total token refreshes", Level.REALM);

    public static final KeycloakMetric OAUTH_TOKEN_EXCHANGE_ERROR_TOTAL = newKeycloakMetric("keycloak_oauth_token_exchange_error_total", "Total errors during token refreshes", Level.REALM);

    private static KeycloakMetric newKeycloakMetric(String name, String description, Level level) {
        var metric = new KeycloakMetric(name, description, level);
        keycloakMetrics.put(name, metric);
        return metric;
    }

    private final MeterRegistry meterRegistry;

    private final KeycloakSessionFactory sessionFactory;

    private final KeycloakMetricStore store;

    public KeycloakMetrics(MeterRegistry meterRegistry, KeycloakSessionFactory sessionFactory) {
        this.meterRegistry = meterRegistry;
        this.sessionFactory = sessionFactory;


        this.store = new KeycloakMetricStore(sessionFactory, meterRegistry, new RealmMetricsUpdater() {
            @Override
            public void updateGlobalMetrics(KeycloakSession session, RealmMetricUpdater metricUpdater, long lastUpdateTimestamp) {
                // Performs the dynamic metrics collection on global level: this is called when metrics need to be refreshed
                log.debugf("Updating realm count");
                var em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
                Number realmCount = (Number) em.createQuery("select count(r) from RealmEntity r").setHint(QueryHints.HINT_READONLY, true).getSingleResult();
                metricUpdater.updateMetricValue(KeycloakMetrics.INVENTORY_REALMS_TOTAL, new MetricUpdateValue<>(realmCount), null);
                log.debugf("Updated realm count");
            }

            @Override
            public void updateRealmMetrics(KeycloakSession session, RealmMetricUpdater metricUpdater, RealmModel realm, long lastUpdateTimestamp) {
                // Performs the dynamic metrics collection on realm level: this is called when metrics need to be refreshed

                metricUpdater.updateMetricValue(KeycloakMetrics.INVENTORY_USERS_TOTAL, new MetricUpdateValue<>(session.users().getUsersCount(realm)), realm);
                metricUpdater.updateMetricValue(KeycloakMetrics.INVENTORY_CLIENTS_TOTAL, new MetricUpdateValue<>(session.clients().getClientsCount(realm)), realm);
                metricUpdater.updateMetricValue(KeycloakMetrics.INVENTORY_GROUPS_TOTAL, new MetricUpdateValue<>(session.groups().getGroupsCount(realm, false)), realm);


                var realmSessionStats = collectRealmSessionStats(session, realm);
                var metricUpdateValue = new MultiMetricUpdateValues(Map.of(Tags.of("type", "online"), realmSessionStats.getOnlineSessions(), Tags.of("type", "offline"), realmSessionStats.getOfflineSessions()));
                metricUpdater.updateMetricValue(KeycloakMetrics.INVENTORY_SESSIONS_TOTAL, metricUpdateValue, realm);
            }
        });
    }

    private RealmSessionStats collectRealmSessionStats(KeycloakSession session, RealmModel realm) {

        Map<String, Long> userSessionsCounts = session.sessions().getActiveClientSessionStats(realm, false);
        Map<String, Long> offlineUserSessionCounts = session.sessions().getActiveClientSessionStats(realm, false);

        long userSessionsCount = 0L;
        for (var entry : userSessionsCounts.entrySet()) {
            userSessionsCount += entry.getValue();
        }

        long offlineSessionsCount = 0L;
        for (var entry : offlineUserSessionCounts.entrySet()) {
            offlineSessionsCount += entry.getValue();
        }

        return new RealmSessionStats(userSessionsCount, offlineSessionsCount);
    }

    @Data
    static class RealmSessionStats {

        private final long onlineSessions;
        private final long offlineSessions;
    }

    public void registerInstanceMetrics() {

        Gauge.builder(INSTANCE_METADATA.getName(), () -> 0) //
                .description(INSTANCE_METADATA.getDescription()) //
                .tags(Tags.of("version", Version.VERSION, "buildtime", Version.BUILD_TIME)) //
                .register(meterRegistry);

        Gauge.builder(INSTANCE_METRICS_REFRESH.getName(), () -> 0) //
                .description(INSTANCE_METRICS_REFRESH.getDescription()) //
                .register(meterRegistry);
    }

    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }

    public void removeRealmMetrics(RealmModel realm) {

        log.infof("Remove metrics for deleted realm %s", realm.getName());

        var realmTag = Tags.of("realm", realm.getName());

        var snapshot = new ArrayList<>(keycloakMetrics.values());
        for (var keycloakMetric : snapshot) {
            Gauge gauge = meterRegistry.find(keycloakMetric.getName()).tags(realmTag).gauge();
            if (gauge != null) {
                meterRegistry.remove(gauge);
                continue;
            }

            Counter counter = meterRegistry.find(keycloakMetric.getName()).tags(realmTag).counter();
            if (counter != null) {
                meterRegistry.remove(counter);
                continue;
            }

            Meter meter = meterRegistry.find(keycloakMetric.getName()).tags(realmTag).meter();
            if (meter != null) {
                meterRegistry.remove(meter);
            }
        }
    }

    public void initialize() {
        store.getMetricValue(null);
    }

    public enum Level {
        INSTANCE, REALM
    }

}
