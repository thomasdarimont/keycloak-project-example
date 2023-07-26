ARG OTEL_VERSION=0.81.0
FROM otel/opentelemetry-collector:$OTEL_VERSION

USER 0

COPY --chown=10001:0 "./acme.test+1-key.pem" /key.pem
COPY --chown=10001:0 "./acme.test+1.pem" /cert.pem

USER 10001
