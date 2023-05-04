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

import java.util.ArrayList;
import java.util.Map;
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
            var rt = new RestTemplate();
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            int sessions = 50_000;
            int maxConcrrentRequests = 2048;

            var sessionsCreated = new AtomicInteger();
            var sessionsFailed = new AtomicInteger();

            var issuerUri = "https://id.acme.test:8443/auth/realms/acme-offline-test";
            var tokenUri = issuerUri + "/protocol/openid-connect/token";

            Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
                log.info("Sessions created: {} failed: {}", sessionsCreated.get(), sessionsFailed.get());

                if (sessionsCreated.get() + sessionsFailed.get() >= sessions) {
                    System.exit(0);
                }

            }, 1, 3, TimeUnit.SECONDS);

            var results = new ArrayList<Future<?>>();
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                var semaphore = new Semaphore(maxConcrrentRequests);
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
                                        // System.out.println("Session created " + i);
                                        sessionsCreated.incrementAndGet();
                                    } else {
                                        //                            System.err.println("Failed to create session status=" + response.getStatusCodeValue());
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
        };
    }
}
