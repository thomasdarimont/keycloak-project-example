Keycloak.X CLI Examples
----

# Run Keycloak.X with HTTPS
```
bin/kc.sh \
  --verbose \
  start \
  --auto-build \
  --http-enabled=true \
  --http-relative-path=/auth \
  --hostname=id.acme.test:8443 \
  --https-certificate-file=/home/tom/dev/repos/gh/thomasdarimont/keycloak-dev/keycloak-project-template/config/stage/dev/tls/acme.test+1.pem \
  --https-certificate-key-file=/home/tom/dev/repos/gh/thomasdarimont/keycloak-dev/keycloak-project-template/config/stage/dev/tls/acme.test+1-key.pem \
  --https-protocols=TLSv1.3,TLSv1.2 \
  --proxy=passthrough \
  --metrics-enabled=false \
  --cache=local
```

--https-trust-store-file=/path/to/file
--https.trust-store.password=<value>