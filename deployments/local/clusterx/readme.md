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
docker compose --env-file ../../../keycloak.env --file nginx/docker-compose-nginx.yml up --remove-orphans --build
```

Browse to: https://id.acme.test:2443/auth

Stop:
```
docker compose --env-file ../../../keycloak.env --file nginx/docker-compose-nginx.yml down --remove-orphans
```

# Run Keycloak.X cluster behind HA-Proxy

Start:
```
docker compose --env-file ../../../keycloak.env --file haproxy/docker-compose-haproxy.yml up --remove-orphans --build
```

Browse to: https://id.acme.test:1443/auth

HA-Proxy status URL: https://id.acme.test:1443/haproxy?status


Stop:
```
docker compose --env-file ../../../keycloak.env --file haproxy/docker-compose-haproxy.yml down --remove-orphans
```

# Run Keycloak.X cluster with database backed user sessions

Start:
```
docker compose --env-file ../../../keycloak.env --file haproxy-database-ispn/docker-compose.yml up --remove-orphans --build
```

Browse to: https://id.acme.test:1443/auth

Stop:
```
docker compose --env-file ../../../keycloak.env --file haproxy-database-ispn/docker-compose.yml down --remove-orphans
```

# Run Keycloak.X cluster behind HA-Proxy with external Infinispan

Start:
```
docker compose --env-file ../../../keycloak.env --file haproxy-external-ispn/docker-compose-haproxy-ispn-remote.yml up --remove-orphans --build
```

Browse to: https://id.acme.test:1443/auth

Stop:
```
docker compose --env-file ../../../keycloak.env --file haproxy-external-ispn/docker-compose-haproxy-ispn-remote.yml down --remove-orphans
```

# Run Keycloak.X cluster behind HA-Proxy with external Infinispan and database persistence

Start:
```
docker compose --env-file ../../../keycloak.env --file haproxy-external-ispn-database/docker-compose-haproxy-ispn-remote-database.yml up --remove-orphans --build
```

Browse to: https://id.acme.test:1443/auth

Stop:
```
docker compose --env-file ../../../keycloak.env --file haproxy-external-ispn-database/docker-compose-haproxy-ispn-remote-database.yml down --remove-orphans
```