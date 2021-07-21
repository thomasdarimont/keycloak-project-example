Undertow Request Logging
---

See: https://mirocupak.com/logging-requests-with-undertow/

Dump Requests for Debugging (Very verbose!!!)
```
batch
/subsystem=undertow/configuration=filter/custom-filter=request-logging-filter:add(class-name=io.undertow.server.handlers.RequestDumpingHandler, module=io.undertow.core)
/subsystem=undertow/server=default-server/host=default-host/filter-ref=request-logging-filter:add
run-batch
```

Apache style access log
See: https://undertow.io/undertow-docs/undertow-docs-2.1.0/index.html#access-log-handler
See: https://undertow.io/undertow-docs/undertow-docs-2.1.0/index.html#exchange-attributes-2
```
/subsystem=undertow/server=default-server/host=default-host/setting=access-log:\
add(pattern="%h %t \"%r\" %s \"%{i,User-Agent}\"", use-server-log=true)
```