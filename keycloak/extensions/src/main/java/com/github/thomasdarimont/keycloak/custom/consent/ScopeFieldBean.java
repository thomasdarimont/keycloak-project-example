package com.github.thomasdarimont.keycloak.custom.consent;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.keycloak.models.UserModel;
import org.keycloak.utils.StringUtil;

@Data
public class ScopeFieldBean {

    @JsonIgnore
    private final ProfileAttribute attribute;

    @JsonIgnore
    private final UserModel user;

    public String getName() {
        return attribute.getName();
    }

    public String getType() {
        return attribute.getType();
    }

    public String getValue() {
        if (attribute instanceof KeycloakProfileAttribute) {
            return ((KeycloakProfileAttribute) attribute).getValue(user);
        }
        String value = attribute.getValue();
        if (StringUtil.isBlank(value) && "email".equals(getName())) {
            value = user.getEmail();
        }
        return value;
    }

    public boolean isRequired() {
        return attribute.isRequired();
    }

    public boolean isReadonly() {
        return attribute.isReadonly();
    }
}
