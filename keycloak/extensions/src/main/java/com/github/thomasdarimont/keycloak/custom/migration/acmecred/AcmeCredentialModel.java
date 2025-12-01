package com.github.thomasdarimont.keycloak.custom.migration.acmecred;

import org.keycloak.credential.CredentialModel;
import org.keycloak.models.credential.dto.PasswordCredentialData;
import org.keycloak.models.credential.dto.PasswordSecretData;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;

import static org.keycloak.utils.StringUtil.isBlank;

public class AcmeCredentialModel extends CredentialModel {

    public static final String TYPE = "acme-password";

    private final PasswordCredentialData acmeCredentialData;

    private final PasswordSecretData acmeSecretData;

    public AcmeCredentialModel(PasswordCredentialData acmeCredentialData, PasswordSecretData acmeSecretData) {
        this.acmeCredentialData = acmeCredentialData;
        this.acmeSecretData = acmeSecretData;
    }

    public static AcmeCredentialModel createFromCredentialModel(CredentialModel credentialModel) {
        try {
            PasswordCredentialData credentialData = isBlank(credentialModel.getCredentialData())
                    ? null
                    : JsonSerialization.readValue(credentialModel.getCredentialData(), PasswordCredentialData.class);
            PasswordSecretData secretData = isBlank(credentialModel.getSecretData())
                    ? null
                    : JsonSerialization.readValue(credentialModel.getSecretData(), PasswordSecretData.class);
            AcmeCredentialModel acmeCredentialModel = new AcmeCredentialModel(credentialData, secretData);
            acmeCredentialModel.setCreatedDate(credentialModel.getCreatedDate());
            acmeCredentialModel.setCredentialData(credentialModel.getCredentialData());
            acmeCredentialModel.setId(credentialModel.getId());
            acmeCredentialModel.setSecretData(credentialModel.getSecretData());
            acmeCredentialModel.setType(TYPE);
            acmeCredentialModel.setUserLabel(credentialModel.getUserLabel());

            return acmeCredentialModel;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public PasswordCredentialData getAcmeCredentialData() {
        return acmeCredentialData;
    }

    public PasswordSecretData getAcmeSecretData() {
        return acmeSecretData;
    }
}
