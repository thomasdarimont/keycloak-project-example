package com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.credentials;

import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.TrustedDeviceToken;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.UserModel;

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

    public static TrustedDeviceCredentialModel lookupTrustedDevice(UserModel user, TrustedDeviceToken trustedDeviceToken) {

        if (user == null) {
            return null;
        }

        if (trustedDeviceToken == null) {
            return null;
        }

        var credentialModel = user.credentialManager().getStoredCredentialsByTypeStream(TrustedDeviceCredentialModel.TYPE)
                .filter(cm -> cm.getSecretData().equals(trustedDeviceToken.getDeviceId()))
                .findAny().orElse(null);

        if (credentialModel == null) {
            return null;
        }

        return new TrustedDeviceCredentialModel(credentialModel.getId(), credentialModel.getUserLabel(), credentialModel.getSecretData());
    }
}