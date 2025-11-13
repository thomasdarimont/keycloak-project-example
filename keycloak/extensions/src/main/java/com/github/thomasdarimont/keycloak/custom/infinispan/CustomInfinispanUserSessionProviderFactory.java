package com.github.thomasdarimont.keycloak.custom.infinispan;

import com.google.auto.service.AutoService;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.common.util.MultiSiteUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.UserSessionProviderFactory;
import org.keycloak.models.sessions.infinispan.InfinispanUserSessionProviderFactory;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.ArrayBlockingQueue;

@JBossLog
//@AutoService(UserSessionProviderFactory.class)
public class CustomInfinispanUserSessionProviderFactory extends InfinispanUserSessionProviderFactory {

    @Override
    public UserSessionProvider create(KeycloakSession session) {

        UserSessionProvider target = super.create(session);
        return UserSessionProvider.class.cast(Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{UserSessionProvider.class}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                if (method.getName().equals("getUserSession") && args.length == 2 && /* find the proper parameter types */ true) {
                    // ddd
                }

                return method.invoke(target, args);
            }
        }));
    }

    //    @Override
//    public void init(Config.Scope config) {
//        super.init(config);
//        log.infof("### Using patched InfinispanUserSessionProviderFactory");
//        boolean useBatches = config.getBoolean(CONFIG_USE_BATCHES, /*DEFAULT_USE_BATCHES*/ true) && MultiSiteUtils.isPersistentSessionsEnabled();
//        if (useBatches) {
//            // -Dkeycloak.infinispan.asyncQueuePersistentUpdateSize=5000
//            int queueSize = Integer.getInteger("keycloak.infinispan.asyncQueuePersistentUpdateSize", 1000 /* default */);
//            var q = new ArrayBlockingQueue<>(queueSize);
//
//            try {
//
//                VarHandle asyncQueuePersistentUpdateHandle = MethodHandles
//                        .privateLookupIn(InfinispanUserSessionProviderFactory.class, MethodHandles.lookup())
//                        .findVarHandle(InfinispanUserSessionProviderFactory.class, "asyncQueuePersistentUpdate", ArrayBlockingQueue.class);
//                asyncQueuePersistentUpdateHandle.set(this, q);
//
//                log.infof("### Using patched InfinispanUserSessionProviderFactory with asyncQueuePersistentUpdateSize=%s", queueSize);
//            } catch (Exception e) {
//                log.warn("### Could not patch InfinispanUserSessionProviderFactory", e);
//            }
//        }
//    }
}
