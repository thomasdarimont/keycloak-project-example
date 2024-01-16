package com.github.thomasdarimont.keycloak.custom.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.keycloak.models.RealmModel;

@Getter
@AllArgsConstructor
@RequiredArgsConstructor
public class RealmConfig implements ConfigAccessor {

    private final RealmModel realm;

    private String prefix;

    @Override
    public String getType() {
        return "Realm";
    }

    @Override
    public String getSource() {
        return realm.getName();
    }

    public String getValue(String key) {
        return realm.getAttribute(prefixed(key));
    }

    public boolean containsKey(String key) {
        return realm.getAttributes().containsKey(prefixed(key));
    }

    private String prefixed(String key) {
        return prefix == null ? key : prefix + key;
    }
}
