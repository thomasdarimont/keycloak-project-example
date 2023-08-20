package com.github.thomasdarimont.keycloak.custom.saml;

import com.google.auto.service.AutoService;
import org.keycloak.Config;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.keycloak.dom.saml.v2.protocol.StatusResponseType;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.saml.preprocessor.SamlAuthenticationPreprocessor;
import org.keycloak.sessions.AuthenticationSessionModel;

/**
 * Example for customizing SAML Requests / Responses
 */
@AutoService(SamlAuthenticationPreprocessor.class)
public class AcmeSamlAuthenticationPreprocessor implements SamlAuthenticationPreprocessor {

    @Override
    public String getId() {
        return "acme-saml-auth-preprocessor";
    }

    @Override
    public AuthnRequestType beforeProcessingLoginRequest(AuthnRequestType authnRequest, AuthenticationSessionModel authSession) {
        return SamlAuthenticationPreprocessor.super.beforeProcessingLoginRequest(authnRequest, authSession);
    }

    @Override
    public LogoutRequestType beforeProcessingLogoutRequest(LogoutRequestType logoutRequest, UserSessionModel authSession, AuthenticatedClientSessionModel clientSession) {
        return SamlAuthenticationPreprocessor.super.beforeProcessingLogoutRequest(logoutRequest, authSession, clientSession);
    }

    @Override
    public AuthnRequestType beforeSendingLoginRequest(AuthnRequestType authnRequest, AuthenticationSessionModel clientSession) {
        return SamlAuthenticationPreprocessor.super.beforeSendingLoginRequest(authnRequest, clientSession);
    }

    @Override
    public LogoutRequestType beforeSendingLogoutRequest(LogoutRequestType logoutRequest, UserSessionModel authSession, AuthenticatedClientSessionModel clientSession) {
        return SamlAuthenticationPreprocessor.super.beforeSendingLogoutRequest(logoutRequest, authSession, clientSession);
    }

    @Override
    public StatusResponseType beforeProcessingLoginResponse(StatusResponseType statusResponse, AuthenticationSessionModel authSession) {
        return SamlAuthenticationPreprocessor.super.beforeProcessingLoginResponse(statusResponse, authSession);
    }

    @Override
    public StatusResponseType beforeSendingResponse(StatusResponseType statusResponse, AuthenticatedClientSessionModel clientSession) {
        return SamlAuthenticationPreprocessor.super.beforeSendingResponse(statusResponse, clientSession);
    }

    @Override
    public SamlAuthenticationPreprocessor create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {
        // NOOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // NOOP
    }

    @Override
    public void close() {
        // NOOP
    }
}
