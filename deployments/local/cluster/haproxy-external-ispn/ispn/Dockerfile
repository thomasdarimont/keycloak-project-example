FROM quay.io/infinispan/server:11.0.9.Final-1

USER 0

RUN true \
  && microdnf clean all \
  && microdnf install shadow-utils \
  && microdnf update --nodocs \
  && adduser ispn \
  && microdnf remove shadow-utils \
  && microdnf clean all

RUN chown -R ispn:0 /opt/infinispan

USER ispn

CMD [ "-c", "infinispan-keycloak.xml" ]
ENTRYPOINT [ "/opt/infinispan/bin/server.sh" ]