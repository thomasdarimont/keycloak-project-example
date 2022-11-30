package com.github.thomasdarimont.keycloak.custom.password;

import com.google.auto.service.AutoService;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.requiredactions.UpdatePassword;
import org.keycloak.models.KeycloakSessionFactory;

@JBossLog
@AutoService(RequiredActionFactory.class)
public class AcmeUpdatePassword extends UpdatePassword {

    private boolean alwaysForceReauth;

    @Override
    public int getMaxAuthAge() {

        if (alwaysForceReauth) {
            // force user to reauthenticate when changing passwords
            return 0;
        }

        return super.getMaxAuthAge();
    }

    @Override
    public void init(Config.Scope config) {
        super.init(config);

        // --spi-required-action-update-password-always-force-reauth=true
        this.alwaysForceReauth = config.getBoolean("always-force-reauth", false);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        super.postInit(factory);

        log.info("Overriding default UpdatePassword action with custom logic.");
    }
}
