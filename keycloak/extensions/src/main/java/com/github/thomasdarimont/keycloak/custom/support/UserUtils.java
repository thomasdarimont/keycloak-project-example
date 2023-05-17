package com.github.thomasdarimont.keycloak.custom.support;

import org.keycloak.models.UserModel;

public class UserUtils {

    public static String deriveDisplayName(UserModel user) {

        String displayName;
        if (user.getFirstName() != null && user.getLastName() != null) {
            displayName = user.getFirstName().trim() + " " + user.getLastName().trim();
        } else if(user.getFirstName() != null) {
            displayName = user.getFirstName().trim();
        } else if (user.getUsername() != null) {
            displayName = user.getUsername();
        } else {
            displayName = user.getEmail();
        }

        return displayName;
    }
}
