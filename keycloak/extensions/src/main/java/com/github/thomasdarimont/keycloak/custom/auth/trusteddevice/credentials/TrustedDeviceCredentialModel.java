package com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.credentials;

import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.TrustedDeviceToken;
import org.keycloak.credential.CredentialModel;

public class TrustedDeviceCredentialModel extends CredentialModel {

    public static final String TYPE = "acme-trusted-device";

    private TrustedDeviceToken trustedDeviceToken;

    private String deviceId;

    public TrustedDeviceCredentialModel(String deviceName, TrustedDeviceToken trustedDeviceToken) {
        this.setUserLabel(deviceName);
        this.trustedDeviceToken = trustedDeviceToken;
    }

    public TrustedDeviceCredentialModel(String id, String deviceName, String deviceId) {
        this.setId(id);
        this.setUserLabel(deviceName);
        this.deviceId = deviceId;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public TrustedDeviceToken getDeviceToken() {
        return trustedDeviceToken;
    }

    public String getDeviceId() {
        return deviceId;
    }
}
