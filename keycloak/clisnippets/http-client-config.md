HTTP Client Configuration
----

# HTTP Client Definition 
```
#### Keycloak HTTP-Client
echo SETUP: Configure HTTP client for outgoing requests
# General HTTP Connection properties
/subsystem=keycloak-server/spi=connectionsHttpClient/provider=default:write-attribute(name=properties.connection-pool-size, value=128)
```

# Proxy configuration
```
# Configure proxy routes for HttpClient SPI
echo SETUP: Configure HTTP client with Proxy
/subsystem=keycloak-server/spi=connectionsHttpClient/provider=default:write-attribute(name=properties.proxy-mappings,value=[".*\\.(acme)\\.de;NO_PROXY",".*;http://www-proxy.acme.com:3128"])
```


# Client Certificate configuration

```
# Configure client certificate for MTLS for HttpClient SPI
echo SETUP: Configure HTTP client with client-certificate
/subsystem=keycloak-server/spi=connectionsHttpClient/provider=default:write-attribute(name=properties.client-keystore,value="")
/subsystem=keycloak-server/spi=connectionsHttpClient/provider=default:write-attribute(name=properties.client-keystore-password,value="changeit")
/subsystem=keycloak-server/spi=connectionsHttpClient/provider=default:write-attribute(name=properties.client-key-password,value="")
/subsystem=keycloak-server/spi=connectionsHttpClient/provider=default:write-attribute(name=properties.disable-trust-manager,value="true")
```