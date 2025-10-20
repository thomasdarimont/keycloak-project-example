package com.github.thomasdarimont.keycloak.custom.security.friendlycaptcha;

import lombok.extern.jbosslog.JBossLog;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.models.KeycloakSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * FriendlyCaptcha client to verify a captcha solution.
 */
@JBossLog
public class FriendlyCaptchaClient {

    private final KeycloakSession session;

    private final FriendlyCaptchaConfig config;

    public FriendlyCaptchaClient(KeycloakSession session, FriendlyCaptchaConfig config) {
        this.session = session;
        this.config = config;
    }

    public boolean verifySolution(String solutionValue) {

        // see: https://docs.friendlycaptcha.com/#/verification_api
        var requestBody = new HashMap<String, String>();
        requestBody.put("solution", solutionValue);
        requestBody.put("sitekey", config.getSiteKey());
        requestBody.put("secret", config.getSecret());

        var post = SimpleHttp.create(session).doPost(config.getUrl());
        post.json(requestBody);
        try (var response = post.asResponse()) {
            var responseBody = response.asJson(Map.class);

            if (Boolean.parseBoolean(String.valueOf(responseBody.get("success")))) {
                log.debugf("Captcha verification service returned success. status=%s", response.getStatus());
                return true;
            } else {
                log.warnf("Captcha verification returned error. status=%s, errors=%s", response.getStatus(), responseBody.get("errors"));
            }

        } catch (IOException e) {
            log.error("Could not access captcha verification service.", e);
        }
        return false;
    }
}
