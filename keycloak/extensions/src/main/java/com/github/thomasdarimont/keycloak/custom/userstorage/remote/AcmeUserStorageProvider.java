package com.github.thomasdarimont.keycloak.custom.userstorage.remote;

import com.google.auto.service.AutoService;
import com.github.thomasdarimont.keycloak.custom.userstorage.remote.accountclient.AccountClientOptions;
import com.github.thomasdarimont.keycloak.custom.userstorage.remote.accountclient.AcmeAccountClient;
import com.github.thomasdarimont.keycloak.custom.userstorage.remote.accountclient.AcmeUser;
import com.github.thomasdarimont.keycloak.custom.userstorage.remote.accountclient.SimpleAcmeAccountClient;
import com.github.thomasdarimont.keycloak.custom.userstorage.remote.accountclient.UserSearchInput;
import com.github.thomasdarimont.keycloak.custom.userstorage.remote.accountclient.UserSearchInput.UserSearchOptions;
import com.github.thomasdarimont.keycloak.custom.userstorage.remote.accountclient.VerifyCredentialsInput;
import com.github.thomasdarimont.keycloak.custom.userstorage.remote.accountclient.VerifyCredentialsOutput;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderFactory;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.user.ImportSynchronization;
import org.keycloak.storage.user.SynchronizationResult;
import org.keycloak.storage.user.UserCountMethodsProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;

import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Adhoc User storage that dynamically generates a local user for a lookup to ease load-tests, every password is valid, unless it starts with "invalid".
 * Lookups for usernames that starts with "notfound" will always fail.
 */
@JBossLog
@RequiredArgsConstructor
public class AcmeUserStorageProvider implements //
        UserStorageProvider, // marker interface
        UserLookupProvider,  // lookup by id, username, email
        UserQueryProvider, // find / search for users
        UserRegistrationProvider, // add users
        UserCountMethodsProvider, // count users efficiently
//        CredentialInputValidator, // validate credentials
        ImportSynchronization // perform sync (sync, syncSince)
    // UserAttributeFederatedStorage
{

    public static final String ID = "acme-user-storage";

    public static final String ACCOUNT_SERVICE_URL_CONFIG_PROPERTY = "accountServiceUrl";

    public static final String CONNECT_TIMEOUT_CONFIG_PROPERTY = "connectTimeout";

    public static final String READ_TIMEOUT_CONFIG_PROPERTY = "readTimeout";

    public static final String WRITE_TIMEOUT_CONFIG_PROPERTY = "writeTimeout";

    private final KeycloakSession session;

    private final ComponentModel model;

    private final AcmeAccountClient accountClient;

    @Override
    public void close() {
        // NOOP
    }

    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        if (StorageId.isLocalStorage(id)) {
            return null;
        }

        AcmeUser acmeUser = accountClient.getUserById(StorageId.externalId(id));
        return wrap(realm, acmeUser);
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        AcmeUser acmeUser = accountClient.getUserByUsername(username);
        return wrap(realm, acmeUser);
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        AcmeUser acmeUser = accountClient.getUserByEmail(email);
        return wrap(realm, acmeUser);
    }

    private AcmeUserAdapter wrap(RealmModel realm, AcmeUser acmeUser) {

        if (acmeUser == null) {
            return null;
        }

        AcmeUserAdapter acmeUserAdapter = new AcmeUserAdapter(session, realm, new StorageId(model.getId(), acmeUser.getId()).toString(), acmeUser);
        acmeUserAdapter.setFederationLink(model.getId());

        RoleModel defaultRoles = realm.getDefaultRole();
        acmeUserAdapter.grantRole(defaultRoles);

        return acmeUserAdapter;
    }

//    @Override
    public boolean supportsCredentialType(String credentialType) {
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

//    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return true;
    }

