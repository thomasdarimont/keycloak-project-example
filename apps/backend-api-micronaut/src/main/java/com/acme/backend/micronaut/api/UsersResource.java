package com.acme.backend.micronaut.api;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static io.micronaut.security.rules.SecurityRule.IS_AUTHENTICATED;

@Secured(IS_AUTHENTICATED)
@Controller("/api/users")
class UsersResource {

    private static final Logger log = LoggerFactory.getLogger(UsersResource.class);

    @Get("/me")
    public Object me(HttpRequest<?> request, Authentication authentication) {

        log.info("### Accessing {}", request.getUri());

        Object username = authentication.getName();

        Map<String, Object> data = new HashMap<>();
        data.put("message", "Hello " + username);
        data.put("backend", "Micronaut");
        data.put("datetime", Instant.now());
        return data;
    }
}
