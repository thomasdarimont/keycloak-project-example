package com.acme.backend.springreactive.users;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;

@Slf4j
@Component
public class UserHandlers {

    public Mono<ServerResponse> me(ServerRequest request) {

        log.info("### Accessing {}", request.uri());

        return request.principal().flatMap(auth -> {

            var username = auth.getName();
            var data = new HashMap<String, Object>();
            data.put("message", "Hello " + username);
            data.put("backend", "Spring Boot Reactive");
            data.put("datetime", Instant.now());

            return ServerResponse.ok().bodyValue(data);
        });

    }
}

