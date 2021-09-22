package com.github.thomasdarimont.keycloak.custom.metrics;

import io.smallrye.metrics.MetricRegistries;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Tag;
import org.keycloak.common.Version;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import javax.persistence.EntityManager;

import static com.github.thomasdarimont.keycloak.custom.metrics.KeycloakMetric.newMetric;

@JBossLog
public final class KeycloakMetrics implements RealmMetricsUpdater {

    public static final KeycloakMetric SERVER_VERSION = newMetric(Metadata.builder()
            .withName("keycloak_server_version")
            .withDescription("Keycloak Server Version")
            .withType(MetricType.GAUGE)
            .build());

    public static final KeycloakMetric METRICS_REFRESH = newMetric(Metadata.builder()
            .withName("keycloak_metrics_refresh_total_milliseconds")
            .withDescription("Duration of Keycloak Metrics refresh in milliseconds.")
            .withType(MetricType.GAUGE)
            .build());

    public static final KeycloakMetric REALMS_TOTAL = newMetric(Metadata.builder()
            .withName("keycloak_realms_total")
            .withDescription("Total realms")
            .withType(MetricType.GAUGE)
            .build());

    public static final KeycloakMetric USERS_TOTAL = newMetric(Metadata.builder()
            .withName("keycloak_users_total")
            .withDescription("Total users")
            .withType(MetricType.GAUGE)
            .build());

    public static final KeycloakMetric CLIENTS_TOTAL = newMetric(Metadata.builder()
            .withName("keycloak_clients_total")
            .withDescription("Total clients")
            .withType(MetricType.GAUGE)
            .build());

    public static final KeycloakMetric GROUPS_TOTAL = newMetric(Metadata.builder()
            .withName("keycloak_groups_total")
            .withDescription("Total groups")
            .withType(MetricType.GAUGE)
            .build());

    public static final KeycloakMetric CLIENT_LOGIN_SUCCESS_TOTAL = newMetric(Metadata.builder()
            .withName("keycloak_client_login_success_total")
            .withDescription("Total successful client logins")
            .withType(MetricType.COUNTER)
            .build());

    public static final KeycloakMetric CLIENT_LOGIN_ERROR_TOTAL = newMetric(Metadata.builder()
            .withName("keycloak_client_login_error_total")
            .withDescription("Total errors during client logins")
            .withType(MetricType.COUNTER)
            .build());

    public static final KeycloakMetric USER_LOGIN_SUCCESS_TOTAL = newMetric(Metadata.builder()
            .withName("keycloak_user_login_success_total")
            .withDescription("Total successful user logins")
            .withType(MetricType.COUNTER)
            .build());

    public static final KeycloakMetric USER_LOGIN_ERROR_TOTAL = newMetric(Metadata.builder()
            .withName("keycloak_user_login_error_total")
            .withDescription("Total errors during user logins")
            .withType(MetricType.COUNTER)
            .build());

    public static final KeycloakMetric USER_LOGOUT_SUCCESS_TOTAL = newMetric(Metadata.builder()
            .withName("keycloak_user_logout_success_total")
            .withDescription("Total successful user logouts")
            .withType(MetricType.COUNTER)
            .build());

    public static final KeycloakMetric USER_LOGOUT_ERROR_TOTAL = newMetric(Metadata.builder()
            .withName("keycloak_user_logout_error_total")
            .withDescription("Total errors during user logouts")
            .withType(MetricType.COUNTER)
            .build());

    public static final KeycloakMetric USER_REGISTER_SUCCESS_TOTAL = newMetric(Metadata.builder()
            .withName("keycloak_user_register_success_total")
            .withDescription("Total user registrations")
            .withType(MetricType.COUNTER)
            .build());

    public static final KeycloakMetric USER_REGISTER_ERROR_TOTAL = newMetric(Metadata.builder()
            .withName("keycloak_user_register_error_total")
            .withDescription("Total errors during user registrations")
            .withType(MetricType.COUNTER)
            .build());

    public static final KeycloakMetric OAUTH_TOKEN_REFRESH_SUCCESS_TOTAL = newMetric(Metadata.builder()
            .withName("keycloak_oauth_token_refresh_success_total")
            .withDescription("Total token refreshes")
            .withType(MetricType.COUNTER)
            .build());

    public static final KeycloakMetric OAUTH_TOKEN_REFRESH_ERROR_TOTAL = newMetric(Metadata.builder()
            .withName("keycloak_oauth_token_refresh_error_total")
            .withDescription("Total errors during token refreshes")
            .withType(MetricType.COUNTER)
            .build());

    public static final KeycloakMetric OAUTH_CODE_TO_TOKEN_SUCCESS_TOTAL = newMetric(Metadata.builder()
            .withName("keycloak_oauth_code_to_token_success_total")
            .withDescription("Total code to token exchanges")
            .withType(MetricType.COUNTER)
            .build());

    public static final KeycloakMetric OAUTH_CODE_TO_TOKEN_ERROR_TOTAL = newMetric(Metadata.builder()
            .withName("keycloak_oauth_code_to_token_error_total")
            .withDescription("Total errors during code to token exchanges")
            .withType(MetricType.COUNTER)
            .build());

    public void registerMetrics(MetricRegistry metricRegistry, KeycloakMetricAccessor metricAccessor) {

        // we should only register metrics here and avoid expensive initializations!

        // static metric that exposes the keycloak server version
        metricRegistry.register(SERVER_VERSION.getMetadata(), (Gauge<Double>) () -> 0.0, tag("version", Version.VERSION));

        // this dynamic metric gauge triggers a metrics collection for realm and global metrics.
        metricRegistry.register(METRICS_REFRESH.getMetadata(), (Gauge<Double>) () -> metricAccessor.getMetricValue(METRICS_REFRESH.getKey()));
    }

    public void updateRealmMetrics(KeycloakSession session, MetricUpdater metricUpdater, RealmModel realm, long lastUpdateTimestamp) {

        // Performs the dynamic metrics collection on realm level: this is called when metrics need to be refreshed

        metricUpdater.updateMetricValue(USERS_TOTAL, session.users().getUsersCount(realm), realm);
        metricUpdater.updateMetricValue(CLIENTS_TOTAL, session.clients().getClientsCount(realm), realm);
        metricUpdater.updateMetricValue(GROUPS_TOTAL, session.groups().getGroupsCount(realm, false), realm);
    }

    @Override
    public void updateGlobalMetrics(KeycloakSession session, MetricUpdater metricUpdater, long lastUpdateTimestamp) {

        // Performs the dynamic metrics collection on global level: this is called when metrics need to be refreshed
        log.debugf("Updating realm count");
        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        Number realmCount = (Number) em.createQuery("select count(r) from RealmEntity r").getSingleResult();
        metricUpdater.updateMetricValue(REALMS_TOTAL, realmCount, null);
        log.debugf("Updated realm count");
    }

    private static final Tag[] EMPTY_TAGS = {};

    public static Tag[] emptyTag() {
        return EMPTY_TAGS;
    }

    public static Tag tag(String name, String value) {
        return new Tag(name, value);
    }

    public static MetricRegistry lookupMetricRegistry() {
        return MetricRegistries.get(MetricRegistry.Type.APPLICATION);
    }

}
