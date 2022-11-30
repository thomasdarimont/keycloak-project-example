package demo;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * keytool -importcert -noprompt -cacerts -alias "id.acme.test" -storepass changeit -file
 * './config/stage/dev/tls/acme.test+1.pem'
 */
@Slf4j
@SpringBootApplication
public class OfflineSessionClient {

    public static void main(String[] args) {
        new SpringApplicationBuilder(OfflineSessionClient.class).web(WebApplicationType.NONE).run(args);
    }

    @Bean
    CommandLineRunner clr(TlsRestTemplateCustomizer tlsRestTemplateCustomizer) {
        return args -> {
            var oauthInfo = OAuthInfo.builder() //
                    .issuer("https://id.acme.test:8443/auth/realms/acme-internal") //
                    .clientId("app-mobile") //
                    // openid scope required for userinfo!
                    // profile scope allows to read profile info
                    // offline_access scope instructs keycloak to create an offline_session in the KC database
                    .scope("openid profile offline_access").grantType("password") // for the sake of the demo we use grant_type=password
                    .username("tester") //
                    .password("test") //
                    .build();

            var rt = new RestTemplateBuilder(tlsRestTemplateCustomizer).build();

            var oauthClient = new OAuthClient(rt, oauthInfo, 3);

            var offlineAccessValid = oauthClient.loadOfflineToken(true, "apps/offline-session-client/data/offline_token");
            log.info("Offline access valid: {}", offlineAccessValid);

            if (Arrays.asList(args).contains("--logout")) {
                log.info("Logout started...");
                var loggedOut = oauthClient.logout();
                log.info("Logout success: {}", loggedOut);
                System.exit(0);
                return;
            }

            var token = oauthClient.getAccessToken();
            log.info("Token: {}", token);

            var userInfo = oauthClient.fetchUserInfo();
            log.info("UserInfo: {}", userInfo);
        };
    }

    @Builder
    @Data
    static class OAuthInfo {

        final String issuer;

        final String clientId;
        final String clientSecret;

        final String grantType;

        final String scope;

        final String username;
        final String password;

        public String getUserInfoUrl() {
            return getIssuer() + "/protocol/openid-connect/userinfo";
        }

        public String getTokenUrl() {
            return getIssuer() + "/protocol/openid-connect/token";
        }

        public String getLogoutUrl() {
            return getIssuer() + "/protocol/openid-connect/logout";
        }
    }

    @Slf4j
    @RequiredArgsConstructor
    static class OAuthClient {

        private final RestTemplate rt;

        private final OAuthInfo oauthInfo;

        private final int tokenMinSecondsValid;

        private AccessTokenResponse accessTokenResponse;

        private Path offlineTokenPath;

        public boolean loadOfflineToken(boolean obtainIfMissing, String offlineTokenLocation) {

            File offlineTokenFile = new File(offlineTokenLocation);
            this.offlineTokenPath = offlineTokenFile.toPath();

            if (offlineTokenFile.exists()) {

                log.info("Found existing offline token...");

                String offlineToken;
                try {
                    offlineToken = Files.readString(offlineTokenPath);
                } catch (IOException e) {
                    log.error("Could not read offline_token", e);
                    return false;
                }

                var offlineRefreshTokenValid = false;
                try {
                    offlineRefreshTokenValid = doRefreshToken(offlineToken);
                } catch (HttpClientErrorException hcee) {
                    if (hcee.getStatusCode().value() == 400 && hcee.getMessage() != null && hcee.getMessage().contains("invalid_grant")) {
                        log.info("Detected stale refresh token");
                    } else {
                        throw new RuntimeException(hcee);
                    }
                }

                if (offlineRefreshTokenValid) {
                    log.info("Refreshed with existing offline token.");
                    return offlineRefreshTokenValid;
                } else {
                    log.warn("Refresh with existing offline token failed");
                    try {
                        log.warn("Removing stale offline token");
                        Files.delete(offlineTokenPath);
                        log.warn("Removed stale offline token");
                    } catch (IOException e) {
                        log.error("Failed to remove stale offline token", e);
                        return false;
                    }
                }

            }

            if (!obtainIfMissing) {
                return false;
            }

            boolean success = this.sendOfflineTokenRequest();
            if (success) {
                log.info("Obtain new offline token...");
                try {
                    Files.write(offlineTokenPath, accessTokenResponse.getRefresh_token().getBytes(StandardCharsets.UTF_8));
                    return true;
                } catch (IOException e) {
                    log.error("Could not write offline_token", e);
                }
            }
            return false;
        }

