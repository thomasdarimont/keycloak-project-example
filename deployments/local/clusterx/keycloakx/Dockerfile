FROM quay.io/keycloak/keycloak-x:15.0.2
#FROM thomasdarimont/keycloak-x:16.0.0-SNAPSHOT

# TODO Add smallrye metrics support

USER 0

# Add simple custom JGroups configuration
COPY --chown=jboss:jboss ./cluster-custom.xml /opt/jboss/keycloak/conf/cluster-custom.xml

# Add enhanced custom JGroups configuration with encryption support
COPY --chown=jboss:jboss ./cluster-custom-jgroups.xml /opt/jboss/keycloak/conf/cluster-custom-jgroups.xml
COPY --chown=jboss:jboss ./jgroups-multicast-enc.xml /opt/jboss/keycloak/conf/jgroups-multicast-enc.xml
COPY --chown=jboss:jboss ./jgroups-multicast-diag.xml /opt/jboss/keycloak/conf/jgroups-multicast-diag.xml
COPY --chown=jboss:jboss ./jgroups.p12 /opt/jboss/keycloak/conf/jgroups.p12

## Workaround for adding the current certifcate to the cacerts truststore
# Import certificate into cacerts truststore
COPY --chown=jboss:jboss "./acme.test+1.pem" "/etc/x509/tls.crt.pem"
RUN keytool -import -cacerts -noprompt -file /etc/x509/tls.crt.pem -storepass changeit

USER jboss

ENTRYPOINT [ "/opt/jboss/tools/docker-entrypoint.sh" ]