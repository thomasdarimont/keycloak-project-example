package com.github.thomasdarimont.keycloak.custom.endpoints;

import com.github.thomasdarimont.keycloak.custom.support.KeycloakSessionLookup;
import com.github.thomasdarimont.keycloak.custom.support.ResteasyUtil;
import lombok.RequiredArgsConstructor;
import org.keycloak.authorization.util.Tokens;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.resource.RealmResourceProvider;

import java.util.Optional;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class CustomResourceProvider implements RealmResourceProvider {

    public static final String ID = "custom-resources";

    private static final Pattern ALLOWED_REALM_NAMES_PATTERN = Pattern.compile(Optional.ofNullable(System.getenv("KEYCLOAK_CUSTOM_ENDPOINT_REALM_PATTERN")).orElse("acme-.*"));

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

        RealmModel realm = session.getContext().getRealm();
        if (realm == null) {
            return null;
        }
        boolean allowedRealm = ALLOWED_REALM_NAMES_PATTERN.matcher(realm.getName()).matches();
        if (!allowedRealm) {
            // only expose custom endpoints for allowed realms
            return null;
        }

        CustomResource customResource = new CustomResource(session, accessToken);
        ResteasyUtil.injectProperties(customResource);
        return customResource;
    }


    @Override
    public void close() {
        // NOOP
    }
}
