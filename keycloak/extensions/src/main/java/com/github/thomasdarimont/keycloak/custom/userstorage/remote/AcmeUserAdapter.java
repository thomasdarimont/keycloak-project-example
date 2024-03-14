package com.github.thomasdarimont.keycloak.custom.userstorage.remote;

import com.github.thomasdarimont.keycloak.custom.userstorage.remote.accountclient.AcmeUser;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.UserStorageUtil;
import org.keycloak.storage.adapter.InMemoryUserAdapter;
import org.keycloak.storage.federated.UserFederatedStorageProvider;

import java.util.stream.Stream;

public class AcmeUserAdapter extends InMemoryUserAdapter {

    public AcmeUserAdapter(KeycloakSession session, RealmModel realm, String id, AcmeUser acmeUser) {
        super(session, realm, id);
        setUsername(acmeUser.getUsername());
        setFirstName(acmeUser.getFirstname());
        setLastName(acmeUser.getLastname());
        setEnabled(acmeUser.isEnabled());
        setEmail(acmeUser.getEmail());
        setEmailVerified(acmeUser.isEmailVerified());
    }

    public UserFederatedStorageProvider getFederatedStorage() {
        return UserStorageUtil.userFederatedStorage(session);
    }

    @Override
    public void addRequiredAction(String action) {
        checkReadonly();
        getFederatedStorage().addRequiredAction(realm, getId(), action);
    }

    @Override
    public void addRequiredAction(RequiredAction action) {
        addRequiredAction(action.name());
    }

    @Override
    public void removeRequiredAction(String action) {
        checkReadonly();
        getFederatedStorage().removeRequiredAction(realm, getId(), action);
    }

    @Override
    public void removeRequiredAction(RequiredAction action) {
        removeRequiredAction(action.name());
    }

    @Override
    public Stream<String> getRequiredActionsStream() {
        return getFederatedStorage().getRequiredActionsStream(realm, getId());
    }
}
