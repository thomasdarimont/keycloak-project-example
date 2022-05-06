Custom Keycloak Server
----

# Build
```
mvn clean verify -DskipTests
```

# Run
```
target/keycloak-18.0.0/bin/kc.sh \
  start-dev \
  --db postgres \
  --db-url-host localhost \
  --db-username keycloak \
  --db-password keycloak \
  --http-port=8080 \
  --http-relative-path=auth \
  --spi-events-listener-jboss-logging-success-level=info \
  --spi-events-listener-jboss-logging-error-level=warn  \
  --https-certificate-file=../../../config/stage/dev/tls/acme.test+1.pem \
  --https-certificate-key-file=../../../config/stage/dev/tls/acme.test+1-key.pem
```