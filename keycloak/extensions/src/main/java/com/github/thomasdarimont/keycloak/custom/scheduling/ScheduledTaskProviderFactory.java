package com.github.thomasdarimont.keycloak.custom.scheduling;

import lombok.extern.jbosslog.JBossLog;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.timer.TimerProvider;

@JBossLog
public abstract class ScheduledTaskProviderFactory implements ProviderFactory<ScheduledTaskProvider> {

    private KeycloakSessionFactory keycloakSessionFactory;

    @Override
    public final void postInit(KeycloakSessionFactory keycloakSessionFactory) {
        this.keycloakSessionFactory = keycloakSessionFactory;
        keycloakSessionFactory.register((event) -> {
            if (event instanceof PostMigrationEvent) {
                var session = keycloakSessionFactory.create();
                var timerProvider = session.getProvider(TimerProvider.class);
                var scheduledTaskProvider = create(session);
                timerProvider.scheduleTask(scheduledTaskProvider.getScheduledTask(), scheduledTaskProvider.getInterval(), scheduledTaskProvider.getTaskName());
                log.infof("Scheduled Task %s", scheduledTaskProvider.getTaskName());
            }
        });
    }

    @Override
    public final void close() {
        var session = keycloakSessionFactory.create();
        var timerProvider = session.getProvider(TimerProvider.class);
        var scheduledTaskProvider = this.create(session);
        timerProvider.cancelTask(scheduledTaskProvider.getTaskName());
        log.infof("Cancelled Task %s", scheduledTaskProvider.getTaskName());
    }
}