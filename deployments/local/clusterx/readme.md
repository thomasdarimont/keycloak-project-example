Keycloak.X Clustering Examples
----

# Prepare

Copy `../../../config/stage/dev/tls/*.pem` to `{./haproxy ./keycloakx ./nginx}`.

```
cp ../../../config/stage/dev/tls/*.pem ./haproxy 
cp ../../../config/stage/dev/tls/*.pem ./keycloakx
cp ../../../config/stage/dev/tls/*.pem ./nginx
```

# Run Keycloak.X cluster behind Nginx
Start:
```
docker-compose --env-file ../../../keycloak.env --file nginx/docker-compose-nginx.yml up --remove-orphans --build
```

Browse to: https://id.acme.test:2443/auth

Stop:
```
docker-compose --env-file ../../../keycloak.env --file nginx/docker-compose-nginx.yml down --remove-orphans
```

# Run Keycloak.X cluster behind HA-Proxy

Start:
```
docker-compose --env-file ../../../keycloak.env --file haproxy/docker-compose-haproxy.yml up --remove-orphans --build
```

Browse to: https://id.acme.test:1443/auth

Stop:
```
docker-compose --env-file ../../../keycloak.env --file haproxy/docker-compose-haproxy.yml down --remove-orphans
```

# Run Keycloak.X cluster behind HA-Proxy with external Infinispan

Start:
```
ocker-compose --env-file ../../../keycloak.env --file haproxy-external-ispn/docker-compose-haproxy-ispn-remote.yml up --remove-orphans --build
```

Browse to: https://id.acme.test:1443/auth

Stop:
```
ocker-compose --env-file ../../../keycloak.env --file haproxy-external-ispn/docker-compose-haproxy-ispn-remote.yml down --remove-orphans
```