package com.github.thomasdarimont.keycloak.custom.config;

import lombok.RequiredArgsConstructor;
import org.keycloak.models.RealmModel;

@RequiredArgsConstructor
public class RealmConfig implements ConfigAccessor {

    private final RealmModel realm;

    @Override
    public String getType() {
        return "Realm";
    }

    @Override
    public String getSource() {
        return realm.getName();
    }

    public String getValue(String key) {
        return realm.getAttribute(key);
    }

    public boolean containsKey(String key) {
        return realm.getAttributes().containsKey(key);
    }
}
