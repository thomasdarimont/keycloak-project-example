package com.github.thomasdarimont.keycloak.custom;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.token.TokenService;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

@Slf4j
public class KeycloakEnvironment {

    public KeycloakContainer keycloak;

    public GenericContainer<?> keycloakConfigCli;

    private String authServerUrl = "http://localhost:8080/auth";

    private String adminUsername = "admin";

    private String adminPassword = "admin";

    private Mode mode = Mode.TESTCONTAINERS;

    private boolean runConfigCli = true;

    public KeycloakEnvironment local() {
        KeycloakEnvironment keycloakEnvironment = new KeycloakEnvironment();
        keycloakEnvironment.setMode(Mode.LOCAL);
        keycloakEnvironment.setRunConfigCli(false);
        return keycloakEnvironment;
    }

    public KeycloakEnvironment custom(String authServerUrl, String adminUsername, String adminPassword) {
        KeycloakEnvironment keycloakEnvironment = new KeycloakEnvironment();
        keycloakEnvironment.setAuthServerUrl(authServerUrl);
        keycloakEnvironment.setAdminUsername(adminUsername);
        keycloakEnvironment.setAdminPassword(adminPassword);
        keycloakEnvironment.setMode(Mode.CUSTOM);
        keycloakEnvironment.setRunConfigCli(false);
        return keycloakEnvironment;
    }

    public void start() {

        switch (mode) {
            case LOCAL:
            case CUSTOM: {
                keycloak = new KeycloakTestSupport.CustomKeycloak(authServerUrl, adminUsername, adminPassword);
                return;
            }
            case TESTCONTAINERS:
            default:
                break;
        }

        keycloak = KeycloakTestSupport.createKeycloakContainer();
        keycloak.withReuse(true);
        keycloak.start();
        keycloak.followOutput(new Slf4jLogConsumer(log));

        if (runConfigCli) {
            keycloakConfigCli = KeycloakTestSupport.createKeycloakConfigCliContainer(keycloak);
            keycloakConfigCli.start();
            keycloakConfigCli.followOutput(new Slf4jLogConsumer(log));
        }
    }

    public void stop() {
        if (keycloak != null) {
            keycloak.stop();
        }

        if (keycloakConfigCli != null) {
            keycloakConfigCli.stop();
        }
    }

    public KeycloakContainer getKeycloak() {
        return keycloak;
    }

    public GenericContainer<?> getKeycloakConfigCli() {
        return keycloakConfigCli;
    }

    public Keycloak getAdminClient() {
        return Keycloak.getInstance(keycloak.getAuthServerUrl(), KeycloakTestSupport.MASTER_REALM,
                keycloak.getAdminUsername(), keycloak.getAdminPassword(), KeycloakTestSupport.ADMIN_CLI);
    }

    public TokenService getTokenService() {
        return getClientProxy(TokenService.class);
    }

    public <T> T getClientProxy(Class<T> iface) {
        return iface.cast(KeycloakTestSupport.getResteasyWebTarget(keycloak).proxy(iface));
    }

    public String getAuthServerUrl() {
        return authServerUrl;
    }

    public KeycloakEnvironment setAuthServerUrl(String authServerUrl) {
        this.authServerUrl = authServerUrl;
        return this;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public KeycloakEnvironment setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
        return this;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public KeycloakEnvironment setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
        return this;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public boolean isRunConfigCli() {
        return runConfigCli;
    }

    public void setRunConfigCli(boolean runConfigCli) {
        this.runConfigCli = runConfigCli;
    }

    enum Mode {
        CUSTOM, LOCAL, TESTCONTAINERS
    }
}
