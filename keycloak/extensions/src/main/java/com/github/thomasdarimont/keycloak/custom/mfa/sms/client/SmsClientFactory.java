package com.github.thomasdarimont.keycloak.custom.mfa.sms.client;

import com.github.thomasdarimont.keycloak.custom.mfa.sms.client.mock.MockSmsClient;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class SmsClientFactory {

    public static final String MOCK_CLIENT = "mock";

    public static SmsClient createClient(String name, Map<String, String> config) {
        Objects.requireNonNull(name);

        switch (name) {
            case MOCK_CLIENT:
                return new MockSmsClient(config);
            default:
                throw new IllegalArgumentException("SMS Client " + name + " not supported.");
        }
    }

    public static Set<String> getAvailableClientNames() {
        return new LinkedHashSet<>(Arrays.asList(MOCK_CLIENT));
    }
}
