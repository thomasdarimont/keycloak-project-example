package com.github.thomasdarimont.keycloak.custom.consent;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.thomasdarimont.keycloak.custom.config.RealmConfig;
import com.github.thomasdarimont.keycloak.custom.support.TokenUtils;
import lombok.Data;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@JBossLog
public class ProfileClient {

    public static ConsentFormProfileDataResponse getProfileAttributesForConsentForm(KeycloakSession session, RealmModel realm, ClientModel client, //
                                                                                    Set<String> scopeNames, UserModel user) {

        var url = getConsentFormUrl(realm, client, scopeNames, user);
        var accessToken = getServiceAccountAccessToken(session, realm);
        var http = SimpleHttp.doGet(url, session).auth(accessToken).socketTimeOutMillis(60 * 1000);

        try {
            var response = http.asResponse();

            if (response.getStatus() >= 400 && response.getStatus() <= 599) {
                log.warnf("external profile fetch failed. realm=%s userId=%s status=%s", //
                        realm.getName(), user.getId(), response.getStatus());
            }
            return toResponse(response, ConsentFormProfileDataResponse.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ConsentFormUpdateProfileResult updateProfileAttributesFromConsentForm(KeycloakSession session, RealmModel realm, ClientModel client, //
                                                                                        Set<String> scopeNames, UserModel user, Map<String, String> profileUpdate) {

        var url = getConsentFormUrl(realm, client, scopeNames, user);
        var accessToken = getServiceAccountAccessToken(session, realm);

        // special handling for email address verified state, since is managed by Keycloak
        if (profileUpdate.containsKey("email") && profileUpdate.get("email") != null) {
            profileUpdate.put("email_verified", String.valueOf(user.isEmailVerified()));
        }

        var http = SimpleHttp.doPost(url, session).auth(accessToken).socketTimeOutMillis(60 * 1000) //
                .json(profileUpdate);

        try {
            var response = http.asResponse();
            if (response.getStatus() >= 400 && response.getStatus() <= 599) {
                log.warnf("external profile update failed. realm=%s userId=%s status=%s", //
                        realm.getName(), user.getId(), response.getStatus());
            }
            return toResponse(response, ConsentFormUpdateProfileResult.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getServiceAccountAccessToken(KeycloakSession session, RealmModel realm) {
        var serviceAccountClientId = new RealmConfig(realm) //
                .getString("custom.external.profile.api.service_account.client_id", "app-demo-service");
        return TokenUtils.generateServiceAccountAccessToken(session, serviceAccountClientId, "", null);
    }

    private static String getProfileApiBaseUrl(RealmModel realm) {
        return new RealmConfig(realm) //
                .getString("custom.external.profile.api.base_url", "https://apps.acme.test:4653/api");
    }

    private static String getConsentFormUrl(RealmModel realm, ClientModel client, Set<String> scopeNames, UserModel user) {
        return UriBuilder.fromUri(getProfileApiBaseUrl(realm)) //
                .path("/consentForm/{userId}") //
                .queryParam("clientId", client.getClientId()) //
                .queryParam("scope", String.join("+", scopeNames)) //
                .buildFromMap(Map.of("userId", user.getId())) //
                .toString();
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

    @Data
    public static class ConsentFormProfileDataResponse {

        private Map<String, List<ProfileAttribute>> mapping;
    }

    @Data
    public static class ConsentFormUpdateProfileResult {

        private List<ProfileAttributeError> errors;

        @JsonIgnore
        public boolean hasErrors() {
            return errors != null && !errors.isEmpty();
        }
    }

    @Data
    public static class ProfileAttributeError {

        private String type;

        private String attributeName;

        private String message;
    }
}
