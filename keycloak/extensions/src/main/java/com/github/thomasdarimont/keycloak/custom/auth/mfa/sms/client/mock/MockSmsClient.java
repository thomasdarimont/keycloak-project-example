package com.github.thomasdarimont.keycloak.custom.auth.mfa.sms.client.mock;

import com.github.thomasdarimont.keycloak.custom.auth.mfa.sms.client.SmsClient;
import lombok.extern.jbosslog.JBossLog;

import java.util.Map;

@JBossLog
public class MockSmsClient implements SmsClient {

    private final Map<String, String> config;

    public MockSmsClient(Map<String, String> config) {
        this.config = config;
    }

    @Override
    public void send(String sender, String receiver, String message) {
        log.infof("##### Sending SMS.%nsender='%s' phoneNumber='%s' message='%s'", sender, receiver, message);
    }
}
