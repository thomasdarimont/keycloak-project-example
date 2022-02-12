package com.github.thomasdarimont.keycloak.custom.config;

import lombok.RequiredArgsConstructor;
import org.keycloak.models.ClientModel;

@RequiredArgsConstructor
public class ClientConfig implements ConfigAccessor {

    private final ClientModel client;

    @Override
    public String getType() {
        return "Client";
    }

    @Override
    public String getSource() {
        return client.getClientId();
    }

    public String getValue(String key) {
        return client.getAttribute(key);
    }

    public boolean containsKey(String key) {
        return client.getAttributes().containsKey(key);
    }
}

