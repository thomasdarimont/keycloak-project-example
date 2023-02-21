#!/usr/bin/env bash

MAVEN_REPO=https://repo1.maven.org/maven2
KEYCLOAK_PROVIDERS=/opt/keycloak/providers

curl $MAVEN_REPO/io/quarkus/quarkus-opentelemetry/$QUARKUS_VERSION/quarkus-opentelemetry-$QUARKUS_VERSION.jar \
    -L \
    -o $KEYCLOAK_PROVIDERS/quarkus-opentelemetry-$QUARKUS_VERSION.jar

curl $MAVEN_REPO/io/opentelemetry/opentelemetry-sdk/$OTEL_VERSION/opentelemetry-sdk-$OTEL_VERSION.jar \
    -L \
    -o $KEYCLOAK_PROVIDERS/opentelemetry-sdk-$OTEL_VERSION.jar

curl $MAVEN_REPO/io/opentelemetry/opentelemetry-api/$OTEL_VERSION/opentelemetry-api-$OTEL_VERSION.jar \
    -L \
    -o $KEYCLOAK_PROVIDERS/opentelemetry-api-$OTEL_VERSION.jar

curl $MAVEN_REPO/io/opentelemetry/opentelemetry-context/$OTEL_VERSION/opentelemetry-context-$OTEL_VERSION.jar \
    -L \
    -o $KEYCLOAK_PROVIDERS/opentelemetry-context-$OTEL_VERSION.jar

curl $MAVEN_REPO/io/opentelemetry/opentelemetry-sdk-common/$OTEL_VERSION/opentelemetry-sdk-common-$OTEL_VERSION.jar \
    -L \
    -o $KEYCLOAK_PROVIDERS/opentelemetry-sdk-common-$OTEL_VERSION.jar

curl $MAVEN_REPO/io/opentelemetry/opentelemetry-sdk-trace/$OTEL_VERSION/opentelemetry-sdk-trace-$OTEL_VERSION.jar \
    -L \
    -o $KEYCLOAK_PROVIDERS/opentelemetry-sdk-trace-$OTEL_VERSION.jar

curl $MAVEN_REPO/io/opentelemetry/opentelemetry-sdk-metrics/$OTEL_VERSION/opentelemetry-sdk-metrics-$OTEL_VERSION.jar \
    -L \
    -o $KEYCLOAK_PROVIDERS/opentelemetry-sdk-metrics-$OTEL_VERSION.jar

curl $MAVEN_REPO/io/opentelemetry/opentelemetry-sdk-logs/$OTEL_ALPHA_VERSION/opentelemetry-sdk-logs-$OTEL_ALPHA_VERSION.jar \
    -L \
    -o $KEYCLOAK_PROVIDERS/opentelemetry-sdk-logs-$OTEL_ALPHA_VERSION.jar

curl $MAVEN_REPO/io/opentelemetry/opentelemetry-extension-annotations/$OTEL_VERSION/opentelemetry-extension-annotations-$OTEL_VERSION.jar \
    -L \
    -o $KEYCLOAK_PROVIDERS/opentelemetry-extension-annotations-$OTEL_VERSION.jar

curl $MAVEN_REPO/io/opentelemetry/opentelemetry-sdk-extension-autoconfigure-spi/$OTEL_VERSION/opentelemetry-sdk-extension-autoconfigure-spi-$OTEL_VERSION.jar \
    -L \
    -o $KEYCLOAK_PROVIDERS/opentelemetry-sdk-extension-autoconfigure-spi-$OTEL_VERSION.jar

curl $MAVEN_REPO/io/opentelemetry/opentelemetry-semconv/$OTEL_ALPHA_VERSION/opentelemetry-semconv-$OTEL_ALPHA_VERSION.jar \
    -L \
    -o $KEYCLOAK_PROVIDERS/opentelemetry-semconv-$OTEL_ALPHA_VERSION.jar

