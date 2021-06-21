Keycloak Clustering Examples
----

# Cluster with haproxy Load-Balancer 

## Prepare

Copy the `acme.test*.pem` files from the `config/stage/dev/tls` into the [haproxy](haproxy) directory.

## Run 
```
docker-compose --env-file ../../../keycloak.env --file haproxy/docker-compose-haproxy.yml up --remove-orphans
```

haproxy status URL: https://id.acme.test:1443/haproxy?status

HAProxy Keycloak URL: https://id.acme.test:1443/auth

# Cluster with nginx Load-Balancer

## Run
```
docker-compose --env-file ../../../keycloak.env --file nginx/docker-compose-nginx.yml up --remove-orphans
```

Nginx Keycloak URL: https://id.acme.test:2443/auth

# Cluster with Apache mod_proxy Load-Balancer

## Run
```
docker-compose --env-file ../../../keycloak.env --file apache/docker-compose-apache.yml up --remove-orphans
```

Apache Keycloak URL: https://id.acme.test:3443/auth

# Cluster with Caddy Load-Balancer

## Run
```
docker-compose --env-file ../../../keycloak.env --file caddy/docker-compose-caddy.yml up --remove-orphans
```

Caddy Keycloak URL: https://id.acme.test:5443/auth
