package com.acme.backend.springboot.consent.web.claim;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;


/*
 Fetch information when issue tokens from keycloak
 */
@Slf4j
@RestController
@RequestMapping("/api/claims")
class ClaimMappingController {

    @GetMapping("/{user}/{claim}")
    public Object me(ServletWebRequest request, Authentication authentication) {

        log.info("### Accessing {}", request.getRequest().getRequestURI());

        Object username = authentication.getName();

        Map<String, Object> data = new HashMap<>();
        data.put("message", "Hello " + username);
        data.put("backend", "Spring Boot");
        data.put("datetime", Instant.now());
        return data;
    }
}

