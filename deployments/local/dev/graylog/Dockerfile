FROM graylog/graylog:4.1

COPY --chown=1100:0 "./acme.test+1.pem" /usr/share/graylog/data/config/ssl/cert.crt
COPY --chown=1100:0 "./acme.test+1-key.pem" /usr/share/graylog/data/config/ssl/key.key

USER 0

RUN echo "Import Acme cert into truststore" && \
    keytool \
    -import \
    -noprompt \
    -keystore /usr/local/openjdk-8/lib/security/cacerts \
    -storetype JKS \
    -storepass changeit \
    -alias acmecert \
    -file /usr/share/graylog/data/config/ssl/cert.crt

USER 1100