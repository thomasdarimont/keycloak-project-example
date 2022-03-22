package com.github.thomasdarimont.keycloak.custom.support;

import org.keycloak.credential.CredentialModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;

import java.util.Optional;

public class CredentialUtils {

    public static Optional<CredentialModel> findFirstOtpCredential(KeycloakSession session, RealmModel realm, UserModel user) {
        return findFirstCredentialOfType(session, realm, user, OTPCredentialModel.TYPE);
    }

    public static Optional<CredentialModel> findFirstCredentialOfType(KeycloakSession session, RealmModel realm, UserModel user, String type) {
        return session.userCredentialManager().getStoredCredentialsByTypeStream(realm, user, type).findFirst();
    }
}
