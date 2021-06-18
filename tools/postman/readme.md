# Work with Postman(tm)

1. Install/Open [Postman](https://www.postman.com/)
2. Import folder tools/postman or individual files, e.g. from `Scratch Pad` 
   1. Import [globals](acme.postman_globals.json)
   2. Import [http-environment for ](acme.postman_environment_http.json)
   3. Import [https-environment for https](acme.postman_environment_https.json)
   4. Import [collection](acme.postman_collection.json) 
3. Apply demo configuration to the keycloak env of choice
   1. ```
      java bin/keycloakConfigCli --import=config/stage/demo/acme-demo.yaml --keycloak-url=http://localhost:8080/auth
      ```
4. From the imported collection named `Acme Keycloak` run `UPDATE GLOBAL VARIABLE USER ID` to work with the id based endpoints. 
   