package com.github.thomasdarimont.keycloak.custom.account.console;

import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resource.AccountResourceProvider;
import org.keycloak.services.resources.account.AccountConsole;
import org.keycloak.services.resources.account.AccountConsoleFactory;
import org.keycloak.theme.Theme;

/**
 * Workaround for https://github.com/keycloak/keycloak/issues/40463
 */
@JBossLog
// @AutoService(AccountResourceProviderFactory.class)
public class AcmeAccountConsoleFactory extends AccountConsoleFactory {

    @Override
    public void init(Config.Scope config) {
        super.init(config);
        log.info("Initializing AcmeAccountConsoleFactory");
    }

    @Override
    public AccountResourceProvider create(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        ClientModel client = getAccountManagementClient(realm);
        Theme theme = getTheme(session);
        return new AccountConsole(session, client, theme);
    }
}