curl $MAVEN_REPO/io/opentelemetry/instrumentation/opentelemetry-instrumentation-api/$OTEL_ALPHA_VERSION/opentelemetry-instrumentation-api-$OTEL_ALPHA_VERSION.jar \
    -L \
    -o $KEYCLOAK_PROVIDERS/opentelemetry-instrumentation-api-$OTEL_ALPHA_VERSION.jar

curl $MAVEN_REPO/io/opentelemetry/instrumentation/opentelemetry-instrumentation-annotations/$OTEL_ALPHA_VERSION/opentelemetry-instrumentation-annotations-$OTEL_ALPHA_VERSION.jar \
    -L \
    -o $KEYCLOAK_PROVIDERS/opentelemetry-instrumentation-annotations-$OTEL_ALPHA_VERSION.jar

curl $MAVEN_REPO/io/opentelemetry/instrumentation/opentelemetry-instrumentation-annotations-support/$OTEL_ALPHA_VERSION/opentelemetry-instrumentation-annotations-support-$OTEL_ALPHA_VERSION.jar \
    -L \
    -o $KEYCLOAK_PROVIDERS/opentelemetry-instrumentation-annotations-support-$OTEL_ALPHA_VERSION.jar

curl $MAVEN_REPO/io/opentelemetry/instrumentation/opentelemetry-instrumentation-api-semconv/$OTEL_ALPHA_VERSION/opentelemetry-instrumentation-api-semconv-$OTEL_ALPHA_VERSION.jar \
    -L \
    -o $KEYCLOAK_PROVIDERS/opentelemetry-instrumentation-api-semconv-$OTEL_ALPHA_VERSION.jar

curl $MAVEN_REPO/io/quarkus/quarkus-opentelemetry-deployment/$QUARKUS_VERSION/quarkus-opentelemetry-deployment-$QUARKUS_VERSION.jar \
    -L \
    -o $KEYCLOAK_PROVIDERS/quarkus-opentelemetry-deployment-$QUARKUS_VERSION.jar

curl $MAVEN_REPO/io/quarkus/quarkus-opentelemetry-exporter-otlp/$QUARKUS_VERSION/quarkus-opentelemetry-exporter-otlp-$QUARKUS_VERSION.jar \
    -L \
    -o $KEYCLOAK_PROVIDERS/quarkus-opentelemetry-exporter-otlp-$QUARKUS_VERSION.jar

curl $MAVEN_REPO/io/quarkus/quarkus-opentelemetry-exporter-otlp-deployment/$QUARKUS_VERSION/quarkus-opentelemetry-exporter-otlp-deployment-$QUARKUS_VERSION.jar \
    -L \
    -o $KEYCLOAK_PROVIDERS/quarkus-opentelemetry-exporter-otlp-deployment-$QUARKUS_VERSION.jar

curl $MAVEN_REPO/io/opentelemetry/opentelemetry-exporter-otlp/$OTEL_VERSION/opentelemetry-exporter-otlp-$OTEL_VERSION.jar \
    -L \
    -o $KEYCLOAK_PROVIDERS/opentelemetry-exporter-otlp-$OTEL_VERSION.jar

curl $MAVEN_REPO/io/opentelemetry/opentelemetry-exporter-otlp-common/$OTEL_VERSION/opentelemetry-exporter-otlp-common-$OTEL_VERSION.jar \
    -L \
    -o $KEYCLOAK_PROVIDERS/opentelemetry-exporter-otlp-common-$OTEL_VERSION.jar

curl $MAVEN_REPO/io/opentelemetry/opentelemetry-exporter-common/$OTEL_VERSION/opentelemetry-exporter-common-$OTEL_VERSION.jar \
    -L \
    -o $KEYCLOAK_PROVIDERS/opentelemetry-exporter-common-$OTEL_VERSION.jar

curl $MAVEN_REPO/io/opentelemetry/opentelemetry-extension-trace-propagators/$OTEL_VERSION/opentelemetry-extension-trace-propagators-$OTEL_VERSION.jar \
    -L \
    -o $KEYCLOAK_PROVIDERS/opentelemetry-extension-trace-propagators-$OTEL_VERSION.jar