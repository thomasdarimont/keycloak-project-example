#see https://www.keycloak.org/server/containers
ARG KEYCLOAK_VERSION=21.0.0
FROM quay.io/keycloak/keycloak:$KEYCLOAK_VERSION
ARG QUARKUS_VERSION=2.13.7.Final
ARG OTEL_VERSION=1.17.0
ARG OTEL_ALPHA_VERSION=1.17.0-alpha
USER root

# Add nashorn javascript engine for new Java versions (JDK>14)
# RUN curl -o /opt/keycloak/providers/nashorn-core-15.3.jar https://search.maven.org/remotecontent?filepath=org/openjdk/nashorn/nashorn-core/15.3/nashorn-core-15.3.jar

## Workaround for adding the current certifcate to the cacerts truststore
# Import certificate into cacerts truststore
RUN echo 1659621300842
COPY --chown=keycloak:keycloak "./acme.test+1.pem" "/etc/x509/tls.crt.pem"
RUN keytool -import -cacerts -noprompt -file /etc/x509/tls.crt.pem -storepass changeit || echo "Failed to import cert"

#RUN  export AEROGEAR_VERSION=2.5.1 && \
#     curl https://github.com/aerogear/keycloak-metrics-spi/releases/download/$AEROGEAR_VERSION/keycloak-metrics-spi-$AEROGEAR_VERSION.jar \
#     --location \
#     --output /opt/jboss/keycloak/providers/keycloak-metrics-spi-$AEROGEAR_VERSION.jar

COPY install-otel.sh /opt/install-otel.sh

RUN /opt/install-otel.sh

USER keycloak