        public UserInfoResponse fetchUserInfo() {

            ensureTokenValidSeconds(tokenMinSecondsValid);

            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBearerAuth(accessTokenResponse.getAccess_token());
            log.info("Fetching data form userinfo: {}", oauthInfo.getUserInfoUrl());
            var userInfoResponseEntity = rt.exchange(oauthInfo.getUserInfoUrl(), HttpMethod.GET, new HttpEntity<>(headers), UserInfoResponse.class);
            return userInfoResponseEntity.getBody();
        }

        private boolean doRefreshToken(String refreshToken) {

            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            var requestBody = new LinkedMultiValueMap<String, String>();
            requestBody.add("client_id", oauthInfo.clientId);
            requestBody.add("grant_type", "refresh_token");
            requestBody.add("refresh_token", refreshToken);
            requestBody.add("scope", oauthInfo.scope);

            var responseEntity = rt.postForEntity(oauthInfo.getTokenUrl(), new HttpEntity<>(requestBody, headers), AccessTokenResponse.class);
            if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                return false;
            }

            AccessTokenResponse body = responseEntity.getBody();

            if (body == null || body.getError() != null) {
                return false;
            }

            accessTokenResponse = body;
            return true;
        }

        private boolean sendOfflineTokenRequest() {

            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            var requestBody = new LinkedMultiValueMap<String, String>();
            requestBody.add("client_id", oauthInfo.clientId);
            requestBody.add("grant_type", oauthInfo.grantType);
            requestBody.add("username", oauthInfo.username);
            requestBody.add("password", oauthInfo.password);
            requestBody.add("scope", oauthInfo.scope);

            var responseEntity = rt.postForEntity(oauthInfo.getTokenUrl(), new HttpEntity<>(requestBody, headers), AccessTokenResponse.class);
            if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                return false;
            }

            AccessTokenResponse body = responseEntity.getBody();
            if (body == null || body.getError() != null) {
                return false;
            }

            accessTokenResponse = body;
            return true;
        }

        public void ensureTokenValidSeconds(int minSecondsValid) {

            Objects.requireNonNull(accessTokenResponse, "accessTokenResponse");
            long accessTokenExpiresAtSeconds = accessTokenResponse.getCreatedAtSeconds() + accessTokenResponse.getExpires_in();
            long nowSeconds = System.currentTimeMillis() / 1000;
            long remainingLifetimeSeconds = accessTokenExpiresAtSeconds - nowSeconds;
            if (remainingLifetimeSeconds < minSecondsValid) {
                doRefreshToken(accessTokenResponse.refresh_token);
            }
        }

        public String getAccessToken() {
            ensureTokenValidSeconds(tokenMinSecondsValid);
            return this.accessTokenResponse.access_token;
        }

        public boolean logout() {

            if (accessTokenResponse == null || accessTokenResponse.getRefresh_token() == null) {
                log.error("Could not logout offline-client: missing offline token");
                return false;
            }

            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            var requestBody = new LinkedMultiValueMap<String, String>();
            requestBody.add("client_id", oauthInfo.clientId);
            requestBody.add("refresh_token", accessTokenResponse.getRefresh_token());

            var responseEntity = rt.postForEntity(oauthInfo.getLogoutUrl(), new HttpEntity<>(requestBody, headers), Map.class);
            if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                log.error("Could not logout offline-client: logout failed");
                return false;
            }

            if (offlineTokenPath != null) {
                try {
                    Files.delete(offlineTokenPath);
                } catch (IOException e) {
                    log.error("Could not delete offline_token", e);
                }
            }

            accessTokenResponse = null;
            return true;
        }
    }

    @Slf4j
    @Component
    @RequiredArgsConstructor
    static class TlsRestTemplateCustomizer implements RestTemplateCustomizer {

        @Override
        public void customize(RestTemplate restTemplate) {

            var httpClient = HttpClients.custom().setSSLSocketFactory(new SSLConnectionSocketFactory(createSslContext())).build();

            var requestFactory = new HttpComponentsClientHttpRequestFactory();
            requestFactory.setHttpClient(httpClient);

            restTemplate.setRequestFactory(requestFactory);
        }

        private SSLContext createSslContext() {
            SSLContext sslContext = null;

            try {
                TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;
                sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
            } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
                e.printStackTrace();
            }
            return sslContext;
        }
    }

    @Data
    static class AccessTokenResponse {

        final long createdAtSeconds = System.currentTimeMillis() / 1000;

        String access_token;

        String refresh_token;

        String error;

        int expires_in;

        Map<String, Object> metadata = new HashMap<>();

        @JsonAnySetter
        public void setMetadata(String key, Object value) {
            metadata.put(key, value);
        }
    }

    @Data
    static class UserInfoResponse {

        Map<String, Object> userdata = new HashMap<>();

        @JsonAnySetter
        public void setMetadata(String key, Object value) {
            userdata.put(key, value);
        }
    }
}
