package com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.credentials;

import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.DeviceToken;
import org.keycloak.credential.CredentialModel;

public class TrustedDeviceCredentialModel extends CredentialModel {

    public static final String TYPE = "acme-trusted-device";

    private DeviceToken deviceToken;

    private String deviceId;

    public TrustedDeviceCredentialModel(String deviceName, DeviceToken deviceToken) {
        this.setUserLabel(deviceName);
        this.deviceToken = deviceToken;
    }

    public TrustedDeviceCredentialModel(String deviceName, String deviceId) {
        this.setUserLabel(deviceName);
        this.deviceId = deviceId;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public DeviceToken getDeviceToken() {
        return deviceToken;
    }

    public String getDeviceId() {
        return deviceId;
    }
}
