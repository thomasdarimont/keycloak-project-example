package com.github.thomasdarimont.keycloak.custom.consent;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.keycloak.models.UserModel;

@Data
public class ScopeFieldBean {

    @JsonIgnore
    private final ScopeField scopeField;

    @JsonIgnore
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

    public boolean isRequired() {
        return scopeField.isRequired();
    }
}
