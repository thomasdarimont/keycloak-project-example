Keycloak Clustering Examples
----

# Cluster with haproxy Load-Balancer 

## Prepare

Copy the `acme.test*.pem` files from the `config/stage/dev/tls` into the [haproxy](haproxy) directory.

## Run 
```
docker-compose --file haproxy/docker-compose-haproxy.yml up --remove-orphans
```

haproxy status URL: https://id.acme.test:1443/haproxy?status

HAProxy Keycloak URL: https://id.acme.test:1443/auth

# Cluster with nginx Load-Balancer

## Run
```
docker-compose --file nginx/docker-compose-nginx.yml up --remove-orphans
```

Nginx Keycloak URL: https://id.acme.test:2443/auth

# Cluster with Apache mod_proxy Load-Balancer

## Run
```
docker-compose --file apache/docker-compose-apache.yml up --remove-orphans
```

Nginx Keycloak URL: https://id.acme.test:3443/auth