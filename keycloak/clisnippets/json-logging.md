JSON Logging
----

```
### Logging Configuration ###

# echo SETUP: Adjust Logging configuration
# See: https://wildscribe.github.io/WildFly/13.0/subsystem/logging/json-formatter/index.html
# See: https://github.com/wildfly/wildfly/blob/master/docs/src/main/asciidoc/_admin-guide/subsystem-configuration/Logging_Formatters.adoc#json-formatter
# supported properties: [date-format, exception-output-type, key-overrides, meta-data, pretty-print, print-details, record-delimiter, zone-id]

echo SETUP: Enable JSON Logging
# /subsystem=logging/json-formatter=JSON-PATTERN:add(exception-output-type=formatted, key-overrides={timestamp=@timestamp,logger-name=logger_name,stack-trace=stack_trace,level=level_name}, meta-data={app=keycloak})
# /subsystem=logging/console-handler=CONSOLE:write-attribute(name=named-formatter,value=JSON-PATTERN)
```