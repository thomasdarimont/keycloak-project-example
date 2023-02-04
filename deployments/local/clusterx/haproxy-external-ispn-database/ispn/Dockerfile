FROM quay.io/infinispan/server:13.0.15.Final-1

USER 0

RUN true \
  && microdnf clean all \
  && microdnf install shadow-utils \
  && microdnf update --nodocs \
  && adduser ispn \
  && microdnf remove shadow-utils \
  && microdnf clean all

RUN chown -R ispn:0 /opt/infinispan

RUN curl https://jdbc.postgresql.org/download/postgresql-42.5.2.jar --output /opt/infinispan/lib/postgresql-42.5.2.jar

USER ispn

CMD [ "-c", "infinispan-keycloak.xml" ]
ENTRYPOINT [ "/opt/infinispan/bin/server.sh" ]