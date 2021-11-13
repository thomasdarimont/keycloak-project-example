Keycloak.X Clustering Examples
----


Run Keycloak clsuter Nginx
```
docker-compose --env-file ../../../keycloak.env --file nginx/docker-compose-nginx.yml up --remove-orphans --build
```


HA-Proxy
```
docker-compose --env-file ../../../keycloak.env --file haproxy/docker-compose-haproxy.yml up --remove-orphans --build
```