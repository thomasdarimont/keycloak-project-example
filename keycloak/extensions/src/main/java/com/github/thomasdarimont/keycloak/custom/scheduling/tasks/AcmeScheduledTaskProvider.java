package com.github.thomasdarimont.keycloak.custom.scheduling.tasks;

import com.github.thomasdarimont.keycloak.custom.scheduling.ScheduledTaskProvider;
import com.github.thomasdarimont.keycloak.custom.scheduling.ScheduledTaskProviderFactory;
import com.google.auto.service.AutoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ServerInfoAwareProviderFactory;
import org.keycloak.timer.ScheduledTask;

import java.time.Duration;
import java.util.Map;

@JBossLog
@RequiredArgsConstructor
public class AcmeScheduledTaskProvider implements ScheduledTaskProvider {

    private final String taskName;

    private final Duration interval;

    @Override
    public ScheduledTask getScheduledTask() {

        return (session -> {

// do something with the cluster
//            ClusterProvider cluster = session.getProvider(ClusterProvider.class);
//            int taskTimeoutSeconds = 1000;
//            String taskKey = getTaskName() + "::scheduled";
//            cluster.executeIfNotExecuted(taskKey, taskTimeoutSeconds, () -> {
//                // do something here
//                return null;
//            });

// do something here on every instance
            log.infof("Running %s", getTaskName());
        });
    }

    @Override
    public long getInterval() {
        return interval.toMillis();
    }

    @Override
    public String getTaskName() {
        return taskName;
    }

    @Override
    public void close() {
        // NOOP
    }

    @AutoService(ScheduledTaskProviderFactory.class)
    public static class Factory extends ScheduledTaskProviderFactory implements ServerInfoAwareProviderFactory {

        private Duration interval;

        private String taskName;

        @Override
        public String getId() {
            return "acme-custom-task";
        }

        @Override
        public ScheduledTaskProvider create(KeycloakSession session) {
            return new AcmeScheduledTaskProvider(taskName, interval);
        }

        @Override
        public void init(Config.Scope config) {
            interval = Duration.ofMillis(config.getLong("interval", 60000L));
            taskName = config.get("task-name", "acme-custom-task");
        }

        @Override
        public Map<String, String> getOperationalInfo() {
            return Map.of("taskName", taskName, "interval", interval.toString());
        }
    }
}