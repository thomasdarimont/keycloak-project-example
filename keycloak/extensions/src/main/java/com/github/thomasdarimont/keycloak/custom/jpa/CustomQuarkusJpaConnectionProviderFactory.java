package com.github.thomasdarimont.keycloak.custom.jpa;

import com.google.auto.service.AutoService;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.connections.jpa.JpaConnectionProviderFactory;
import org.keycloak.quarkus.runtime.storage.database.jpa.QuarkusJpaConnectionProviderFactory;

import java.util.HashMap;
import java.util.Map;

@JBossLog
@AutoService(JpaConnectionProviderFactory.class)
public class CustomQuarkusJpaConnectionProviderFactory extends QuarkusJpaConnectionProviderFactory {

    private static final Map<String, String> TUNING_PROPERTIES;

    static {
        log.info("### Using custom Quarkus JPA Connection factory");

        Map<String, String> props = new HashMap<>();
//        props.put("hibernate.generate_statistics", "true");

        if (!props.isEmpty()) {
            log.infof("### Apply additional hibernate tuning properties: %s", props);
        }

        TUNING_PROPERTIES = props;
    }

    @Override
    protected EntityManagerFactory getEntityManagerFactory() {

        EntityManagerFactory emf = super.getEntityManagerFactory();
        if (TUNING_PROPERTIES.isEmpty()) {
            emf.getProperties().putAll(TUNING_PROPERTIES);
        }

        return emf;
    }
}
