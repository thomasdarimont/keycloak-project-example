Keycloak.X Clustering Examples
----


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