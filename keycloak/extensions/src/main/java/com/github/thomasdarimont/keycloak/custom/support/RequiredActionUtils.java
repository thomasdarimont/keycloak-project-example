package com.github.thomasdarimont.keycloak.custom.support;

import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.http.HttpRequest;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.core.MultivaluedMap;
import java.util.function.Consumer;

public class RequiredActionUtils {

    public static boolean isCancelApplicationInitiatedAction(RequiredActionContext context) {

        HttpRequest httpRequest = context.getHttpRequest();
        MultivaluedMap<String, String> formParams = httpRequest.getDecodedFormParameters();
        return formParams.containsKey(LoginActionsService.CANCEL_AIA);
    }

    public static void cancelApplicationInitiatedAction(RequiredActionContext context, String actionProviderId, Consumer<AuthenticationSessionModel> cleanup) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        AuthenticationManager.setKcActionStatus(actionProviderId, RequiredActionContext.KcActionStatus.CANCELLED, authSession);
        cleanup.accept(authSession);
        context.success();
    }
}
