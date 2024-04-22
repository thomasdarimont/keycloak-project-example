package com.github.thomasdarimont.keycloak.custom.eventpublishing;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class NatsEventPublisher {

    public void publish(Object event) {

        String natsURL = "nats://acme-nats:4222";

        Options options = Options.builder() //
                .connectionName("keycloak") //
                .server(natsURL) //
                .build();

        try (Connection nc = Nats.connect(options)) {
            byte[] messageBytes = JsonSerialization.writeValueAsBytes(event);
            nc.publish("acme.iam.keycloak.admin", messageBytes);
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }
}
