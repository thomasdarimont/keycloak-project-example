package com.github.thomasdarimont.keycloak.custom.config;

import lombok.RequiredArgsConstructor;

import java.util.function.Function;

import static java.util.function.Function.identity;

public interface ConfigAccessor {

    String getType();

    String getSource();

    boolean containsKey(String key);

    String getValue(String key);

    default <T> T getValueOrDefault(String key, T defaultValue, Function<String, T> converter) {

        String value = getValue(key);
        if (value == null) {
            return defaultValue;
        }

        return converter.apply(value);
    }

    default <T> T getValue(String key, Function<String, T> converter) {

        String value = getValue(key);
        if (value == null) {
            throw new MissingKeyException(getType(), getSource(), key);
        }

        return converter.apply(value);
    }

    default String getString(String key, String defaultValue) {
        return getValueOrDefault(key, defaultValue, identity());
    }

    default String getString(String key) {
        return getValue(key, identity());
    }

    default Integer getInt(String key, Integer defaultValue) {
        return getValueOrDefault(key, defaultValue, Integer::parseInt);
    }

    default int getInt(String key) {
        return getValue(key, Integer::parseInt);
    }

    default <T extends Enum<T>> T getEnum(Class<T> enumType, String key, T defaultValue) {
        return getValueOrDefault(key, defaultValue, s -> Enum.valueOf(enumType, s));
    }

    default <T extends Enum<T>> T getEnum(Class<T> enumType, String key) {
        return getValue(key, s -> Enum.valueOf(enumType, s));
    }

    default Long getLong(String key, Long defaultValue) {
        return getValueOrDefault(key, defaultValue, Long::parseLong);
    }

    default long getLong(String key) {
        return getValue(key, Long::parseLong);
    }

    default Boolean getBoolean(String key, Boolean defaultValue) {
        return getValueOrDefault(key, defaultValue, Boolean::parseBoolean);
    }

    default boolean getBoolean(String key) {
        return getValue(key, Boolean::parseBoolean);
    }

    /**
     * Check if the value is present and non-null and not an empty string.
     *
     * @param key
     * @param defaultValue
     * @return
     */
    default boolean isConfigured(String key, boolean defaultValue) {
        if (!containsKey(key)) {
            return defaultValue;
        }
        String value = getValue(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return true;
    }

    @RequiredArgsConstructor
    class MissingKeyException extends RuntimeException {

        private final String type;

        private final String source;

        private final String key;

        @Override
        public String getMessage() {
            return String.format("Missing %s Config Key. %s=%s, key=%s", type, type, source, key);
        }
    }
}
