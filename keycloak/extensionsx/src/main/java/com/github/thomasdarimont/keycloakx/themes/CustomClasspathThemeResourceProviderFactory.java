package com.github.thomasdarimont.keycloakx.themes;

import com.google.auto.service.AutoService;
import org.keycloak.theme.ClasspathThemeResourceProviderFactory;
import org.keycloak.theme.ThemeResourceProviderFactory;

/**
 * Workaround for missing {@link ClasspathThemeResourceProviderFactory} in Keycloak.X to support theme-resources in extension jars.
 *
 * @see https://github.com/keycloak/keycloak/issues/9653
 */
@AutoService(ThemeResourceProviderFactory.class)
public class CustomClasspathThemeResourceProviderFactory extends ClasspathThemeResourceProviderFactory {

    private static final String ID = "custom-classpath-themeresource-provider";

    public CustomClasspathThemeResourceProviderFactory() {
        super(ID, Thread.currentThread().getContextClassLoader());
    }

    @Override
    public String getId() {
        return ID;
    }
}
