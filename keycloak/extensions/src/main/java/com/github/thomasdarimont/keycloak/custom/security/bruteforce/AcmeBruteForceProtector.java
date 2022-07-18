package com.github.thomasdarimont.keycloak.custom.security.bruteforce;

import com.github.thomasdarimont.keycloak.custom.account.AccountActivity;
import com.google.auto.service.AutoService;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.services.managers.BruteForceProtectorFactory;
import org.keycloak.services.managers.DefaultBruteForceProtector;
import org.keycloak.services.managers.DefaultBruteForceProtectorFactory;

import static org.keycloak.models.UserModel.DISABLED_REASON;

public class AcmeBruteForceProtector extends DefaultBruteForceProtector {

    public AcmeBruteForceProtector(KeycloakSessionFactory factory) {
        super(factory);
    }

    @Override
    public void failure(KeycloakSession session, LoginEvent event) {
        super.failure(session, event);

        RealmModel realm = getRealmModel(session, event);
        if (realm == null) {
            return;
        }

        if (!realm.isPermanentLockout()) {
            return;
        }

        UserLoginFailureModel userLoginFailure = getUserModel(session, event);
        if (userLoginFailure == null) {
            return;
        }

        UserModel user = session.users().getUserById(realm, userLoginFailure.getUserId());
        if (user == null) {
            return;
        }

        if (user.isEnabled()) {
            return;
        }

        var userIsPermanentlyLockedOut = DISABLED_BY_PERMANENT_LOCKOUT.equals(user.getFirstAttribute(DISABLED_REASON));
        if (userIsPermanentlyLockedOut) {

            KeycloakSession newSession = null;
            try {
                // creating new session here to ensure that the theme configuration is pulled from the correct realm.
                newSession = factory.create();
                newSession.getContext().setRealm(realm);

                AccountActivity.onAccountLockedOut(newSession, realm, user, userLoginFailure);
            } finally {
                if (newSession != null) {
                    newSession.close();
                }
            }
        }
    }

    @AutoService(BruteForceProtectorFactory.class)
    public static class Factory extends DefaultBruteForceProtectorFactory {

        private AcmeBruteForceProtector protector;

        @Override
        public BruteForceProtector create(KeycloakSession session) {
            return protector;
        }

        @Override
        public void postInit(KeycloakSessionFactory factory) {
            protector = new AcmeBruteForceProtector(factory);
            protector.start();
        }

        @Override
        public void close() {
            protector.shutdown();
        }
    }
}
