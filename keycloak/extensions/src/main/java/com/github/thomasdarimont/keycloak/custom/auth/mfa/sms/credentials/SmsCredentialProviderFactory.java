package com.github.thomasdarimont.keycloak.custom.auth.mfa.sms.credentials;

import com.google.auto.service.AutoService;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.CredentialProviderFactory;
import org.keycloak.models.KeycloakSession;

@AutoService(CredentialProviderFactory.class)
public class SmsCredentialProviderFactory implements CredentialProviderFactory<SmsCredentialProvider> {

    public static final String ID = "acme-mfa-sms";

    @Override
    public CredentialProvider<CredentialModel> create(KeycloakSession session) {
        return new SmsCredentialProvider(session);
    }

    @Override
    public String getId() {
        return ID;
    }
}
