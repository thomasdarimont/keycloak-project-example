package com.github.thomasdarimont.keycloak.custom.account.console;

import com.google.auto.service.AutoService;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resource.AccountResourceProvider;
import org.keycloak.services.resource.AccountResourceProviderFactory;
import org.keycloak.services.resources.account.AccountConsole;
import org.keycloak.services.resources.account.AccountConsoleFactory;
import org.keycloak.theme.FreeMarkerException;
import org.keycloak.theme.Theme;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Workaround for https://github.com/keycloak/keycloak/issues/40463
 */
@JBossLog
// @AutoService(AccountResourceProviderFactory.class)
public class AcmeAccountConsoleFactory extends AccountConsoleFactory {

    private static final MethodHandle renderAccountConsoleMH;

    static {
        MethodHandle mh = null;
        try {
            MethodHandles.Lookup privateLookup = MethodHandles.privateLookupIn(AccountConsole.class, MethodHandles.lookup());
            mh = privateLookup.findVirtual(AccountConsole.class, "renderAccountConsole", MethodType.methodType(Response.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            log.warn("Could not reflect renderAccountConsole method", e);
        }
        renderAccountConsoleMH = mh;
    }

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
        return new AccountConsole(session, client, theme) {
            @Override
            public Response getMainPage(String path) throws IOException, FreeMarkerException {
                if (renderAccountConsoleMH != null) {
                    try {
                        return (Response) renderAccountConsoleMH.invoke(this);
                    } catch (Throwable ignored) {
                        // fallback to original method
                    }
                }
                return super.getMainPage(path);
            }
        };
    }

    private Theme getTheme(KeycloakSession session) {
        try {
            return session.theme().getTheme(Theme.Type.ACCOUNT);
        } catch (IOException e) {
            throw new InternalServerErrorException(e);
        }
    }

    private ClientModel getAccountManagementClient(RealmModel realm) {
        ClientModel client = realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
        if (client == null || !client.isEnabled()) {
            throw new NotFoundException("account management not enabled");
        }
        return client;
    }
}
