#see https://www.keycloak.org/server/containers
ARG KEYCLOAK_VERSION=19.0.1
FROM quay.io/keycloak/keycloak:$KEYCLOAK_VERSION

USER root

# Add java-11-openjdk-devel JDK for debugging
#RUN microdnf update -y && microdnf install -y java-11-openjdk-devel && microdnf clean all
RUN microdnf update -y && microdnf install -y java-17-openjdk-devel && microdnf clean all

# Add nashorn javascript engine for new Java versions (JDK>14)
# RUN curl -o /opt/keycloak/providers/nashorn-core-15.3.jar https://search.maven.org/remotecontent?filepath=org/openjdk/nashorn/nashorn-core/15.3/nashorn-core-15.3.jar

## Workaround for adding the current certifcate to the cacerts truststore
# Import certificate into cacerts truststore
COPY --chown=keycloak:keycloak "./acme.test+1.pem" "/etc/x509/tls.crt.pem"
RUN keytool -import -cacerts -noprompt -file /etc/x509/tls.crt.pem -storepass changeit || echo "Failed to import cert"

#RUN  export AEROGEAR_VERSION=2.5.1 && \
#     curl https://github.com/aerogear/keycloak-metrics-spi/releases/download/$AEROGEAR_VERSION/keycloak-metrics-spi-$AEROGEAR_VERSION.jar \
#     --location \
#     --output /opt/jboss/keycloak/providers/keycloak-metrics-spi-$AEROGEAR_VERSION.jar

USER keycloak
