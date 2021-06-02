package com.github.thomasdarimont.keycloak.custom.endpoints;

import com.github.thomasdarimont.keycloak.custom.support.KeycloakSessionLookup;
import lombok.RequiredArgsConstructor;
import org.keycloak.authorization.util.Tokens;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.resource.RealmResourceProvider;

@RequiredArgsConstructor
public class CustomResourceProvider implements RealmResourceProvider {

    public static final String ID = "custom-resources";

    @Override
    public Object getResource() {

        KeycloakSession session = KeycloakSessionLookup.currentSession();

        AccessToken accessToken = Tokens.getAccessToken(session);

        // check access
//        if (accessToken == null) {
//            throw new NotAuthorizedException("Invalid Token", Response.status(UNAUTHORIZED).build());
//        } else if (!ScopeUtils.hasScope("custom.api", accessToken.getScope())) {
//            throw new ForbiddenException("No Access", Response.status(FORBIDDEN).build());
//        }

        return new CustomResource(session, accessToken);
    }



    @Override
    public void close() {
        // NOOP
    }
}
