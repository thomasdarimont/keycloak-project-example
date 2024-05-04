package com.github.thomasdarimont.keycloak.custom.eventpublishing;

import lombok.extern.jbosslog.JBossLog;

import java.util.Map;

@JBossLog
public class NoopPublisher implements EventPublisher{

    @Override
    public void publish(String topic, Object event) {
        // NOOP
    }

    @Override
    public Map<String, String> getOperationalInfo() {
        return Map.of();
    }

    @Override
    public void init() {
        // NOOP
    }

    @Override
    public void close() {
        // NOOP
    }
}
