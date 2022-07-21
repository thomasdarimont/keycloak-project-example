package com.acme.backend.springboot.consent.web.consent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;


/*
 Handle Consent Form
 */
@Slf4j
@RestController
@RequestMapping("/api/consentForm")
class ConsentFormController {

    // get the metadata of the consent-form and if the form should be shown at all
    @GetMapping("/{user}")
    public Object getForm(ServletWebRequest request, Authentication authentication) {

        log.info("### Accessing {}", request.getRequest().getRequestURI());

        Object username = authentication.getName();

        Map<String, Object> data = new HashMap<>();
        data.put("message", "Hello " + username);
        data.put("backend", "Spring Boot");
        data.put("datetime", Instant.now());
        return data;
    }

    // receive the update from the consent-form
    @PostMapping("/{user}")
    public Object updateForm(ServletWebRequest request, Authentication authentication) {
        return Map.of();
    }

    //Delete?
    //Not required. Consent is removed via Account App

}

