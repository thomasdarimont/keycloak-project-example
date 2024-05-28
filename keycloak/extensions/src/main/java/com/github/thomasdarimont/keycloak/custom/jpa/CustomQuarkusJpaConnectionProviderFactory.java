package com.github.thomasdarimont.keycloak.custom.jpa;

import com.google.auto.service.AutoService;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.connections.jpa.JpaConnectionProviderFactory;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.quarkus.runtime.storage.legacy.database.QuarkusJpaConnectionProviderFactory;

import java.util.HashMap;
import java.util.Map;

@JBossLog
@AutoService(JpaConnectionProviderFactory.class)
public class CustomQuarkusJpaConnectionProviderFactory extends QuarkusJpaConnectionProviderFactory {

    @Override
    protected EntityManagerFactory getEntityManagerFactory() {
        // apply tuning suggestions from https://github.com/keycloak/keycloak/issues/26162

        Map<String, String> tuningProperties = new HashMap<>();
        tuningProperties.put("hibernate.jdbc.batch_size", "50");
        tuningProperties.put("hibernate.order_inserts", "true");
        tuningProperties.put("hibernate.order_updates", "true");
        tuningProperties.put("hibernate.jdbc.fetch_size", "50");
        tuningProperties.put("hibernate.query.in_clause_parameter_padding", "true");
        log.infof("apply custom hibernate tuning properties: %s", tuningProperties);

        EntityManagerFactory emf = super.getEntityManagerFactory();
        emf.getProperties().putAll(tuningProperties);

        return emf;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        log.info("### Using custom Quarkus JPA Connection factory");
        super.postInit(factory);
    }
}
