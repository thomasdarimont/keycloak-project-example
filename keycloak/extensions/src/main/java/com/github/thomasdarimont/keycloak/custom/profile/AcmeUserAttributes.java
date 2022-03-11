package com.github.thomasdarimont.keycloak.custom.profile;

public enum AcmeUserAttributes {

    ACCOUNT_DELETION_REQUESTED_AT("deletion-requested-at");

    public static final String PREFIX = "acme:";

    private final String attributeName;

    AcmeUserAttributes(String name) {
        this.attributeName = PREFIX + name;
    }

    public String getAttributeName() {
        return attributeName;
    }
}
