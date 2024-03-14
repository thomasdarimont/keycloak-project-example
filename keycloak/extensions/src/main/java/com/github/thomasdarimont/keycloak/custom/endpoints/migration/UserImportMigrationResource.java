package com.github.thomasdarimont.keycloak.custom.endpoints.migration;

import com.github.thomasdarimont.keycloak.custom.userstorage.remote.AcmeUserStorageProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.representations.AccessToken;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.adapter.InMemoryUserAdapter;
import org.keycloak.storage.federated.UserFederatedStorageProvider;

import java.util.List;
import java.util.Map;

@JBossLog
@RequiredArgsConstructor
public class UserImportMigrationResource {

    private final KeycloakSession session;

    private final AccessToken token;

    /**
     * curl -k -v -H "Content-type: application/json" -d '{"batchSize":10000}' https://id.acme.test:8443/auth/realms/acme-user-migration/custom-resources/migration/users
     *
     * @param request
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response migrateUsers(Request request, MigrationRequest migrationRequest) {

        log.infof("Migrate users...");

        // UserStorageProvider storageProvider = session.getProvider(AcmeUserStorageProvider.class, AcmeUserStorageProvider.ID);
        KeycloakContext context = session.getContext();
        UserProvider userProvider = session.users();
        RealmModel realm = context.getRealm();

        int batchSize = migrationRequest.batchSize();
        ComponentModel customStorageProviderComponent = realm.getComponentsStream().filter(c -> AcmeUserStorageProvider.ID.equals(c.getProviderId())).toList().get(0);
        AcmeUserStorageProvider acmeStorageProvider = (AcmeUserStorageProvider) session.getProvider(UserStorageProvider.class, customStorageProviderComponent);

//        List<UserModel> federatedUsers = userProvider.searchForUserStream(realm, "*") //
//                .filter(u -> !StorageId.isLocalStorage(u.getId()) && "bugs".equals(u.getUsername())).toList();

        // generate batch partitions

        // for batch partition (startIndex, pageSize)

        // check current tx / create new tx

        List<UserModel> federatedUsers = acmeStorageProvider.searchForUserStream(realm, Map.of(UserModel.SEARCH, "*"), 0, Integer.MAX_VALUE) //
                .filter(u -> u.getFirstAttribute("migrated") == null && "bugs".equals(u.getUsername())) //
                .toList();

        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        Query migrateOfflineSesions = em.createNativeQuery("""
                update offline_user_session ous
                set user_id = :localUserId
                where ous.realm_id = :realmId
                  and ous.user_id = :federatedId
                """);

        for (var federatedUser : federatedUsers) {
            String fedUserId = federatedUser.getId();
            String externalUserId = StorageId.externalId(fedUserId);
            UserModel localUser = userProvider.addUser(realm, externalUserId, federatedUser.getUsername(), true, true);
            localUser.setEnabled(federatedUser.isEnabled());
            localUser.setFirstName(federatedUser.getFirstName());
            localUser.setLastName(federatedUser.getLastName());
            localUser.setEmail(federatedUser.getEmail());
            localUser.setEmailVerified(federatedUser.isEmailVerified());
            localUser.setCreatedTimestamp(federatedUser.getCreatedTimestamp());
            for (var attr : federatedUser.getAttributes().entrySet()) {
                localUser.setAttribute(attr.getKey(), attr.getValue());
            }
            localUser.setSingleAttribute("acmeLegacyId", fedUserId);

            UserFederatedStorageProvider jpaUserFederatedStorageProvider = session.getProvider(UserFederatedStorageProvider.class, "jpa");

            List<String> requiredActions = jpaUserFederatedStorageProvider.getRequiredActionsStream(realm, fedUserId).toList();
            requiredActions.forEach(localUser::addRequiredAction);

            List<CredentialModel> federatedCreds = jpaUserFederatedStorageProvider.getStoredCredentialsStream(realm, fedUserId).toList();
            //federatedUser.credentialManager().getStoredCredentialsStream()
            federatedCreds.forEach(cred -> {
                cred.setId(null);
                localUser.credentialManager().createStoredCredential(cred);
            });

            List<FederatedIdentityModel> federatedIdentities = jpaUserFederatedStorageProvider.getFederatedIdentitiesStream(fedUserId, realm).toList();
//            List<FederatedIdentityModel> federatedIdentities = userProvider.getFederatedIdentitiesStream(realm, federatedUser).toList();
            for (FederatedIdentityModel fedId : federatedIdentities) {
                userProvider.addFederatedIdentity(realm, localUser, fedId);
            }

            // TODO verify migrate offline user session from federated user to new local user
            int updateCount = -1;

            // NOTE : This only moves the offline session on database level-> the old offline session is still in memory
            // Server / Cluster restart is needed in order to propagate new offline session information
            migrateOfflineSesions.setParameter("localUserId", localUser.getId());
            migrateOfflineSesions.setParameter("realmId", realm.getId());
            migrateOfflineSesions.setParameter("federatedId", fedUserId);
            updateCount = migrateOfflineSesions.executeUpdate();


            log.infof("Imported local user. Old id=%s, username=%s. New id=%s, username=%s. Migrated offline sessions: %d", fedUserId, federatedUser.getUsername(), localUser.getId(), localUser.getUsername(), updateCount);

//            federatedUser.setSingleAttribute("migrated", "true");

            // Note: dangling federated indentity links for federated user are cleared after restart

            jpaUserFederatedStorageProvider.preRemove(realm, new InMemoryUserAdapter(session, realm, fedUserId));

        }

        // commit tx

        // end
        return Response.ok(Map.of("foo", "bar")).build();
    }

    /**
     * curl -k -v -H "Content-type: application/json" -X DELETE -d '{}' https://id.acme.test:8443/auth/realms/acme-user-migration/custom-resources/migration/users/cache
     *
     * @param request
     * @return
     */
    @Path("/cache")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    public Response clearCache(Request request) {

        log.infof("Clearing offline session cache");
        session.getProvider(InfinispanConnectionProvider.class) //
                .getCache(InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME) //
                .clear();

        return Response.noContent().build();
    }

    public record MigrationRequest(int batchSize) {
    }
}
