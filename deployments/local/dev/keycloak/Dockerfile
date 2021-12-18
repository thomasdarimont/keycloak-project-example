FROM quay.io/keycloak/keycloak:15.1.1

# Add local JMX user for testing
RUN /opt/jboss/keycloak/bin/add-user.sh jmxuser password

# Note that we need to register the smallrye components via 0010-register-smallrye-extensions.cli