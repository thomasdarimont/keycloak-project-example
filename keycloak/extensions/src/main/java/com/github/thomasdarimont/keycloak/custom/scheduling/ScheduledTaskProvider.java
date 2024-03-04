package com.github.thomasdarimont.keycloak.custom.scheduling;

import org.keycloak.provider.Provider;
import org.keycloak.timer.ScheduledTask;

public interface ScheduledTaskProvider extends Provider {

    ScheduledTask getScheduledTask();

    long getInterval();

    String getTaskName();
}