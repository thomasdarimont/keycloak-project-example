package com.github.thomasdarimont.keycloak.custom.auth.mfa;

public class MfaInfo {

    private final String label;

    public MfaInfo(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