//    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput credentialInput) {

        VerifyCredentialsOutput output = accountClient.verifyCredentials(StorageId.externalId(user.getId()), new VerifyCredentialsInput(credentialInput.getChallengeResponse()));
        return output != null && output.isValid();
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        return null;
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        return true;
    }


    /* UserCountMethodsProvider */
    public int getUsersCount(RealmModel realm, Map<String, String> params) {
        boolean includeServiceAccounts = Boolean.parseBoolean(params.get(UserModel.INCLUDE_SERVICE_ACCOUNT));
        var search = params.get(UserModel.SEARCH);
        var options = EnumSet.noneOf(UserSearchOptions.class);
        options.add(UserSearchOptions.COUNT_ONLY);
        if (includeServiceAccounts) {
            options.add(UserSearchOptions.INCLUDE_SERVICE_ACCOUNTS);
        }
        var userSearchOutput = accountClient.searchForUsers(new UserSearchInput(search, null, null, options));
        if (userSearchOutput == null) {
            return 0;
        }
        return userSearchOutput.getCount();
    }

    /* UserQueryProvider */

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {
        var includeServiceAccounts = Boolean.parseBoolean(params.get(UserModel.INCLUDE_SERVICE_ACCOUNT));
        var options = EnumSet.noneOf(UserSearchOptions.class);
        if (includeServiceAccounts) {
            options.add(UserSearchOptions.INCLUDE_SERVICE_ACCOUNTS);
        }
        var search = params.get(UserModel.SEARCH);
        var userSearchOutput = accountClient.searchForUsers(new UserSearchInput(search, firstResult, maxResults, options));
        if (userSearchOutput == null || userSearchOutput.getUsers().isEmpty()) {
            return Stream.empty();
        }
        return userSearchOutput.getUsers().stream() //
                .filter(acmeUser -> !acmeUser.getUsername().startsWith("service-account-") || includeServiceAccounts) //
                .map(acmeUser -> wrap(realm, acmeUser));
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults) {
        return null;
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
        return null;
    }

    /* ImportSynchronization */

    @Override
    public SynchronizationResult sync(KeycloakSessionFactory sessionFactory, String realmId, UserStorageProviderModel model) {

        log.infof("Run sync");

        return null;
    }

    @Override
    public SynchronizationResult syncSince(Date lastSync, KeycloakSessionFactory sessionFactory, String realmId, UserStorageProviderModel model) {
        return null;
    }

    @AutoService(UserStorageProviderFactory.class)
    public static class Factory implements UserStorageProviderFactory<AcmeUserStorageProvider> {

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public String getHelpText() {
            return "Acme User Storage fetches users from a remote user service";
        }

        @Override
        public AcmeUserStorageProvider create(KeycloakSession session, ComponentModel model) {
            var accountServiceUrl = model.getConfig().getFirst(ACCOUNT_SERVICE_URL_CONFIG_PROPERTY);
            AccountClientOptions options = AccountClientOptions.builder() //
                    .url(accountServiceUrl) //
                    .connectTimeoutMillis(Integer.parseInt(model.getConfig().getFirst(CONNECT_TIMEOUT_CONFIG_PROPERTY))) //
                    .readTimeoutMillis(Integer.parseInt(model.getConfig().getFirst(READ_TIMEOUT_CONFIG_PROPERTY))) //
                    .writeTimeoutMillis(Integer.parseInt(model.getConfig().getFirst(WRITE_TIMEOUT_CONFIG_PROPERTY))) //
                    .build();
            var acmeAccountClient = new SimpleAcmeAccountClient(session, options);
            return new AcmeUserStorageProvider(session, model, acmeAccountClient);
        }

        @Override
        public List<ProviderConfigProperty> getConfigProperties() {
            return ProviderConfigurationBuilder.create() //
                    .property() //
                    .name(ACCOUNT_SERVICE_URL_CONFIG_PROPERTY) //
                    .label("Account Service URL") //
                    .helpText("Account Service URL") //
                    .type(ProviderConfigProperty.STRING_TYPE) //
                    .defaultValue("http://account-service:7070") //
                    .add() //

                    .property() //
                    .name(CONNECT_TIMEOUT_CONFIG_PROPERTY) //
                    .label("Connect Timeout (MS)") //
                    .helpText("Connect Timeout (MS)") //
                    .type(ProviderConfigProperty.STRING_TYPE) //
                    .defaultValue("20000") //
                    .add() //

                    .property() //
                    .name(READ_TIMEOUT_CONFIG_PROPERTY) //
                    .label("Read Timeout (MS)") //
                    .helpText("Read Timeout (MS)") //
                    .type(ProviderConfigProperty.STRING_TYPE) //
                    .defaultValue("20000") //
                    .add() //

                    .property() //
                    .name(WRITE_TIMEOUT_CONFIG_PROPERTY) //
                    .label("Write Timeout (MS)") //
                    .helpText("Write Timeout (MS)") //
                    .type(ProviderConfigProperty.STRING_TYPE) //
                    .defaultValue("20000") //
                    .add() //

                    .build();
        }
    }
}
