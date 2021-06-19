Keycloak Clustering Examples
----

# Cluster with haproxy Load-Balancer 

## Prepare

Copy the `acme.test*.pem` files from the `config/stage/dev/tls` into the [haproxy](haproxy) directory.
```
cp ../../../config/stage/dev/tls/*.pem haproxy/
```


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

# Cluster with Envoy Load-Balancer

## Run
```
docker-compose --env-file ../../../keycloak.env --file envoy/docker-compose-envoy.yml up --remove-orphans
```

Envoy Keycloak URL: https://id.acme.test:4443/auth
