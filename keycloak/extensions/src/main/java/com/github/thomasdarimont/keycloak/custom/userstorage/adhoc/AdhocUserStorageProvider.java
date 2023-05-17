package com.github.thomasdarimont.keycloak.custom.userstorage.adhoc;

import com.google.auto.service.AutoService;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderFactory;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.user.ImportSynchronization;
import org.keycloak.storage.user.SynchronizationResult;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserRegistrationProvider;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Adhoc User storage that dynamically generates a local user for a lookup to ease load-tests, every password is valid, unless it starts with "invalid".
 * Lookups for usernames that starts with "notfound" will always fail.
 */
public class AdhocUserStorageProvider implements UserStorageProvider, //
        UserLookupProvider,  //
        UserRegistrationProvider, //
        CredentialInputValidator, //
        ImportSynchronization //
{

    public static final String ID = "adhoc";

    private final KeycloakSession session;

    private final ComponentModel model;

    public AdhocUserStorageProvider(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.model = model;
    }

    @Override
    public void close() {
        // NOOP
    }

    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        var jpaUserProvider = session.getProvider(UserProvider.class);
        return jpaUserProvider.getUserById(realm, id);
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {

        if (username.startsWith("notfound")) {
            return null;
        }

        var jpaUserProvider = session.getProvider(UserProvider.class);
        var jpaUser = jpaUserProvider.getUserByUsername(realm, username);
        if (jpaUser != null) {
            return jpaUser;
        }

        var userId = UUID.nameUUIDFromBytes(username.getBytes(StandardCharsets.UTF_8)).toString();
        var email = username + "@acme.test";

        jpaUser = jpaUserProvider.addUser(realm, userId, username, true, false);
        jpaUser.setEmail(email);
        jpaUser.setFirstName("First " + username);
        jpaUser.setLastName("Last " + username);
        jpaUser.setEnabled(true);
        jpaUser.setFederationLink(model.getId());

        return jpaUser;
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        return getUserByUsername(realm, email);
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return true;
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput credentialInput) {
        // accept all password for load test, except if the password starts with "invalid", then always reject the password.

        String challengeResponse = credentialInput.getChallengeResponse();
        return challengeResponse == null || !challengeResponse.startsWith("invalid");
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        return null;
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public SynchronizationResult sync(KeycloakSessionFactory sessionFactory, String realmId, UserStorageProviderModel model) {
        return null;
    }

    @Override
    public SynchronizationResult syncSince(Date lastSync, KeycloakSessionFactory sessionFactory, String realmId, UserStorageProviderModel model) {
        return null;
    }

    @AutoService(UserStorageProviderFactory.class)
    public static class Factory implements UserStorageProviderFactory<AdhocUserStorageProvider> {

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public String getHelpText() {
            return "Generates requested users on the fly. Useful for load-testing. Username lookup will fail for username and emails beginning with 'notfound'. All provided passwords will be considered valid, unless they begin with 'invalid'.";
        }

        @Override
        public AdhocUserStorageProvider create(KeycloakSession session, ComponentModel model) {
            return new AdhocUserStorageProvider(session, model);
        }

        @Override
        public List<ProviderConfigProperty> getConfigProperties() {
            return UserStorageProviderFactory.super.getConfigProperties();
        }
    }
}
