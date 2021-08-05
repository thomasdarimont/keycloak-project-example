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
 * keytool -importcert -noprompt -cacerts -alias "id.acme.test" -storepass changeit -file './config/stage/dev/tls/acme.test+1.pem'
 */
@SpringBootApplication
public class OfflineSessionClient {

    public static void main(String[] args) {
        new SpringApplicationBuilder(OfflineSessionClient.class).web(WebApplicationType.NONE).run(args);
    }

    @Bean
    CommandLineRunner clr(TlsRestTemplateCustomizer tlsRestTemplateCustomizer) {
        return args -> {

            var rt = new RestTemplateBuilder(tlsRestTemplateCustomizer)
                    .rootUri("https://id.acme.test:8443/auth")
                    .build();

            var oauthInfo = OAuthInfo.builder()
                    .clientId("app-mobile")
                    .scope("profile offline_access")
                    .grantType("password")
                    .username("tester")
                    .password("test")
                    .build();

            var oauthClient = new OAuthClient(rt, oauthInfo, 3);

            oauthClient.loadOfflineToken(true, "apps/offline-session-client/data/offline_token");

            if (Arrays.asList(args).contains("--logout")) {
                boolean loggedOut = oauthClient.logout();
                System.out.println("Logout success: " + loggedOut);
                System.exit(0);
            }

            var userInfo = oauthClient.fetchUserInfo();
            System.out.println(userInfo);

            var token = oauthClient.getAccessToken();
            System.out.println("Token: " + token);
        };
    }

    @Builder
    @Data
    static class OAuthInfo {

        final String clientId;
        final String clientSecret;

        final String grantType;

        final String scope;

        final String username;
        final String password;
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

                String offlineToken = null;
                try {
                    offlineToken = new String(Files.readAllBytes(offlineTokenPath), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    log.error("Could not read offline_token", e);
                    return false;
                }

                return doRefreshToken(offlineToken);
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

            var userInfoResponseEntity = rt.exchange("/realms/acme-internal/protocol/openid-connect/userinfo", HttpMethod.GET, new HttpEntity<>(headers), UserInfoResponse.class);
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

            var responseEntity = rt.postForEntity("/realms/acme-internal/protocol/openid-connect/token", new HttpEntity<>(requestBody, headers), AccessTokenResponse.class);
            if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                return false;
            }

            AccessTokenResponse body = responseEntity.getBody();

            if (body.getError() != null) {
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

            var responseEntity = rt.postForEntity("/realms/acme-internal/protocol/openid-connect/token", new HttpEntity<>(requestBody, headers), AccessTokenResponse.class);
            if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                return false;
            }

            AccessTokenResponse body = responseEntity.getBody();
            if (body.getError() != null) {
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

//            ensureTokenValidSeconds(10);

            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
//            headers.setBearerAuth(accessTokenResponse.getAccess_token());

            var requestBody = new LinkedMultiValueMap<String, String>();
            requestBody.add("client_id", oauthInfo.clientId);
            requestBody.add("refresh_token", accessTokenResponse.getRefresh_token());
//            requestBody.add("grant_type", oauthInfo.grantType);
//            requestBody.add("username", oauthInfo.username);
//            requestBody.add("password", oauthInfo.password);
//            requestBody.add("scope", oauthInfo.scope);

            var responseEntity = rt.postForEntity("/realms/acme-internal/protocol/openid-connect/logout", new HttpEntity<>(requestBody, headers), Map.class);
            if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                return false;
            }

            if (offlineTokenPath != null) {
                try {
                    Files.delete(offlineTokenPath);
                } catch (IOException e) {
                    log.error("Could not delete offline_token", e);
                }
            }

            Map body = responseEntity.getBody();

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

            var httpClient = HttpClients.custom()
                    .setSSLSocketFactory(new SSLConnectionSocketFactory(createSslContext()))
                    .build();

            var requestFactory = new HttpComponentsClientHttpRequestFactory();
            requestFactory.setHttpClient(httpClient);

            restTemplate.setRequestFactory(requestFactory);
        }

        private SSLContext createSslContext() {
            SSLContext sslContext = null;

            try {
                TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;
                sslContext = SSLContexts.custom()
                        .loadTrustMaterial(null, acceptingTrustStrategy)
                        .build();
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
