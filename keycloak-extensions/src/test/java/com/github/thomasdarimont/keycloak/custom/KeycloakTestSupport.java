package com.github.thomasdarimont.keycloak.custom;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.token.TokenService;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.utility.MountableFile;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class KeycloakTestSupport {

    public static final String MASTER_REALM = "master";

    public static final String ADMIN_CLI = "admin-cli";

    public static KeycloakContainer createKeycloakContainer(boolean keycloakLocal) {
        return createKeycloakContainer(keycloakLocal, null);
    }

    public static KeycloakContainer createKeycloakContainer(boolean keycloakLocal, String realmImportFileName) {

        if (keycloakLocal) {
            return new LocalKeycloak("http://localhost:8080/auth", "admin", "admin");
        }

        KeycloakContainer keycloakContainer = new KeycloakContainer("quay.io/keycloak/keycloak:12.0.4");

        if (realmImportFileName != null) {
            addRealmImportFile(realmImportFileName, keycloakContainer);
        }
        addStartupCliFilesIfPresent(keycloakContainer);

        return keycloakContainer.withExtensionClassesFrom("target/classes");
    }

    private static void addRealmImportFile(String realmImportFileName, KeycloakContainer keycloakContainer) {
        keycloakContainer.withEnv("KEYCLOAK_IMPORT", "/tmp/" + realmImportFileName);
        keycloakContainer.withCopyFileToContainer(MountableFile.forHostPath(Path.of("../imex/" + realmImportFileName)), "/tmp/" + realmImportFileName);
    }

    private static void addStartupCliFilesIfPresent(KeycloakContainer keycloakContainer) {
        try {
            Files.list(Path.of("../cli")).forEach(cliFilePath -> {
                File cliFile = cliFilePath.toFile();
                if (!cliFile.getName().endsWith(".cli")) {
                    return;
                }
                String sourcePath = cliFile.getAbsolutePath();
                String containerPath = "/opt/jboss/startup-scripts/" + cliFile.getName();
                log.info("Copying {} into container {}", sourcePath, containerPath);
                keycloakContainer.withCopyFileToContainer(MountableFile.forHostPath(cliFilePath), containerPath);
            });
        } catch (IOException e) {
            log.error("Failed to list init-cli files.", e);
        }
    }

    public static TokenService getTokenService(KeycloakContainer keycloak) {
        return getResteasyWebTarget(keycloak).proxy(TokenService.class);
    }

    public static ResteasyWebTarget getResteasyWebTarget(KeycloakContainer keycloak) {
        ResteasyClient client = new ResteasyClientBuilder().build();
        return client.target(UriBuilder.fromPath(keycloak.getAuthServerUrl()));
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

    @Data
    @AllArgsConstructor
    public static class UserRef {
        String userId;
        String username;
    }


    @Data
    @AllArgsConstructor
    public static class LocalKeycloak extends KeycloakContainer {

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
