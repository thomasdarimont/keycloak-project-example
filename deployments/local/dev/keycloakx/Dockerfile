ARG KEYCLOAK_VERSION=26.4.5

## Example for adding custom packages (curl) to Keycloak image
## See: https://www.keycloak.org/server/containers#_installing_additional_rpm_packages
FROM registry.access.redhat.com/ubi9 AS ubi-micro-build
RUN mkdir -p /mnt/rootfs
RUN dnf install --installroot /mnt/rootfs curl --releasever 9 --setopt install_weak_deps=false --nodocs -y && \
    dnf --installroot /mnt/rootfs clean all && \
    rpm --root /mnt/rootfs -e --nodeps setup

#see https://www.keycloak.org/server/containers
FROM quay.io/keycloak/keycloak:$KEYCLOAK_VERSION
COPY --from=ubi-micro-build /mnt/rootfs /
#FROM thomasdarimont/keycloak:21.0.999.1
USER root

#RUN echo "Add nashorn javascript engine"
#RUN --mount=from=busybox:1.36.0,src=/bin/,dst=/bin/ \
# wget -O /opt/keycloak/providers/nashorn-core-15.3.jar https://search.maven.org/remotecontent?filepath=org/openjdk/nashorn/nashorn-core/15.3/nashorn-core-15.3.jar


## Workaround for adding the current certifcate to the cacerts truststore
# Import certificate into cacerts truststore
RUN echo 1659621300842
COPY --chown=keycloak:keycloak "./acme.test+1.pem" "/etc/x509/tls.crt.pem"
RUN keytool -import -cacerts -noprompt -file /etc/x509/tls.crt.pem -storepass changeit || echo "Failed to import cert"

#RUN  export AEROGEAR_VERSION=2.5.1 && \
#     curl https://github.com/aerogear/keycloak-metrics-spi/releases/download/$AEROGEAR_VERSION/keycloak-metrics-spi-$AEROGEAR_VERSION.jar \
#     --location \
#     --output /opt/jboss/keycloak/providers/keycloak-metrics-spi-$AEROGEAR_VERSION.jar

# sRUN echo "Downloading OpenTelemetry Javaagent and support libs"

#ENV OTEL_AGENT_VERSION=1.33.6
#ENV OTEL_TRACE_VERSION=1.42.1

#ADD --chown=keycloak:keycloak https://search.maven.org/remotecontent?filepath=io/opentelemetry/javaagent/opentelemetry-javaagent/$OTEL_AGENT_VERSION/opentelemetry-javaagent-$OTEL_AGENT_VERSION.jar /opt/keycloak/opentelemetry-javaagent.jar
#ADD --chown=keycloak:keycloak https://repo1.maven.org/maven2/io/opentelemetry/opentelemetry-extension-trace-propagators/$OTEL_TRACE_VERSION/opentelemetry-extension-trace-propagators-$OTEL_TRACE_VERSION.jar /opt/keycloak/providers/opentelemetry-extension-trace-propagators.jar

USER keycloak
