package com.github.thomasdarimont.keycloak.custom.endpoints.filter;

import com.github.thomasdarimont.keycloak.custom.support.KeycloakSessionLookup;
import jakarta.annotation.Priority;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.representations.AccessToken;

import java.io.IOException;
import java.util.regex.Pattern;

import static com.github.thomasdarimont.keycloak.custom.endpoints.filter.TokenFilter.ACCESS_TOKEN_SESSION_KEY;

@AzpFilterBinding
@JBossLog
@Priority(1)
@Provider
public class AzpFilter implements ContainerRequestFilter {

    private final Pattern clientPattern = Pattern.compile("acme.*");

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        AccessToken accessToken = KeycloakSessionLookup.currentSession().getAttribute(ACCESS_TOKEN_SESSION_KEY, AccessToken.class);

        if (accessToken == null) {
            log.error("AzpFilter must run after TokenFilter");
            throw new InternalServerErrorException();
        }

        if (!clientPattern.matcher(accessToken.getIssuedFor()).matches()) {
            throw new ForbiddenException("This resource is only accessible for acme clients");
        }
    }
}
