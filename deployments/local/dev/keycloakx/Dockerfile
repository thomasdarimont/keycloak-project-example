FROM quay.io/keycloak/keycloak-x:15.0.2
#FROM thomasdarimont/keycloak-x:16.0.0-SNAPSHOT

# TODO Add smallrye metrics support

USER 0

## Workaround for adding the current certifcate to the cacerts truststore
# Import certificate into cacerts truststore
COPY --chown=jboss:jboss "./acme.test+1.pem" "/etc/x509/tls.crt.pem"
RUN keytool -import -cacerts -noprompt -file /etc/x509/tls.crt.pem -storepass changeit

USER jboss
