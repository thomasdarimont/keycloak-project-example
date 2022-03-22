package com.github.thomasdarimont.keycloak.custom.endpoints;

import com.github.thomasdarimont.keycloak.custom.config.RealmConfig;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.utils.WebOriginsUtils;
import org.keycloak.services.resources.Cors;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Set;

public class CorsUtils {

    private static final String FALLBACK_CLIENT_ID = "app-minispa";

    public static Cors addCorsHeaders(KeycloakSession session, //
                                      HttpRequest request, //
                                      Response.ResponseBuilder responseBuilder, //
                                      Set<String> allowedHttpMethods, //
                                      String clientId //
    ) {

        var client = resolveClient(session, clientId);
        var requestOrigin = URI.create(request.getHttpHeaders().getHeaderString("origin")).toString();
        var allowedOrigins = WebOriginsUtils.resolveValidWebOrigins(session, client);

        var cors = Cors.add(request, responseBuilder);
        if (allowedOrigins.contains(requestOrigin)) {
            cors.allowedOrigins(requestOrigin); //
        }

        var methods = allowedHttpMethods.toArray(new String[0]);
        return cors.auth().allowedMethods(methods).preflight();
    }

    private static ClientModel resolveClient(KeycloakSession session, String clientId) {

        // TODO only allow custom clients here
        var realm = session.getContext().getRealm();
        String clientIdToUse;
        if (clientId != null) {
            clientIdToUse = clientId;
        } else {
            clientIdToUse = new RealmConfig(realm).getString("customAccountEndpointsClient", FALLBACK_CLIENT_ID);
        }
        return realm.getClientByClientId(clientIdToUse);
    }
}
