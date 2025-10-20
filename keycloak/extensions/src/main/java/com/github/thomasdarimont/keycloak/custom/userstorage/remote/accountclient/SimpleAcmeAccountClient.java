package com.github.thomasdarimont.keycloak.custom.userstorage.remote.accountclient;

import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.apache.http.client.config.RequestConfig;
import org.keycloak.http.simple.SimpleHttp;
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
        var http = createHttpClient(session);
        var request = http.doPost(options.getUrl() + "/api/users/lookup/username");
        request.json(Map.of("username", username));
        try (var response = request.asResponse()) {
            AcmeUser user = response.asJson(AcmeUser.class);
            return user;
        } catch (Exception e) {
            log.warn("Failed to parse user response", e);
            return null;
        }
    }

    protected SimpleHttp createHttpClient(KeycloakSession session) {
        var http = SimpleHttp.create(session);
        var requestConfig = RequestConfig.custom() //
                .setConnectTimeout(options.getConnectTimeoutMillis()) //
                .setConnectionRequestTimeout(options.getReadTimeoutMillis()) //
                .setSocketTimeout(options.getWriteTimeoutMillis())
                .build();
        http.withRequestConfig(requestConfig);
        return http;
    }

    @Override
    public AcmeUser getUserByEmail(String email) {
        var http = createHttpClient(session);
        var request = http.doPost(options.getUrl() + "/api/users/lookup/email");
        request.json(Map.of("email", email));
        try (var response = request.asResponse()) {
            return response.asJson(AcmeUser.class);
        } catch (IOException e) {
            log.warn("Failed to parse user response", e);
            return null;
        }
    }

    @Override
    public AcmeUser getUserById(String userId) {
        var http = createHttpClient(session);
        var request = http.doGet(options.getUrl() + "/api/users/" + userId);
        try (var response = request.asResponse()) {
            return response.asJson(AcmeUser.class);
        } catch (IOException e) {
            log.warn("Failed to parse user response", e);
            return null;
        }
    }

    @Override
    public VerifyCredentialsOutput verifyCredentials(String userId, VerifyCredentialsInput input) {
        var http = createHttpClient(session);
        var request = http.doPost(options.getUrl() + "/api/users/" + userId + "/credentials/verify");
        request.json(input);
        try (var response = request.asResponse()) {
            return response.asJson(VerifyCredentialsOutput.class);
        } catch (IOException e) {
            log.warn("Failed to parse user response", e);
            return null;
        }
    }

    @Override
    public UserSearchOutput searchForUsers(UserSearchInput userSearchInput) {
        var http = createHttpClient(session);
        var request = http.doPost(options.getUrl() + "/api/users/search");
        request.json(userSearchInput);
        try (var response = request.asResponse()) {
            return response.asJson(UserSearchOutput.class);
        } catch (IOException e) {
            log.warn("Failed to parse user response", e);
            return null;
        }
    }
}
