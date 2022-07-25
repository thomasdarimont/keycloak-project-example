package com.github.thomasdarimont.keycloak.custom.consent;

import org.keycloak.models.UserModel;

import java.util.function.Function;

public class KeycloakProfileAttribute extends ProfileAttribute {

    private final Function<UserModel, String> valueAccessor;

    public KeycloakProfileAttribute(String name, String claimName, String type, boolean required, boolean readonly, Function<UserModel, String> valueAccessor) {
        super(name, claimName, type, null, required, readonly);
        this.valueAccessor = valueAccessor;
    }

    public String getValue(UserModel user) {
        return this.valueAccessor.apply(user);
    }
}
