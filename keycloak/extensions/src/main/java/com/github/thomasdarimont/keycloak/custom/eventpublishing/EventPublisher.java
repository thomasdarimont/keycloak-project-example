package com.github.thomasdarimont.keycloak.custom.eventpublishing;

import java.util.Map;

public interface EventPublisher {

    void publish(String topic, Object event);

    Map<String, String> getOperationalInfo();

    void init();

    void close();
}
