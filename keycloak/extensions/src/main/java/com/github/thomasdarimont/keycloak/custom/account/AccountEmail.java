package com.github.thomasdarimont.keycloak.custom.account;

import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.email.freemarker.beans.ProfileBean;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.HashMap;
import java.util.Map;

public class AccountEmail {

    public static void send(EmailTemplateProvider emailTemplateProvider, RealmModel realm, UserModel user, SendEmailTask sendEmailTask) throws EmailException {

        if (emailTemplateProvider == null) {
            throw new EmailException("Missing emailTemplateProvider");
        }

        emailTemplateProvider.setRealm(realm);
        emailTemplateProvider.setUser(user);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("user", new ProfileBean(user));

        sendEmailTask.sendEmail(emailTemplateProvider, attributes);
    }

    @FunctionalInterface
    public interface SendEmailTask {

        void sendEmail(EmailTemplateProvider emailTemplateProvider, Map<String, Object> attributes) throws EmailException;
    }
}
