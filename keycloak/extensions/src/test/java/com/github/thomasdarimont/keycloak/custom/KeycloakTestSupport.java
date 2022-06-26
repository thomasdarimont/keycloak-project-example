package com.github.thomasdarimont.keycloak.custom;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.SelinuxContext;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class KeycloakTestSupport {

    public static final String MASTER_REALM = "master";

    public static final String ADMIN_CLI = "admin-cli";

    public static final String CONTEXT_PATH = "/auth";

    public static KeycloakContainer createKeycloakContainer() {
        return createKeycloakContainer(null);
    }

    public static KeycloakContainer createKeycloakContainer(String realmImportFileName) {
        return createKeycloakContainer("quay.io/keycloak/keycloak:18.0.2", realmImportFileName);
    }

    public static KeycloakContainer createKeycloakContainer(String imageName, String realmImportFileName) {

        KeycloakContainer keycloakContainer;
        if (imageName != null) {
            keycloakContainer = new KeycloakContainer(imageName);
            keycloakContainer.addEnv("KC_FEATURES", "preview");
        } else {
            // building custom Keycloak docker image with additional libraries
            String customDockerFileName = "../docker/src/main/docker/keycloakx/Dockerfile.ci.plain";
            ImageFromDockerfile imageFromDockerfile = new ImageFromDockerfile();
            imageFromDockerfile.withDockerfile(Paths.get(customDockerFileName));
            keycloakContainer = new KeycloakContainer();
            keycloakContainer.setImage(imageFromDockerfile);
            keycloakContainer.withContextPath(CONTEXT_PATH);
        }

        return keycloakContainer.withProviderClassesFrom("target/classes");
    }

    public static ResteasyWebTarget getResteasyWebTarget(KeycloakContainer keycloak) {
        Client client = ResteasyClientBuilder.newBuilder().build();
        return (ResteasyWebTarget) client.target(UriBuilder.fromPath(keycloak.getAuthServerUrl()));
    }

    public static UserRef createOrUpdateTestUser(RealmResource realm, String username, String password, Consumer<UserRepresentation> adjuster) {

        List<UserRepresentation> existingUsers = realm.users().search(username, true);

        String userId;
        UserRepresentation userRep;

        if (existingUsers.isEmpty()) {
            userRep = new UserRepresentation();
            userRep.setUsername(username);
            userRep.setEnabled(true);
            adjuster.accept(userRep);
            try (Response response = realm.users().create(userRep)) {
                userId = CreatedResponseUtil.getCreatedId(response);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        } else {
            userRep = existingUsers.get(0);
            adjuster.accept(userRep);
            userId = userRep.getId();
        }

        CredentialRepresentation passwordRep = new CredentialRepresentation();
        passwordRep.setType(CredentialRepresentation.PASSWORD);
        passwordRep.setValue(password);
        realm.users().get(userId).resetPassword(passwordRep);

        return new UserRef(userId, username);
    }

    public static GenericContainer<?> createKeycloakConfigCliContainer(KeycloakContainer keycloakContainer) {

        GenericContainer<?> keycloakConfigCli = new GenericContainer<>("quay.io/adorsys/keycloak-config-cli:5.2.0-18.0.0");
        keycloakConfigCli.addEnv("KEYCLOAK_AVAILABILITYCHECK_ENABLED", "true");
        keycloakConfigCli.addEnv("KEYCLOAK_AVAILABILITYCHECK_TIMEOUT", "30s");
        keycloakConfigCli.addEnv("IMPORT_FILES_LOCATION", "/config/*");
        keycloakConfigCli.addEnv("IMPORT_CACHE_ENABLED", "true");
        keycloakConfigCli.addEnv("IMPORT_VAR_SUBSTITUTION_ENABLED", "true");
        keycloakConfigCli.addEnv("KEYCLOAK_USER", keycloakContainer.getAdminUsername());
        keycloakConfigCli.addEnv("KEYCLOAK_PASSWORD", keycloakContainer.getAdminPassword());
        keycloakConfigCli.addEnv("KEYCLOAK_URL", keycloakContainer.getAuthServerUrl());
        keycloakConfigCli.addEnv("KEYCLOAK_FRONTEND_URL", keycloakContainer.getAuthServerUrl());
        keycloakConfigCli.addEnv("APPS_FRONTEND_URL_MINISPA", "http://localhost:4000");
        keycloakConfigCli.addEnv("APPS_FRONTEND_URL_GREETME", "http://localhost:4000");
        keycloakConfigCli.addEnv("ACME_AZURE_AAD_TENANT_URL", "https://login.microsoftonline.com/dummy-azuread-tenant-id");

        // TODO make the realm config folder parameterizable
        keycloakConfigCli.addFileSystemBind("../../config/stage/dev/realms", "/config", BindMode.READ_ONLY, SelinuxContext.SHARED);
        keycloakConfigCli.setWaitStrategy(Wait.forLogMessage(".*keycloak-config-cli running in.*", 1));
        keycloakConfigCli.setNetworkMode("host");
        return keycloakConfigCli;
    }

    @Data
    @AllArgsConstructor
    public static class UserRef {
        String userId;
        String username;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class CustomKeycloak extends KeycloakContainer {

        String authServerUrl;
        String adminUsername;
        String adminPassword;

        public void start() {
            // NOOP
        }

        @Override
        public void followOutput(Consumer<OutputFrame> consumer) {
            // NOOP
        }
    }
}
