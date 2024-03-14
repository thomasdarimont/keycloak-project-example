package com.github.thomasdarimont.keycloak.custom.userstorage.remote.accountclient;

import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.models.KeycloakSession;

import java.io.IOException;
import java.util.Map;

@JBossLog
@RequiredArgsConstructor
public class SimpleAcmeAccountClient implements AcmeAccountClient {

    private final KeycloakSession session;

    private final AccountClientOptions options;

    @Override
    public AcmeUser getUserByUsername(String username) {
        SimpleHttp http = SimpleHttp.doPost(options.getUrl() + "/api/users/lookup/username", session);
        configureHttpClient(http);
        http.json(Map.of("username", username));
        try (SimpleHttp.Response response = http.asResponse()) {
            AcmeUser user = response.asJson(AcmeUser.class);
            return user;
        } catch (Exception e) {
            log.warn("Failed to parse user response", e);
            return null;
        }
    }

    private void configureHttpClient(SimpleHttp http) {
        http.connectTimeoutMillis(options.getConnectTimeoutMillis());
        http.connectionRequestTimeoutMillis(options.getReadTimeoutMillis());
        http.socketTimeOutMillis(options.getWriteTimeoutMillis());
    }

    @Override
    public AcmeUser getUserByEmail(String email) {
        SimpleHttp http = SimpleHttp.doPost(options.getUrl() + "/api/users/lookup/email", session);
        configureHttpClient(http);
        http.json(Map.of("email", email));
        try (SimpleHttp.Response response = http.asResponse()) {
            return response.asJson(AcmeUser.class);
        } catch (IOException e) {
            log.warn("Failed to parse user response", e);
            return null;
        }
    }

    @Override
    public AcmeUser getUserById(String userId) {

        SimpleHttp http = SimpleHttp.doGet(options.getUrl() + "/api/users/" + userId, session);
        configureHttpClient(http);
        try (SimpleHttp.Response response = http.asResponse()) {
            return response.asJson(AcmeUser.class);
        } catch (IOException e) {
            log.warn("Failed to parse user response", e);
            return null;
        }
    }

    @Override
    public VerifyCredentialsOutput verifyCredentials(String userId, VerifyCredentialsInput input) {
        SimpleHttp http = SimpleHttp.doPost(options.getUrl() + "/api/users/" + userId + "/credentials/verify", session);
        configureHttpClient(http);
        http.json(input);
        try (SimpleHttp.Response response = http.asResponse()) {
            return response.asJson(VerifyCredentialsOutput.class);
        } catch (IOException e) {
            log.warn("Failed to parse user response", e);
            return null;
        }
    }

    @Override
    public UserSearchOutput searchForUsers(UserSearchInput userSearchInput) {
        SimpleHttp http = SimpleHttp.doPost(options.getUrl() + "/api/users/search", session);
        configureHttpClient(http);
        http.json(userSearchInput);
        try (SimpleHttp.Response response = http.asResponse()) {
            return response.asJson(UserSearchOutput.class);
        } catch (IOException e) {
            log.warn("Failed to parse user response", e);
            return null;
        }
    }
}
