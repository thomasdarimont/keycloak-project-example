package com.github.thomasdarimont.keycloak.custom.auth.opa;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.thomasdarimont.keycloak.custom.auth.authzen.AuthZen;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
public class OpaAccessResponse {

    private AuthZen.AccessResponse result;

    private Map<String, Object> additionalData;

    public OpaAccessResponse(AuthZen.AccessResponse result) {
        this.result = result;
    }

    @JsonIgnore
    public boolean isAllowed() {
        return result != null && result.decision();
    }

    public String getHint() {
        if (result == null) {
            return null;
        }
        if (result.context() == null) {
            return null;
        }
        Object hint = result.context().get("hint");
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
