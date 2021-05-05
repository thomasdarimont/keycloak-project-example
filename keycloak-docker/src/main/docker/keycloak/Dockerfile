ARG KEYCLOAK_VERSION=12.0.4
FROM quay.io/keycloak/keycloak:$KEYCLOAK_VERSION

# Add custom Startup-Scripts
COPY --chown=jboss:root maven/cli/ /opt/jboss/startup-scripts

# Add feature configuration
COPY --chown=jboss:root profile.properties /opt/jboss/keycloak/standalone/configuration/

# Add Keycloak Extensions
COPY --chown=jboss:root maven/keycloak-extensions/ /opt/jboss/keycloak/standalone/deployments

# Add custom Theme
COPY --chown=jboss:root maven/keycloak-themes/apps/ /opt/jboss/keycloak/themes/apps
COPY --chown=jboss:root maven/keycloak-themes/internal/ /opt/jboss/keycloak/themes/internal
