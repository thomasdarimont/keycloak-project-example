package com.github.thomasdarimont.keycloak.custom.consent;

import lombok.Data;
import org.keycloak.models.UserModel;

@Data
public class ScopeFieldBean {

    private final ScopeField scopeField;

    private final UserModel user;

    public String getName() {
        return scopeField.getName();
    }

    public String getType() {
        return scopeField.getType();
    }

    public String getValue() {
        return scopeField.getValueAccessor().apply(user);
    }
}
