package com.github.thomasdarimont.keycloak.custom.account;

import com.github.thomasdarimont.keycloak.custom.profile.AcmeUserAttributes;
import com.google.auto.service.AutoService;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.TokenVerifier;
import org.keycloak.authentication.actiontoken.AbstractActionTokenHandler;
import org.keycloak.authentication.actiontoken.ActionTokenContext;
import org.keycloak.authentication.actiontoken.ActionTokenHandlerFactory;
import org.keycloak.authentication.actiontoken.TokenUtils;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;

import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.time.LocalDate;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;

@JBossLog
@SuppressWarnings("rawtypes")
@AutoService(ActionTokenHandlerFactory.class)
public class RequestAccountDeletionActionTokenHandler extends AbstractActionTokenHandler<RequestAccountDeletionActionToken> {

    private static final String ERROR_REQUEST_ACCOUNT_DELETION = "errorAccountDeletion";

    public RequestAccountDeletionActionTokenHandler() {
        super(RequestAccountDeletionActionToken.TOKEN_TYPE, RequestAccountDeletionActionToken.class, ERROR_REQUEST_ACCOUNT_DELETION, EventType.DELETE_ACCOUNT, Errors.NOT_ALLOWED);
    }

    @Override
    public Response handleToken(RequestAccountDeletionActionToken token, ActionTokenContext<RequestAccountDeletionActionToken> tokenContext) {
        var authSession = tokenContext.getAuthenticationSession();

        // deactivate user
        var authenticatedUser = authSession.getAuthenticatedUser();
        authenticatedUser.setEnabled(false);
        authenticatedUser.setSingleAttribute(AcmeUserAttributes.ACCOUNT_DELETION_REQUESTED_AT.getAttributeName(), LocalDate.now().format(ISO_LOCAL_DATE));

        log.infof("Marked user for account deletion. realm=%s userId=%s", authSession.getRealm().getName(), authenticatedUser.getId());

        return Response.temporaryRedirect(URI.create(token.getRedirectUri())).build();
    }

    @Override
    public TokenVerifier.Predicate<? super RequestAccountDeletionActionToken>[] getVerifiers(ActionTokenContext<RequestAccountDeletionActionToken> tokenContext) {
        // TODO add additional checks if necessary
        return TokenUtils.predicates();
    }

    @Override
    public boolean canUseTokenRepeatedly(RequestAccountDeletionActionToken token, ActionTokenContext<RequestAccountDeletionActionToken> tokenContext) {
        return false;
    }

}
