package com.github.thomasdarimont.keycloak.custom.config;

import java.util.Collections;
import java.util.Map;

public class MapConfig implements ConfigAccessor {

    private final Map<String, String> config;

    public MapConfig(Map<String, String> config) {
        this.config = config == null ? Collections.emptyMap() : config;
    }

    @Override
    public String getType() {
        return "Map";
    }

    @Override
    public String getSource() {
        return "configMap";
    }

    @Override
    public boolean containsKey(String key) {
        return config.containsKey(key);
    }

    @Override
    public String getValue(String key) {
        return config.get(key);
    }
}
