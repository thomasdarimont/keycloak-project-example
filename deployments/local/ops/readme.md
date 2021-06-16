Operations
-----
This shows how to work as operator with the current start.java --http setup

#Monitoring and logging
Monitoring and logging with [Grafana](#grafana).

Background: 
* Scraping metrics by [Prometheus](#prometheus).
* Log aggregation with [Promtail](#promtail) und [Loki](#loki).

Https is not tested so far.

## Prometheus
Keycloak does not protect [Prometheus](https://prometheus.io/).
Open [Prometheus](http://localhost:9090)

Scrape targets:

|System|Target  |Additional Labels
|------|--------|------
|keycloak |http://acme-keycloak:9990/metrics | env

## Grafana
Keycloak protects [Grafana](https://grafana.com/grafana/). Open [Grafana](http://localhost:3000)

Properties:
* No login as guest, only when correct role is assigned in keycloak
* Manual steps when logged in as Admin (User: admina, Password: test)
  * Add prometheus as datasource (http://acme-prometheus:9090/)
  * Import Boards of your choice from [Grafana](https://grafana.com/grafana/dashboards) (for testing an [exported board](../../../config/stage/ops/grafana/microprofile-wildfly-16-metrics_rev1.json) can be used)
  * Add loki as a datasource (http://acme-loki:3100)
  * Try loki via explore and search with query string: `{filename="/var/log/server-rotating.log"} |~ "LOGIN"`

## Loki
[Loki by grafana](https://grafana.com/docs/loki/latest/overview/) is not protected by keyclaok. Collects log and makes them searchable.

Properties:
* Able to collect logs from various sources
* Receives the logs from [Promtail](#promtail)

## Promtail
[Promtail](https://grafana.com/docs/loki/latest/clients/promtail/) is not protected by keycloak.
Prometheus style log collection from file.

Properties:
* Push logs to [Loki](#loki)
* Takes files from log file directory

# Run
```
docker-compose up
```
