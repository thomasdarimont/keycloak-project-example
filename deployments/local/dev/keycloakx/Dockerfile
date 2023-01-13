#see https://www.keycloak.org/server/containers
ARG KEYCLOAK_VERSION=20.0.3
FROM quay.io/keycloak/keycloak:$KEYCLOAK_VERSION
ARG QUARKUS_VERSION=2.13.6.Final
ARG OTEL_VERSION=1.17.0
ARG OTEL_ALPHA_VERSION=1.17.0-alpha
USER root

# Add java-11-openjdk-devel JDK for debugging
#RUN microdnf update -y && microdnf install -y java-11-openjdk-devel && microdnf clean all
RUN microdnf update -y && microdnf install -y java-17-openjdk-devel && microdnf clean all

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

RUN curl https://repo1.maven.org/maven2/io/quarkus/quarkus-opentelemetry/$QUARKUS_VERSION/quarkus-opentelemetry-$QUARKUS_VERSION.jar \
    -L \
    -o /opt/keycloak/providers/quarkus-opentelemetry-$QUARKUS_VERSION.jar

RUN curl https://repo1.maven.org/maven2/io/opentelemetry/opentelemetry-sdk/$OTEL_VERSION/opentelemetry-sdk-$OTEL_VERSION.jar \
    -L \
    -o /opt/keycloak/providers/opentelemetry-sdk-$OTEL_VERSION.jar

RUN curl https://repo1.maven.org/maven2/io/opentelemetry/opentelemetry-api/$OTEL_VERSION/opentelemetry-api-$OTEL_VERSION.jar \
    -L \
    -o /opt/keycloak/providers/opentelemetry-api-$OTEL_VERSION.jar

RUN curl https://repo1.maven.org/maven2/io/opentelemetry/opentelemetry-context/$OTEL_VERSION/opentelemetry-context-$OTEL_VERSION.jar \
    -L \
    -o /opt/keycloak/providers/opentelemetry-context-$OTEL_VERSION.jar

RUN curl https://repo1.maven.org/maven2/io/opentelemetry/opentelemetry-sdk-common/$OTEL_VERSION/opentelemetry-sdk-common-$OTEL_VERSION.jar \
    -L \
    -o /opt/keycloak/providers/opentelemetry-sdk-common-$OTEL_VERSION.jar

RUN curl https://repo1.maven.org/maven2/io/opentelemetry/opentelemetry-sdk-trace/$OTEL_VERSION/opentelemetry-sdk-trace-$OTEL_VERSION.jar \
    -L \
    -o /opt/keycloak/providers/opentelemetry-sdk-trace-$OTEL_VERSION.jar

RUN curl https://repo1.maven.org/maven2/io/opentelemetry/opentelemetry-sdk-metrics/$OTEL_VERSION/opentelemetry-sdk-metrics-$OTEL_VERSION.jar \
    -L \
    -o /opt/keycloak/providers/opentelemetry-sdk-metrics-$OTEL_VERSION.jar

RUN curl https://repo1.maven.org/maven2/io/opentelemetry/opentelemetry-sdk-logs/$OTEL_ALPHA_VERSION/opentelemetry-sdk-logs-$OTEL_ALPHA_VERSION.jar \
    -L \
    -o /opt/keycloak/providers/opentelemetry-sdk-logs-$OTEL_ALPHA_VERSION.jar

RUN curl https://repo1.maven.org/maven2/io/opentelemetry/opentelemetry-extension-annotations/$OTEL_VERSION/opentelemetry-extension-annotations-$OTEL_VERSION.jar \
    -L \
    -o /opt/keycloak/providers/opentelemetry-extension-annotations-$OTEL_VERSION.jar

RUN curl https://repo1.maven.org/maven2/io/opentelemetry/opentelemetry-sdk-extension-autoconfigure-spi/$OTEL_VERSION/opentelemetry-sdk-extension-autoconfigure-spi-$OTEL_VERSION.jar \
    -L \
    -o /opt/keycloak/providers/opentelemetry-sdk-extension-autoconfigure-spi-$OTEL_VERSION.jar

RUN curl https://repo1.maven.org/maven2/io/opentelemetry/opentelemetry-semconv/$OTEL_ALPHA_VERSION/opentelemetry-semconv-$OTEL_ALPHA_VERSION.jar \
    -L \
    -o /opt/keycloak/providers/opentelemetry-semconv-$OTEL_ALPHA_VERSION.jar

