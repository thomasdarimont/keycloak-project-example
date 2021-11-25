package com.github.thomasdarimont.keycloak.tools.sessiongenerator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Slf4j
@SpringBootApplication
public class SessionGeneratorApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(SessionGeneratorApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Bean
    CommandLineRunner clr() {
        return args -> {
            var rt = new RestTemplate();
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            var requestBody = new LinkedMultiValueMap<String, String>();
            requestBody.add("client_id", "test-client");
            requestBody.add("grant_type", "password");
            requestBody.add("username", "user1");
            requestBody.add("password", "test");
//            requestBody.add("scope", "openid");

            HttpEntity<Object> request = new HttpEntity<>(requestBody, headers);
            var issuerUri = "https://id.acme.test:1443/auth/realms/demo";
            var tokenUri = issuerUri + "/protocol/openid-connect/token";

            int sessions = 50_000;

            var sessionsCreated = new AtomicInteger();
            var sessionsFailed = new AtomicInteger();

            Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
                log.info("Sessions created: {} failed: {}", sessionsCreated.get(), sessionsFailed.get());
            }, 1, 3, TimeUnit.SECONDS);

            IntStream.range(0, sessions).parallel().forEach(idx -> {
                var response = rt.exchange(tokenUri, HttpMethod.POST, request, Map.class);
                if (response.getStatusCodeValue() == 200) {
                    // System.out.println("Session created " + i);
                    sessionsCreated.incrementAndGet();
                } else {
//                            System.err.println("Failed to create session status=" + response.getStatusCodeValue());
                    sessionsFailed.incrementAndGet();
                }
            });

        };
    }
}
