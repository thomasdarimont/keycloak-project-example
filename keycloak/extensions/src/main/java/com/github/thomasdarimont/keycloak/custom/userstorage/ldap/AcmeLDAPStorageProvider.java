package com.github.thomasdarimont.keycloak.custom.userstorage.ldap;

import com.google.auto.service.AutoService;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.UserStorageProviderFactory;
import org.keycloak.storage.ldap.LDAPIdentityStoreRegistry;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.LDAPStorageProviderFactory;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.store.ldap.LDAPIdentityStore;
import org.keycloak.storage.ldap.mappers.LDAPConfigDecorator;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Example for a custom {@link LDAPStorageProvider} which supports storing user attributes locally despite a read-only ldap connection.
 */
public class AcmeLDAPStorageProvider extends LDAPStorageProvider {

    private final Pattern localCustomAttributePattern;

    public AcmeLDAPStorageProvider(LDAPStorageProviderFactory factory, KeycloakSession session, ComponentModel model, LDAPIdentityStore ldapIdentityStore, Pattern localCustomAttributePattern) {
        super(factory, session, model, ldapIdentityStore);
        this.localCustomAttributePattern = localCustomAttributePattern;
    }

    @Override
    protected UserModel proxy(RealmModel realm, UserModel local, LDAPObject ldapObject, boolean newUser) {
        UserModel proxy = super.proxy(realm, local, ldapObject, newUser);
        return new AcmeReadonlyLDAPUserModelDelegate(proxy, localCustomAttributePattern);
    }

    @JBossLog
    @AutoService(UserStorageProviderFactory.class)
    public static class Factory extends LDAPStorageProviderFactory {

        private LDAPIdentityStoreRegistry ldapStoreRegistry;

        private Pattern localCustomAttributePattern;

        @Override
        public void init(Config.Scope config) {
            this.ldapStoreRegistry = new LDAPIdentityStoreRegistry();
            String localCustomAttributePatternString = config.get("localCustomAttributePattern", "(custom-.*|foo)");
            log.infof("Using local custom attribute pattern: %s", localCustomAttributePatternString);
            this.localCustomAttributePattern = Pattern.compile(localCustomAttributePatternString);
        }

        @Override
        public LDAPStorageProvider create(KeycloakSession session, ComponentModel model) {
            Map<ComponentModel, LDAPConfigDecorator> configDecorators = getLDAPConfigDecorators(session, model);

            LDAPIdentityStore ldapIdentityStore = this.ldapStoreRegistry.getLdapStore(session, model, configDecorators);
            return new AcmeLDAPStorageProvider(this, session, model, ldapIdentityStore, localCustomAttributePattern);
        }

    }
}
