package com.github.thomasdarimont.keycloak.custom.consent;

import lombok.RequiredArgsConstructor;
import org.keycloak.models.ClientScopeModel;

import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public class ScopeBean {

    private final ClientScopeModel scopeModel;
    private final boolean optional;
    private final boolean granted;
    private final List<ScopeFieldBean> scopeFields;

    public boolean isOptional() {
        return optional;
    }

    public boolean isGranted() {
        return granted;
    }

    public ClientScopeModel getScopeModel() {
        return scopeModel;
    }

    public String getName() {
        return scopeModel.getName();
    }

    public String getDescription() {
        return scopeModel.getDescription();
    }

    public List<ScopeFieldBean> getFields() {
        return Collections.unmodifiableList(scopeFields);
    }

    @Override
    public String toString() {
        return scopeModel.getName();
    }
}
