# Stay on 17.0.1 due to regression in Keycloak 18.0.0: admin-console -> index.html /auth[/]js/keycloak.js
ARG KEYCLOAK_VERSION=17.0.1
FROM quay.io/keycloak/keycloak:$KEYCLOAK_VERSION-legacy
USER root

# Add java-11-openjdk-devel JDK for debugging
RUN microdnf update -y && microdnf install -y java-11-openjdk-devel && microdnf clean all

USER 1000

# Add local JMX user for testing
RUN /opt/jboss/keycloak/bin/add-user.sh jmxuser password

# Note that we need to register the smallrye components via 0010-register-smallrye-extensions.cli