package com.github.thomasdarimont.keycloak.custom.support;

import org.keycloak.credential.CredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;

import java.util.Optional;

public class CredentialUtils {

    public static Optional<CredentialModel> findFirstOtpCredential(UserModel user) {
        return findFirstCredentialOfType(user, OTPCredentialModel.TYPE);
    }

    public static Optional<CredentialModel> findFirstCredentialOfType(UserModel user, String type) {
        return user.credentialManager().getStoredCredentialsByTypeStream(type).findFirst();
    }
}
