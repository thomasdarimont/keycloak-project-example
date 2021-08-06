package com.github.thomasdarimont.keycloak.custom.auth.trusteddevice;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.TokenCategory;
import org.keycloak.representations.JsonWebToken;

public class TrustedDeviceToken extends JsonWebToken {

    @JsonProperty("device_id")
    private String deviceId;

    @Override
    public TokenCategory getCategory() {
        return TokenCategory.INTERNAL;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}