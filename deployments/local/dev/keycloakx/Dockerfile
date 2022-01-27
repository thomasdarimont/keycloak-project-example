FROM quay.io/keycloak/keycloak-x:16.1.1

USER root

# Add java-11-openjdk-devel JDK for debugging
RUN microdnf update -y && microdnf install -y java-11-openjdk-devel && microdnf clean all

## Workaround for adding the current certifcate to the cacerts truststore
# Import certificate into cacerts truststore
COPY --chown=keycloak:keycloak "./acme.test+1.pem" "/etc/x509/tls.crt.pem"
RUN keytool -import -cacerts -noprompt -file /etc/x509/tls.crt.pem -storepass changeit

#RUN  export AEROGEAR_VERSION=2.5.1 && \
#     curl https://github.com/aerogear/keycloak-metrics-spi/releases/download/$AEROGEAR_VERSION/keycloak-metrics-spi-$AEROGEAR_VERSION.jar \
#     --location \
#     --output /opt/jboss/keycloak/providers/keycloak-metrics-spi-$AEROGEAR_VERSION.jar

USER keycloak
