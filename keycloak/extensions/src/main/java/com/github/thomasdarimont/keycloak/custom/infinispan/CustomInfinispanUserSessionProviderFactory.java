package com.github.thomasdarimont.keycloak.custom.infinispan;

import com.google.auto.service.AutoService;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.common.util.MultiSiteUtils;
import org.keycloak.models.UserSessionProviderFactory;
import org.keycloak.models.sessions.infinispan.InfinispanUserSessionProviderFactory;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.ArrayBlockingQueue;

@JBossLog
// @AutoService(UserSessionProviderFactory.class)
public class CustomInfinispanUserSessionProviderFactory extends InfinispanUserSessionProviderFactory {

    @Override
    public void init(Config.Scope config) {
        super.init(config);
        log.infof("### Using patched InfinispanUserSessionProviderFactory");
        boolean useBatches = config.getBoolean(CONFIG_USE_BATCHES, /*DEFAULT_USE_BATCHES*/ true) && MultiSiteUtils.isPersistentSessionsEnabled();
        if (useBatches) {
            // -Dkeycloak.infinispan.asyncQueuePersistentUpdateSize=5000
            int queueSize = Integer.getInteger("keycloak.infinispan.asyncQueuePersistentUpdateSize", 1000 /* default */);
            var q = new ArrayBlockingQueue<>(queueSize);

            try {

                VarHandle asyncQueuePersistentUpdateHandle = MethodHandles
                        .privateLookupIn(InfinispanUserSessionProviderFactory.class, MethodHandles.lookup())
                        .findVarHandle(InfinispanUserSessionProviderFactory.class, "asyncQueuePersistentUpdate", ArrayBlockingQueue.class);
                asyncQueuePersistentUpdateHandle.set(this, q);

                log.infof("### Using patched InfinispanUserSessionProviderFactory with asyncQueuePersistentUpdateSize=%s", queueSize);
            } catch (Exception e) {
                log.warn("### Could not patch InfinispanUserSessionProviderFactory", e);
            }
        }
    }
}
