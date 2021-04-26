ARG KEYCLOAK_VERSION=12.0.4
FROM quay.io/keycloak/keycloak:$KEYCLOAK_VERSION

COPY --chown=jboss:root maven/cli/ /opt/jboss/startup-scripts
COPY --chown=jboss:root maven/keycloak-extensions/ /opt/jboss/keycloak/standalone/deployments
COPY --chown=jboss:root maven/keycloak-theme/ /opt/jboss/keycloak/themes/custom
COPY --chown=jboss:root profile.properties /opt/jboss/keycloak/standalone/configuration/
