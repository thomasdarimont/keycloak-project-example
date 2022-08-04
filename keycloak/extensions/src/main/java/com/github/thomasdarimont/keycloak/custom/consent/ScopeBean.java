package com.github.thomasdarimont.keycloak.custom.consent;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.RequiredArgsConstructor;
import org.keycloak.models.ClientScopeModel;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
public class ScopeBean {

    public static final Comparator<ScopeBean> DEFAULT_ORDER;

    static {
        DEFAULT_ORDER = Comparator.comparing(ScopeBean::getGuiOrder);
    }

    @JsonIgnore
    private final ClientScopeModel scopeModel;
    @JsonIgnore
    private final boolean optional;
    @JsonIgnore
    private final boolean granted;

    @JsonIgnore
    private final List<ScopeAttributeBean> attributes;

    public boolean isOptional() {
        return optional;
    }

    public boolean isGranted() {
        return granted;
    }

    public String getGuiOrder() {

        String guiOrder = getScopeModel().getGuiOrder();
        if (guiOrder != null) {
            return guiOrder;
        }

        return getName();
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

    public List<ScopeAttributeBean> getAttributes() {
        return Collections.unmodifiableList(attributes);
    }

    @Override
    public String toString() {
        return scopeModel.getName();
    }
}
