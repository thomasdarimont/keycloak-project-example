package com.github.thomasdarimont.keycloak.custom.registration.actiontokens;

import com.google.auto.service.AutoService;
import org.keycloak.authentication.actiontoken.ActionTokenContext;
import org.keycloak.authentication.actiontoken.ActionTokenHandlerFactory;
import org.keycloak.authentication.actiontoken.execactions.ExecuteActionsActionToken;
import org.keycloak.authentication.actiontoken.execactions.ExecuteActionsActionTokenHandler;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.sessions.AuthenticationSessionModel;

/**
 * Example for changing Keycloaks standard behavior to remain logged in after action token handler execution.
 */
@AutoService(ActionTokenHandlerFactory.class)
public class AcmeExecuteActionsActionTokenHandler extends ExecuteActionsActionTokenHandler {

    @Override
    public AuthenticationSessionModel startFreshAuthenticationSession(ExecuteActionsActionToken token, ActionTokenContext<ExecuteActionsActionToken> tokenContext) {
        AuthenticationSessionModel authSession = super.startFreshAuthenticationSession(token, tokenContext);

        boolean remainSingedIn = true; // set to true to remain signed in after auth
        boolean remainSignedInAfterExecuteActions = tokenContext.getRealm().getAttribute("acme.remainSignedInAfterExecuteActions", remainSingedIn);

        if (remainSignedInAfterExecuteActions) {
            authSession.removeAuthNote(AuthenticationManager.END_AFTER_REQUIRED_ACTIONS);
        }

        return authSession;
    }
}
