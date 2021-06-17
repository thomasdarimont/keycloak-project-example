# Work with Postman(tm)

1. Install/Open [Postman](https://www.postman.com/)
2. Import [environment](keycloak-project-example.postman_environment.json)
3. Import [collection](keycloak_endpoints.postman_collection.json) 

Depending on the started keycloak environment the variables might need to be adjusted.

1. Keycloak server URL
2. Keycloak realm to use
    1. Be aware of the setting loginname as username
    
The collection contains calls to create resources like clients and users to work with the collection.

1. Create a user with POST Create user
    1. Read the user with GET User by id
2. Create a client with POST Create client
    1. Read the client-credentials with GET Client credentials
    
When using the Standard flows via OAuth2/OIDC make sure you lock out when the multistep flows do not work

To be continued...