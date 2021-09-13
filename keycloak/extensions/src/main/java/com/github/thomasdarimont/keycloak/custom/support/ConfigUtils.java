package com.github.thomasdarimont.keycloak.custom.support;

import org.keycloak.models.AuthenticatorConfigModel;

import java.util.Map;

public class ConfigUtils {

    public static Map<String, String> getConfig(AuthenticatorConfigModel configModel, Map<String, String> defaultConfig) {

        if (configModel == null) {
            return defaultConfig;
        }

        Map<String, String> config = configModel.getConfig();
        if (config == null) {
            return defaultConfig;
        }

        return config;
    }

    public static String getConfigValue(AuthenticatorConfigModel configModel, String key, String defaultValue) {

        if (configModel == null) {
            return defaultValue;
        }

        return getConfigValue(configModel.getConfig(), key, defaultValue);
    }

    public static String getConfigValue(Map<String, String> config, String key, String defaultValue) {

        if (config == null) {
            return defaultValue;
        }

        return config.getOrDefault(key, defaultValue);
    }
}