RUN curl https://repo1.maven.org/maven2/io/opentelemetry/instrumentation/opentelemetry-instrumentation-api/$OTEL_ALPHA_VERSION/opentelemetry-instrumentation-api-$OTEL_ALPHA_VERSION.jar \
    -L \
    -o /opt/keycloak/providers/opentelemetry-instrumentation-api-$OTEL_ALPHA_VERSION.jar

RUN curl https://repo1.maven.org/maven2/io/opentelemetry/instrumentation/opentelemetry-instrumentation-annotations/$OTEL_ALPHA_VERSION/opentelemetry-instrumentation-annotations-$OTEL_ALPHA_VERSION.jar \
    -L \
    -o /opt/keycloak/providers/opentelemetry-instrumentation-annotations-$OTEL_ALPHA_VERSION.jar

RUN curl https://repo1.maven.org/maven2/io/opentelemetry/instrumentation/opentelemetry-instrumentation-annotations-support/$OTEL_ALPHA_VERSION/opentelemetry-instrumentation-annotations-support-$OTEL_ALPHA_VERSION.jar \
    -L \
    -o /opt/keycloak/providers/opentelemetry-instrumentation-annotations-support-$OTEL_ALPHA_VERSION.jar

RUN curl https://repo1.maven.org/maven2/io/opentelemetry/instrumentation/opentelemetry-instrumentation-api-semconv/$OTEL_ALPHA_VERSION/opentelemetry-instrumentation-api-semconv-$OTEL_ALPHA_VERSION.jar \
    -L \
    -o /opt/keycloak/providers/opentelemetry-instrumentation-api-semconv-$OTEL_ALPHA_VERSION.jar

RUN curl https://repo1.maven.org/maven2/io/quarkus/quarkus-opentelemetry-deployment/$QUARKUS_VERSION/quarkus-opentelemetry-deployment-$QUARKUS_VERSION.jar \
    -L \
    -o /opt/keycloak/providers/quarkus-opentelemetry-deployment-$QUARKUS_VERSION.jar

RUN curl https://repo1.maven.org/maven2/io/quarkus/quarkus-opentelemetry-exporter-otlp/$QUARKUS_VERSION/quarkus-opentelemetry-exporter-otlp-$QUARKUS_VERSION.jar \
    -L \
    -o /opt/keycloak/providers/quarkus-opentelemetry-exporter-otlp-$QUARKUS_VERSION.jar

RUN curl https://repo1.maven.org/maven2/io/quarkus/quarkus-opentelemetry-exporter-otlp-deployment/$QUARKUS_VERSION/quarkus-opentelemetry-exporter-otlp-deployment-$QUARKUS_VERSION.jar \
    -L \
    -o /opt/keycloak/providers/quarkus-opentelemetry-exporter-otlp-deployment-$QUARKUS_VERSION.jar

RUN curl https://repo1.maven.org/maven2/io/opentelemetry/opentelemetry-exporter-otlp/$OTEL_VERSION/opentelemetry-exporter-otlp-$OTEL_VERSION.jar \
    -L \
    -o /opt/keycloak/providers/opentelemetry-exporter-otlp-$OTEL_VERSION.jar

RUN curl https://repo1.maven.org/maven2/io/opentelemetry/opentelemetry-exporter-otlp-common/$OTEL_VERSION/opentelemetry-exporter-otlp-common-$OTEL_VERSION.jar \
    -L \
    -o /opt/keycloak/providers/opentelemetry-exporter-otlp-common-$OTEL_VERSION.jar

RUN curl https://repo1.maven.org/maven2/io/opentelemetry/opentelemetry-exporter-common/$OTEL_VERSION/opentelemetry-exporter-common-$OTEL_VERSION.jar \
    -L \
    -o /opt/keycloak/providers/opentelemetry-exporter-common-$OTEL_VERSION.jar

RUN curl https://repo1.maven.org/maven2/io/opentelemetry/opentelemetry-extension-trace-propagators/$OTEL_VERSION/opentelemetry-extension-trace-propagators-$OTEL_VERSION.jar \
    -L \
    -o /opt/keycloak/providers/opentelemetry-extension-trace-propagators-$OTEL_VERSION.jar

USER keycloak
