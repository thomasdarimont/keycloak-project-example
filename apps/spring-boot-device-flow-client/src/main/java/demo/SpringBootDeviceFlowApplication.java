package demo;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootApplication
public class SpringBootDeviceFlowApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(SpringBootDeviceFlowApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Bean
    CommandLineRunner clr() {
        return args -> {
            log.info("Running");

            var clientId = "acme-device-client";
            var clientSecret = "Fm8DAPaaMTBHejIV0NWzVhFAP92CZJ3J";
            var scope = "email";

            var authServerUrl = "https://id.acme.test:8443/auth";
            var realm = "acme-demo";
            var issuerUrl = authServerUrl + "/realms/" + realm;
            var deviceAuthUrl = issuerUrl + "/protocol/openid-connect/auth/device";
            var tokenUrl = issuerUrl + "/protocol/openid-connect/token";

            log.info("Browse to {} and enter the following code.", deviceAuthUrl);

            var deviceCodeResponseEntity = requestDeviceCode(clientId, clientSecret, scope, deviceAuthUrl);

            log.info("Response code: {}", deviceCodeResponseEntity.getStatusCodeValue());
            var deviceCodeResponse = deviceCodeResponseEntity.getBody();
            log.info("{}", deviceCodeResponse);

            log.info("Browse to {} and enter the code {}", deviceCodeResponse.getVerification_uri(), deviceCodeResponse.getUser_code());
            log.info("--- OR ----");
            log.info("Browse to {}", deviceCodeResponse.getVerification_uri_complete());

            System.out.println("Waiting for completion...");

            var expiresAt = Instant.now().plusSeconds(deviceCodeResponse.expires_in);
            while (Instant.now().isBefore(expiresAt)) {
                log.info("Start device flow");
                try {
                    var deviceFlowResponse = checkForDeviceFlowCompletion(clientId, clientSecret, deviceCodeResponse.getDevice_code(), tokenUrl);
                    log.info("Got response status: {}", deviceFlowResponse.getStatusCodeValue());
                    if (deviceFlowResponse.getStatusCodeValue() == 200) {
                        log.info("Success!");
                        log.info("{}", deviceFlowResponse.getBody());
                    } else {
                        log.info("Problem!");
                        log.info("{}", deviceFlowResponse.getBody());
                    }
                    break;
                } catch (HttpClientErrorException.BadRequest badRequest) {
                    log.info("Failed ...");
                    log.info("Continue with polling - sleeping...");
                    TimeUnit.SECONDS.sleep(deviceCodeResponse.getInterval());
                }
            }
        };
    }

    private ResponseEntity<DeviceCodeResponse> requestDeviceCode(String clientId, String clientSecret, String scope, String deviceAuthUrl) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        var requestBody = new LinkedMultiValueMap<String, String>();
        requestBody.add("client_id", clientId);
        requestBody.add("client_secret", clientSecret);
        requestBody.add("grant_type", "urn:ietf:params:oauth:grant-type:device_code");
        requestBody.add("scope", scope);

        var rt = new RestTemplate();

        return rt.postForEntity(deviceAuthUrl, new HttpEntity<>(requestBody, headers), DeviceCodeResponse.class);
    }

    private ResponseEntity<AccessTokenResponse> checkForDeviceFlowCompletion(String clientId, String clientSecret, String deviceCode, String tokenUrl) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        var requestBody = new LinkedMultiValueMap<String, String>();
        requestBody.add("client_id", clientId);
        requestBody.add("client_secret", clientSecret);
        requestBody.add("device_code", deviceCode);
        requestBody.add("grant_type", "urn:ietf:params:oauth:grant-type:device_code");

        var rt = new RestTemplate();

        return rt.postForEntity(tokenUrl, new HttpEntity<>(requestBody, headers), AccessTokenResponse.class);
    }

    @Data
    static class DeviceCodeResponse {

        String device_code;

        String user_code;

        String verification_uri;

        String verification_uri_complete;

        int expires_in;

        int interval;

        Map<String, Object> other = new HashMap<>();

        @JsonAnySetter
        public void setValue(String key, Object value) {
            other.put(key, value);
        }
    }

    @Data
    static class AccessTokenResponse {

        String access_token;

        String refresh_token;

        Map<String, Object> other = new HashMap<>();

        @JsonAnySetter
        public void setValue(String key, Object value) {
            other.put(key, value);
        }
    }
}
