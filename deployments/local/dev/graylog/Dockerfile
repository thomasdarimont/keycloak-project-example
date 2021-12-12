FROM graylog/graylog:4.2.3-1-jre11@sha256:0f277b217c988cd4a0ce6f536271edde61e8b610ede0a96c9a214cbf0f86b4bf

COPY --chown=1100:0 "./acme.test+1.pem" /usr/share/graylog/data/config/ssl/cert.crt
COPY --chown=1100:0 "./acme.test+1-key.pem" /usr/share/graylog/data/config/ssl/key.key

USER 0

RUN echo "Import Acme cert into truststore" && \
    keytool \
    -import \
    -noprompt \
    -keystore /usr/local/openjdk-11/lib/security/cacerts \
    -storetype JKS \
    -storepass changeit \
    -alias acmecert \
    -file /usr/share/graylog/data/config/ssl/cert.crt

USER 1100