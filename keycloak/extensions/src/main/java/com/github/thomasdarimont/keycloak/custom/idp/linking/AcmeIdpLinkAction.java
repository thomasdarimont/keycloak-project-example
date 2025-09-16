package com.github.thomasdarimont.keycloak.custom.idp.linking;

import java.util.Set;

import com.google.auto.service.AutoService;
import jakarta.ws.rs.core.Response;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.broker.provider.IdpLinkAction;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.sessions.AuthenticationSessionModel;

/**
 * Custom IdpLinkAction that allows app initiated IdP linking for selected clients.
 */
// @AutoService(RequiredActionFactory.class)
public class AcmeIdpLinkAction extends IdpLinkAction {

    @Override
    public String getDisplayText() {
        return "Acme: Linking Identity Provider";
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        KeycloakSession session = context.getSession();
        RealmModel realm = context.getRealm();
        UserModel user = context.getUser();
        ClientModel client = authSession.getClient();
        EventBuilder event = context.getEvent().clone();
        event.event(EventType.FEDERATED_IDENTITY_LINK);

        String identityProviderAlias = authSession.getClientNote(Constants.KC_ACTION_PARAMETER);
        if (identityProviderAlias == null) {
            event.error(Errors.UNKNOWN_IDENTITY_PROVIDER);
            context.ignore();
            return;
        }
        event.detail(Details.IDENTITY_PROVIDER, identityProviderAlias);
        IdentityProviderModel identityProviderModel = session.identityProviders().getByAlias(identityProviderAlias);
        if (identityProviderModel == null) {
            event.error(Errors.UNKNOWN_IDENTITY_PROVIDER);
            context.ignore();
            return;
        }

        boolean forceAllowAccountLinking = isAllowAccountLinkingForcedFor(realm, client, user, identityProviderModel);
        if (!forceAllowAccountLinking) {
            // Check role
            ClientModel accountService = realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
            RoleModel manageAccountRole = accountService.getRole(AccountRoles.MANAGE_ACCOUNT);
            if (!user.hasRole(manageAccountRole) || !client.hasScope(manageAccountRole)) {
                RoleModel linkRole = accountService.getRole(AccountRoles.MANAGE_ACCOUNT_LINKS);
                if (!user.hasRole(linkRole) || !client.hasScope(linkRole)) {
                    event.error(Errors.NOT_ALLOWED);
                    context.ignore();
                    return;
                }
            }
        }

        String idpDisplayName = KeycloakModelUtils.getIdentityProviderDisplayName(session, identityProviderModel);
        Response challenge = context.form()
                .setAttribute("idpDisplayName", idpDisplayName)
                .createForm("link-idp-action.ftl");
        context.challenge(challenge);
    }

    protected boolean isAllowAccountLinkingForcedFor(RealmModel realm, ClientModel client, UserModel user, IdentityProviderModel targetIdp) {
        // your custom logic here
        return "company-apps".equals(realm.getName()) && Set.of("special-client").contains(client.getClientId());
    }

}