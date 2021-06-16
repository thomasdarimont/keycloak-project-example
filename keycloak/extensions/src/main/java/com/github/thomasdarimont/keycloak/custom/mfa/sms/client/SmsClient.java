package com.github.thomasdarimont.keycloak.custom.mfa.sms.client;

public interface SmsClient {

    void send(String sender, String receiver, String message);

}
