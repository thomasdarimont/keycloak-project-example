package com.github.thomasdarimont.keycloak.custom.oidc.wellknown;

import com.google.auto.service.AutoService;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCWellKnownProvider;
import org.keycloak.protocol.oidc.OIDCWellKnownProviderFactory;
import org.keycloak.wellknown.WellKnownProvider;
import org.keycloak.wellknown.WellKnownProviderFactory;

/**
 * Custom {@link WellKnownProviderFactory} which reuses the {@link OIDCWellKnownProviderFactory#PROVIDER_ID} to override
 * the default {@link OIDCWellKnownProviderFactory}.
 */
@AutoService(WellKnownProviderFactory.class)
public class AcmeOidcWellKnownProviderFactory extends OIDCWellKnownProviderFactory {

    @Override
    public WellKnownProvider create(KeycloakSession session) {
        return new AcmeOidcWellKnownProvider(session, (OIDCWellKnownProvider) super.create(session));
    }
}
