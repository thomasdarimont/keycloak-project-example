FROM quay.io/keycloak/keycloak-x:16.0.0
#FROM thomasdarimont/keycloak-x:16.0.0-SNAPSHOT

# TODO Add smallrye metrics support

USER 0

# Add simple custom JGroups configuration
COPY --chown=keycloak:keycloak ./cache-custom.xml /opt/keycloak/conf/cache-custom.xml

# Add enhanced custom JGroups configuration with encryption support
COPY --chown=keycloak:keycloak ./cache-custom-jgroups.xml /opt/keycloak/conf/cache-custom-jgroups.xml
COPY --chown=keycloak:keycloak ./jgroups-multicast-enc.xml /opt/keycloak/conf/jgroups-multicast-enc.xml
COPY --chown=keycloak:keycloak ./jgroups-multicast-diag.xml /opt/keycloak/conf/jgroups-multicast-diag.xml
COPY --chown=keycloak:keycloak ./jgroups.p12 /opt/keycloak/conf/jgroups.p12

## Workaround for adding the current certifcate to the cacerts truststore
# Import certificate into cacerts truststore
COPY --chown=keycloak:keycloak "./acme.test+1.pem" "/etc/x509/tls.crt.pem"
RUN keytool -import -cacerts -noprompt -file /etc/x509/tls.crt.pem -storepass changeit

USER keycloak

ENTRYPOINT [ "/opt/keycloak/bin/kc.sh" ]