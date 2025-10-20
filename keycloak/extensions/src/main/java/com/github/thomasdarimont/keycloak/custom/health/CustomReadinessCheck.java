package com.github.thomasdarimont.keycloak.custom.health;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import java.util.concurrent.atomic.AtomicBoolean;

@Readiness
@JBossLog
@ApplicationScoped
public class CustomReadinessCheck implements HealthCheck {

    private static final AtomicBoolean STOPPED = new AtomicBoolean(false);

    @Override
    public HealthCheckResponse call() {
        return STOPPED.get() ? HealthCheckResponse.down("CONTAINER_STATUS") : HealthCheckResponse.up("CONTAINER_STATUS");
    }

    static {
        log.info("Adding CustomReadinessCheck for SIG TERM");
//        SignalHandler signalHandler = sig -> {
//            log.infof("Detected SIG %s, marking this instance as unavailable", sig.getName());
//            STOPPED.set(true);
//        };
//        try {
//            Signal.handle(new Signal("TERM"), signalHandler);
//        } catch (Exception e) {
//            log.warnf("Failed to register signal handler: ", e.getMessage());
//        }
    }
}