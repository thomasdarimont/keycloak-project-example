package com.github.thomasdarimont.keycloak.custom.userstorage.ldap;

import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.storage.ldap.ReadonlyLDAPUserModelDelegate;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class AcmeReadonlyLDAPUserModelDelegate extends ReadonlyLDAPUserModelDelegate {

    private final Pattern localCustomAttributePattern;

    public AcmeReadonlyLDAPUserModelDelegate(UserModel delegate, Pattern localCustomAttributePattern) {
        super(delegate);
        this.localCustomAttributePattern = localCustomAttributePattern;
    }

    @Override
    public void setAttribute(String name, List<String> values) {

        if (localCustomAttributePattern.matcher(name).matches()) {
            UserModel rootDelegate = getRootDelegate(delegate);
            rootDelegate.setSingleAttribute(name, values.get(0));
            return;
        }

        super.setAttribute(name, values);
    }

    @Override
    public void removeAttribute(String name) {

        if (localCustomAttributePattern.matcher(name).matches()) {
            UserModel rootDelegate = getRootDelegate(delegate);
            rootDelegate.removeAttribute(name);
            return;
        }

        super.removeAttribute(name);
    }

    /**
     * Unwrap deeply nested {@link UserModelDelegate UserModelDelegate's}
     *
     * @param delegate
     * @return
     */
    private UserModel getRootDelegate(UserModel delegate) {
        UserModel current = delegate;
        while (current instanceof UserModelDelegate del) {
            current = del.getDelegate();
        }
        return current;
    }
}
