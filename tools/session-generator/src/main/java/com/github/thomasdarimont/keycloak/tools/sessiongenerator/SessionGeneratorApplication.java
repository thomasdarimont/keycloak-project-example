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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@SpringBootApplication
public class SessionGeneratorApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(SessionGeneratorApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Bean
    CommandLineRunner clr() {
        return args -> {

            String baseUrl = "https://id.acme.test/auth";
            var realmName = "acme-offline-test";
            var issuerUri = baseUrl + "/realms/" + realmName;
            var adminUri = baseUrl + "/admin/realms/" + realmName;

//            deleteOfflineSession(issuerUri, adminUri, "897768ae-be97-3c8f-9e47-07e006360799", "app-mobile");

            generateOfflineSessions(issuerUri, 100_000);
        };
    }

    private boolean deleteOfflineSession(String issuerUri, String adminUri, String userUuid, String clientId) {

        var userUri = adminUri + "/users/" + userUuid;
        var consentUri = userUri + "/consents/" + clientId;

        var rt = new RestTemplate();
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBearerAuth(getAdminSvcAccessToken(issuerUri));


        var request = new HttpEntity<>(headers);
        try {
            var deleteConsentResponse = rt.exchange(consentUri, HttpMethod.DELETE, request, Map.class);
            System.out.println(deleteConsentResponse.getStatusCode());
            return deleteConsentResponse.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException hcee) {
            System.out.printf("Could not delete client session %s%n", hcee.getMessage());
            return false;
        }
    }

    private static String getAdminSvcAccessToken(String issuerUri) {
        var rt = new RestTemplate();
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        var requestBody = new LinkedMultiValueMap<String, String>();
        requestBody.add("client_id", "acme-admin-svc");
        requestBody.add("client_secret", "jOOgfhjFT2OWpimKUzRCj0or5FsUEqaK");
        requestBody.add("grant_type", "client_credentials");
        requestBody.add("scope", "email");

        var request = new HttpEntity<>(requestBody, headers);

        var tokenUri = issuerUri + "/protocol/openid-connect/token";
        var accessTokenResponse = rt.exchange(tokenUri, HttpMethod.POST, request, Map.class);

        var accessTokenResponseBody = accessTokenResponse.getBody();
        return (String) accessTokenResponseBody.get("access_token");
    }

    private static void generateOfflineSessions(String issuerUri, int sessions) {
        var rt = new RestTemplate();
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        var tokenUri = issuerUri + "/protocol/openid-connect/token";

        int maxConcurrentRequests = 180;

        var sessionsCreated = new AtomicInteger();
        var sessionsFailed = new AtomicInteger();

        var sessionsFile = Paths.get("data/" + DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").format(LocalDateTime.now()) + ".sessions");


        var generatedTokens = new ConcurrentLinkedDeque<Map.Entry<Integer, String>>();

        try (var offlineTokenWriter = new PrintWriter(Files.newBufferedWriter(sessionsFile, StandardOpenOption.CREATE_NEW))) {

            Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
                log.info("Sessions created: {} failed: {}", sessionsCreated.get(), sessionsFailed.get());

                if (sessionsCreated.get() + sessionsFailed.get() >= sessions) {
                    System.exit(0);
                    return;
                }

                int tokenCount = 0;
                Map.Entry<Integer, String> entry;
                while ((entry = generatedTokens.poll()) != null) {
                    Integer idx = entry.getKey();
                    String refreshToken = entry.getValue();

                    offlineTokenWriter.print(idx);
                    offlineTokenWriter.print('=');
                    offlineTokenWriter.println(refreshToken);
                    tokenCount++;
                }
                log.info("Wrote {} tokens to disk.", tokenCount);

            }, 1, 3, TimeUnit.SECONDS);

            var results = new ArrayList<Future<?>>();
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                var semaphore = new Semaphore(maxConcurrentRequests);
                for (var i = 0; i < sessions; i++) {

                    var idx = i;

                    results.add(executor.submit(() -> {

                        while (true) {
                            try {
                                semaphore.acquire();
                                try {
                                    var requestBody = new LinkedMultiValueMap<String, String>();
                                    requestBody.add("client_id", "app-mobile");
                                    requestBody.add("grant_type", "password");
                                    requestBody.add("username", "user" + idx);
                                    requestBody.add("password", "test");
                                    requestBody.add("scope", "openid profile offline_access");

                                    HttpEntity<Object> request = new HttpEntity<>(requestBody, headers);

                                    var response = rt.exchange(tokenUri, HttpMethod.POST, request, Map.class);
                                    if (response.getStatusCode().value() == 200) {
                                        var refeshToken = (String) response.getBody().get("refresh_token");
                                        // System.out.println("Session created " + i);
                                        sessionsCreated.incrementAndGet();
                                        generatedTokens.add(new AbstractMap.SimpleImmutableEntry<>(idx, refeshToken));
                                    } else {
                                        // System.err.println("Failed to create session status=" + response.getStatusCodeValue());
                                        sessionsFailed.incrementAndGet();
                                    }
                                    return;
                                } finally {
                                    semaphore.release();
                                }
                            } catch (Exception ex) {
                                log.warn("Error during session creation waiting and retry... " + idx);
                                try {
                                    TimeUnit.MILLISECONDS.sleep(500 + ThreadLocalRandom.current().nextInt(500));
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    }));
                }
            }

            results.forEach(f -> {
                try {
                    f.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });

            System.out.printf("Generation of %s completed.%n", sessions);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
