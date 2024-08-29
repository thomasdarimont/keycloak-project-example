package com.github.thomasdarimont.keycloak.custom.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.CDI;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;
import org.keycloak.common.Version;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.time.Instant;

/**
 * Example for custom health checks
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
                    .withData("startupTime", Instant.ofEpochMilli(ManagementFactory.getRuntimeMXBean().getStartTime()).toString());

    /**
     * <a href="http://localhost:8080/auth/health/live">Example Keycloak.X liveness check</a>
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
}
