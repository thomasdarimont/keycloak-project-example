package com.github.thomasdarimont.keycloak.custom.security.friendlycaptcha;

import com.github.thomasdarimont.keycloak.custom.config.RealmConfig;
import org.jetbrains.annotations.NotNull;
import org.keycloak.models.RealmModel;

public class FriendlyCaptchaConfig extends RealmConfig {

    public static final String DEFAULT_VERIFICATION_URL = "https://api.friendlycaptcha.com/api/v1/siteverify";

    public FriendlyCaptchaConfig(RealmModel realm) {
        super(realm);
    }

    @NotNull
    public String getSiteKey() {
        return getString("friendlyCaptchaSiteKey");
    }

    @NotNull
    public String getSolutionFieldName() {
        return getString("friendlyCaptchaSolutionFieldName");
    }

    public String getSecret() {
        return getString("friendlyCaptchaSecret");
    }

    public String getStart() {
        return getString("friendlyCaptchaStart");
    }

    public boolean isEnabled() {
        return getBoolean("friendlyCaptchaEnabled", false);
    }

    public String getSourceModule() {
        return getString("friendlyCaptchaSourceModule");
    }

    public String getSourceNoModule() {
        return getString("friendlyCaptchaSourceNoModule");
    }

    public String getUrl() {
        return getString("friendlyCaptchaVerificationUrl", DEFAULT_VERIFICATION_URL);
    }
}
