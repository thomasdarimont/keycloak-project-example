package com.github.thomasdarimont.keycloak.custom.account;

import com.github.thomasdarimont.keycloak.custom.themes.login.AcmeUrlBean;
import org.keycloak.common.util.Time;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.resources.LoginActionsService;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

public class AccountDeletion {

    public static URI createActionToken(KeycloakSession session, RealmModel realm, UserModel user, UriInfo uriInfo) {
        String userId = user.getId();
        int validityInSecs = realm.getActionTokenGeneratedByAdminLifespan();
        int absoluteExpirationInSecs = Time.currentTime() + validityInSecs;
        RequestAccountDeletionActionToken requestAccountDeletionActionToken = new RequestAccountDeletionActionToken(userId, absoluteExpirationInSecs, Constants.ACCOUNT_MANAGEMENT_CLIENT_ID, new AcmeUrlBean(session).getAccountDeletedUrl());
        String token = requestAccountDeletionActionToken.serialize(session, realm, uriInfo);
        UriBuilder builder = LoginActionsService.actionTokenProcessor(session.getContext().getUri());
        builder.queryParam("key", token);
        String actionTokenLink = builder.build(realm.getName()).toString();
        return URI.create(actionTokenLink);
    }
}
