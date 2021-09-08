package com.acme.backend.springboot.users.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@Slf4j
@RequiredArgsConstructor
public class UsersController {

    @GetMapping("/me")
    public Object me(ServletWebRequest request, Authentication authentication) {

        log.info("### Accessing {}", request.getRequest().getRequestURI());

        Object username = authentication.getName();
        Map<String, Object> data = new HashMap<>();
        data.put("message", "Hello " + username);
        return data;
    }
}
