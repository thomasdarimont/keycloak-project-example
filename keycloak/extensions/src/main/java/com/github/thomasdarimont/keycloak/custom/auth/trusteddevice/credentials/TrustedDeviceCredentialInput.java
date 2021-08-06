package com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.credentials;

import org.keycloak.credential.CredentialInput;

public class TrustedDeviceCredentialInput implements CredentialInput {

    private final String credentialId;

    private final String type;

    private final String challengeResponse;

    public TrustedDeviceCredentialInput(String credentialId, String type, String challengeResponse) {
        this.credentialId = credentialId;
        this.type = type;
        this.challengeResponse = challengeResponse;
    }

    @Override
    public String getCredentialId() {
        return credentialId;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getChallengeResponse() {
        return challengeResponse;
    }
}
