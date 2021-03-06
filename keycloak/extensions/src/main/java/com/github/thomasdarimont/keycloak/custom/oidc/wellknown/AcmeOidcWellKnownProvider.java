package com.github.thomasdarimont.keycloak.custom.oidc.wellknown;

import com.google.auto.service.AutoService;
import org.keycloak.OAuth2Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCWellKnownProvider;
import org.keycloak.protocol.oidc.OIDCWellKnownProviderFactory;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.wellknown.WellKnownProvider;
import org.keycloak.wellknown.WellKnownProviderFactory;

import java.util.ArrayList;

/**
 * Custom OpenID {@link WellKnownProvider} which can remove unwanted OpenID configuration information.
 */
public class AcmeOidcWellKnownProvider implements WellKnownProvider {

    private final KeycloakSession session;
    private final OIDCWellKnownProvider delegate;

    public AcmeOidcWellKnownProvider(KeycloakSession session, OIDCWellKnownProvider delegate) {
        this.session = session;
        this.delegate = delegate;
    }

    @Override
    public Object getConfig() {
        OIDCConfigurationRepresentation config = (OIDCConfigurationRepresentation) delegate.getConfig();

        var grantTypesSupported = new ArrayList<>(config.getGrantTypesSupported());
        config.setGrantTypesSupported(grantTypesSupported);

//        // remove device-flow metadata
//        grantTypesSupported.remove(OAuth2Constants.DEVICE_CODE_GRANT_TYPE);
//        config.setDeviceAuthorizationEndpoint(null);

//        // remove ciba metadata
//        grantTypesSupported.remove(OAuth2Constants.CIBA_GRANT_TYPE);
//        config.setMtlsEndpointAliases(null);
//        config.setBackchannelAuthenticationEndpoint(null);
//        config.setBackchannelAuthenticationRequestSigningAlgValuesSupported(null);
//        config.setBackchannelTokenDeliveryModesSupported(null);

//        // remove dynamic client registration endpoint
//        config.setRegistrationEndpoint(null);

//        // Add custom claim
//        var claimsSupported = new ArrayList<>(config.getClaimsSupported());
//        claimsSupported.add("customClaim");
//        config.setClaimsSupported(claimsSupported);

        return config;
    }

    @Override
    public void close() {
        // NOOP
    }

    /**
     * Custom {@link WellKnownProviderFactory} which reuses the {@link OIDCWellKnownProviderFactory#PROVIDER_ID} to override
     * the default {@link OIDCWellKnownProviderFactory}.
     */
    @AutoService(WellKnownProviderFactory.class)
    public static class Factory extends OIDCWellKnownProviderFactory {

        @Override
        public WellKnownProvider create(KeycloakSession session) {
            return new AcmeOidcWellKnownProvider(session, (OIDCWellKnownProvider) super.create(session));
        }
    }
}
