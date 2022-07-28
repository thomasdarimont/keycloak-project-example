package com.github.thomasdarimont.keycloak.custom.consent;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.keycloak.models.UserModel;
import org.keycloak.utils.StringUtil;

import java.util.Map;
import java.util.Set;

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

    public Set<String> getAllowedValues() {
        return attribute.getAllowedValues();
    }

    public Map<String, String> getAnnotations() {
        return attribute.getAnnotations();
    }

    public String getValue() {

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
