package com.github.thomasdarimont.keycloak.custom.auth.opa;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
public class OpaAccessResponse {

    private Map<String, Object> result;

    private Map<String, Object> additionalData;

    public OpaAccessResponse(Map<String, Object> result) {
        this.result = result;
    }

    @JsonIgnore
    public boolean isAllowed() {
        return result != null && Boolean.parseBoolean(String.valueOf(result.get("allow")));
    }

    public String getHint() {
        if (result == null) {
            return null;
        }
        Object hint = result.get("hint");
        if (!(hint instanceof String)) {
            return null;
        }
        return (String) hint;
    }

    @JsonAnySetter
    public void handleUnknownProperty(String key, Object value) {
        if (additionalData == null) {
            additionalData = new HashMap<>();
        }
        this.additionalData.put(key, value);
    }
}
