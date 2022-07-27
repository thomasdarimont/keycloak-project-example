package com.github.thomasdarimont.keycloak.custom.consent;

import com.github.thomasdarimont.keycloak.custom.support.TokenUtils;
import lombok.Data;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@JBossLog
public class ProfileClient {

    public static ConsentFormProfileDataResponse getProfileAttributesForConsentForm(KeycloakSession session, ClientModel client, Set<String> scopeNames, UserModel user) {

        // TODO create confidential client with service-accounts enabled
        var accessToken = TokenUtils.generateServiceAccountAccessToken(session, "app-demo-service", "", null);

        // TODO externalize URL
        var url = String.format("https://apps.acme.test:4653/api/consentForm/%s?clientId=%s&scope=%s", //
                user.getId(), client.getClientId(), String.join("+", scopeNames));

        var http = SimpleHttp.doGet(url, session).auth(accessToken).socketTimeOutMillis(60 * 1000);

        try {
            var response = http.asResponse();
            var body = toResponse(response, ConsentFormProfileDataResponse.class);
            return body;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> T toResponse(SimpleHttp.Response response, Class<T> type) {

        T result = null;
        if (response == null) {
            return result;
        }

        try {
            return response.asJson(type);
        } catch (IOException e) {
            log.errorf(e, "Failed to parse profile response");
        } finally {
            try {
                response.close();
            } catch (IOException e) {
                log.errorf(e, "Failed to cleanup response");
            }
        }

        return result;
    }

    public static ConsentFormUpdateProfileResult updateProfileAttributesFromConsentForm(
            KeycloakSession session, ClientModel client, Set<String> scopeNames, UserModel user, Map<String, String> profileUpdate) {

        // TODO create confidential client with service-accounts enabled
        var accessToken = TokenUtils.generateServiceAccountAccessToken(session, "app-demo-service", "", null);

        // TODO externalize URL
        var url = String.format("https://apps.acme.test:4653/api/consentForm/%s?clientId=%s&scope=%s", //
                user.getId(), client.getClientId(), String.join("+", scopeNames));

        if (profileUpdate.containsKey("email") && profileUpdate.get("email") != null) {
            profileUpdate.put("email_verified", String.valueOf(user.isEmailVerified()));
        }

        var http = SimpleHttp.doPost(url, session).auth(accessToken).socketTimeOutMillis(60 * 1000) //
                .json(profileUpdate);

        try {
            var response = http.asResponse();
            var body = toResponse(response, ConsentFormUpdateProfileResult.class);
            return body;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Data
    public static class ConsentFormProfileDataResponse {

        private Map<String, List<ProfileAttribute>> mapping;
    }

    @Data
    public static class ConsentFormUpdateProfileResult {

        private List<ProfileAttributeError> errors;
    }

    @Data
    public static class ProfileAttributeError {

        private String type;

        private String attributeName;
    }
}
