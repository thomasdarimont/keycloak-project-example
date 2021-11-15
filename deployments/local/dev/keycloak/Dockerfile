FROM quay.io/bitnami/wildfly:23.0.2-debian-10-r25 as wildfly

FROM quay.io/keycloak/keycloak:15.0.2

# Add smallrye common
COPY --from=wildfly --chown=1000:1000 /opt/bitnami/wildfly/modules/system/layers/base/io/smallrye/common /opt/jboss/keycloak/modules/system/layers/base/io/smallrye/common

# Add smallrye config
COPY --from=wildfly --chown=1000:1000 /opt/bitnami/wildfly/modules/system/layers/base/io/smallrye/config /opt/jboss/keycloak/modules/system/layers/base/io/smallrye/config
COPY --from=wildfly --chown=1000:1000 /opt/bitnami/wildfly/modules/system/layers/base/org/eclipse/microprofile/config /opt/jboss/keycloak/modules/system/layers/base/org/eclipse/microprofile/config
COPY --from=wildfly --chown=1000:1000 /opt/bitnami/wildfly/modules/system/layers/base/org/wildfly/extension/microprofile/config-smallrye /opt/jboss/keycloak/modules/system/layers/base/org/wildfly/extension/microprofile/config-smallrye

# Add smallrye health
COPY --from=wildfly --chown=1000:1000 /opt/bitnami/wildfly/modules/system/layers/base/io/smallrye/health /opt/jboss/keycloak/modules/system/layers/base/io/smallrye/health
COPY --from=wildfly --chown=1000:1000 /opt/bitnami/wildfly/modules/system/layers/base/org/eclipse/microprofile/health /opt/jboss/keycloak/modules/system/layers/base/org/eclipse/microprofile/health
COPY --from=wildfly --chown=1000:1000 /opt/bitnami/wildfly/modules/system/layers/base/org/wildfly/extension/microprofile/health-smallrye /opt/jboss/keycloak/modules/system/layers/base/org/wildfly/extension/microprofile/health-smallrye

# Add smallrye metrics
COPY --from=wildfly --chown=1000:1000 /opt/bitnami/wildfly/modules/system/layers/base/io/smallrye/metrics /opt/jboss/keycloak/modules/system/layers/base/io/smallrye/metrics
COPY --from=wildfly --chown=1000:1000 /opt/bitnami/wildfly/modules/system/layers/base/org/eclipse/microprofile/metrics /opt/jboss/keycloak/modules/system/layers/base/org/eclipse/microprofile/metrics
COPY --from=wildfly --chown=1000:1000 /opt/bitnami/wildfly/modules/system/layers/base/org/wildfly/extension/microprofile/metrics-smallrye /opt/jboss/keycloak/modules/system/layers/base/org/wildfly/extension/microprofile/metrics-smallrye

# Add local JMX user for testing
RUN /opt/jboss/keycloak/bin/add-user.sh jmxuser password

# Note that we need to register the smallrye components via 0010-register-smallrye-extensions.cli