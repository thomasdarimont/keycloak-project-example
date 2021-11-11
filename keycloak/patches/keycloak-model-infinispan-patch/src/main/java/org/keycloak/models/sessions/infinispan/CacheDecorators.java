package org.keycloak.models.sessions.infinispan;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.context.Flag;

public class CacheDecorators {
    // Patch:Begin
    private static final boolean IGNORE_SKIP_CACHE_STORE = Boolean.getBoolean("keycloak.infinispan.ignoreSkipCacheStore");
    // Patch:End

    public CacheDecorators() {
    }

    public static <K, V> AdvancedCache<K, V> localCache(Cache<K, V> cache) {
        return cache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL);
    }

    public static <K, V> AdvancedCache<K, V> skipCacheLoaders(Cache<K, V> cache) {
        return cache.getAdvancedCache().withFlags(Flag.SKIP_CACHE_LOAD, Flag.SKIP_CACHE_STORE);
    }

    public static <K, V> AdvancedCache<K, V> skipCacheStore(Cache<K, V> cache) {
        // Patch:Begin
        if (IGNORE_SKIP_CACHE_STORE) {
            return cache.getAdvancedCache();
        }
        // Patch:End
        return cache.getAdvancedCache().withFlags(Flag.SKIP_CACHE_STORE);
    }
}
