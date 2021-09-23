package com.github.thomasdarimont.keycloak.custom.metrics.events;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;

public class MetricEventListenerProvider implements EventListenerProvider {

    private final MetricsRecorder recorder;

    public MetricEventListenerProvider() {
        this.recorder = new MetricsRecorder();
    }

    @Override
    public void onEvent(Event event) {
        recorder.lookupUserEventHandler(event).accept(event);
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        recorder.onEvent(event, includeRepresentation);
    }

    @Override
    public void close() {
        // NOOP
    }
}
