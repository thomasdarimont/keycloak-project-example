package com.github.thomasdarimont.keycloak.webapp.support;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

public class HttpServletRequestUtils {

    public static Optional<HttpServletRequest> getCurrentHttpServletRequest() {
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return Optional.ofNullable(servletRequestAttributes).map(ServletRequestAttributes::getRequest);
    }

    public static Optional<HttpServletResponse> getCurrentHttpServletResponse() {
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return Optional.ofNullable(servletRequestAttributes).map(ServletRequestAttributes::getResponse);
    }

    public static Optional<HttpSession> getCurrentHttpSession(boolean create) {
        return getCurrentHttpServletRequest().map(req -> req.getSession(false));
    }
}
