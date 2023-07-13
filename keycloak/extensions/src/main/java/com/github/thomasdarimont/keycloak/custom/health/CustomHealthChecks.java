package com.github.thomasdarimont.keycloak.custom.health;

import com.github.thomasdarimont.keycloak.custom.support.KeycloakUtil;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;
import org.keycloak.common.Version;

import javax.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.CDI;
import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.sql.Connection;

/**
 * Example for custom health checks
 *
 * <p>Keycloak</p>
 * <a href="http://localhost:9990/health">Example Keycloak health checks</a>
 *
 * <p>Keycloak.X (with custom http-relative-path=/auth</p>
 * <a href="http://localhost:8080/auth/health">Example Keycloak.X health checks</a>
 */
@JBossLog
@ApplicationScoped
public class CustomHealthChecks {

    private static final HealthCheckResponseBuilder KEYCLOAK_SERVER_HEALTH_CHECK = //
            HealthCheckResponse.named("keycloak:server") //
                    .withData("version", Version.VERSION) //
                    .withData("startupTimestamp", ManagementFactory.getRuntimeMXBean().getStartTime());

    private static final int DB_CONN_VALID_TIMEOUT_MILLIS = 1000;

    // JNDI Lookup ignored by Keycloak.X
    @Resource(lookup = "java:jboss/datasources/KeycloakDS")
    private DataSource keycloakDatasource;

    /**
     * <a href="http://localhost:9990/health/live">Example Keycloak liveness check</a>
     * <a href="http://localhost:8080/auth/health/ready">Example Keycloak.X liveness check</a>
     *
     * @return
     */
    @Produces
    @Liveness
    HealthCheck serverCheck() {
        return () -> {
            log.debug("Liveness check");
            return KEYCLOAK_SERVER_HEALTH_CHECK.up().build();
        };
    }

    /**
     * <a href="http://localhost:9990/health/ready">Example Keycloak readiness check</a>
     * <a href="http://localhost:8080/auth/health/ready">Example Keycloak.X readiness check</a>
     *
     * @return
     */
    @Produces
    @Readiness
    HealthCheck databaseCheck() {
        HealthCheckResponseBuilder databaseHealth = HealthCheckResponse.named("keycloak:database");

        return () -> {
            log.debug("Readiness check");
            return (isDatabaseReady() ? databaseHealth.up() : databaseHealth.down()).build();
        };
    }

    private boolean isDatabaseReady() {
        DataSource dataSource = getDataSource();

        if (dataSource == null) {
            return false;
        }

        try (Connection con = dataSource.getConnection()) {
            return con.isValid(DB_CONN_VALID_TIMEOUT_MILLIS);
        } catch (Exception ex) {
            return false;
        }
    }

    private DataSource getDataSource() {

        if (KeycloakUtil.isRunningOnKeycloak()) {
            return keycloakDatasource;
        }

        // Manual lookup for datasource via CDI
        return CDI.current().select(DataSource.class).get();
    }

}
