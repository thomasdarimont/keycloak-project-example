package com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.credentials;

import com.google.auto.service.AutoService;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.CredentialProviderFactory;
import org.keycloak.models.KeycloakSession;

@AutoService(CredentialProviderFactory.class)
public class TrustedDeviceCredentialProviderFactory implements CredentialProviderFactory<TrustedDeviceCredentialProvider> {

    public static final String ID = "custom-trusted-device";

    @Override
    public CredentialProvider<CredentialModel> create(KeycloakSession session) {
        return new TrustedDeviceCredentialProvider(session);
    }

    @Override
    public String getId() {
        return ID;
    }
}