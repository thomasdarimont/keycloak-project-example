package com.github.thomasdarimont.keycloak.custom.auth.mfa.emailcode;

import org.keycloak.credential.CredentialModel;

public class EmailCodeCredentialModel extends CredentialModel {

    public static final String TYPE = "mfa-email-code";

    public EmailCodeCredentialModel() {
        setType(TYPE);
        setUserLabel("Email OTP");
    }
}
