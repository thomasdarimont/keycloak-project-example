package com.github.thomasdarimont.keycloak.webapp.support;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class IntrospectionResult {

    private boolean active;

    private Map<String, Object> data = new HashMap<>();

    @JsonAnySetter
    public void setDataEntry(String key, Object value) {
        data.put(key, value);
    }
}
