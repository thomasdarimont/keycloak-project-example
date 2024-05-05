package com.github.thomasdarimont.keycloak.custom.eventpublishing;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.Map;

@JBossLog
@RequiredArgsConstructor
public class NatsEventPublisher implements EventPublisher {

    private final String url;

    private final String username;

    private final String password;

    private Connection connection;

    public void publish(String subject, Object event) {

        byte[] messageBytes = null;
        try {
            messageBytes = JsonSerialization.writeValueAsBytes(event);
        } catch (IOException e) {
            log.warn("Could not serialize event", e);
        }

        if (messageBytes == null) {
            return;
        }

        try {
            connection.publish(subject, messageBytes);
        } catch (Exception e) {
            log.warn("Could not publish event", e);
        }

    }

    public Map<String, String> getOperationalInfo() {
        return Map.of("url", url, "nats-username", username, "status", getStatus());
    }

    public String getStatus() {
        if (connection == null) {
            return null;
        }
        return connection.getStatus().name();
    }

    public void init() {

        Options options = Options.builder() //
                .connectionName("keycloak") //
                .userInfo(username, password) //
                .server(url) //
                .build();

        try {
            connection = Nats.connect(options);
        } catch (Exception e) {
            throw new RuntimeException("Could not connect to nats server", e);
        }
    }

    public void close() {
        if (connection == null) {
            return;
        }

        try {
            connection.close();
        } catch (Exception e) {
            log.warn("Could not close connection", e);
        }
    }
}
