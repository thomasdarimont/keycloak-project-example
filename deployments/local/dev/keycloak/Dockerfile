FROM quay.io/keycloak/keycloak:16.1.0

USER root

# Add java-11-openjdk-devel JDK for debugging
RUN microdnf update -y && microdnf install -y java-11-openjdk-devel && microdnf clean all

USER 1000

# Add local JMX user for testing
RUN /opt/jboss/keycloak/bin/add-user.sh jmxuser password

# Note that we need to register the smallrye components via 0010-register-smallrye-extensions.cli