package com.skyblockexp.ezeconomy.cache;

/**
 * Global cache manager. Keeps a single active provider selected by strategy.
 */
public final class CacheManager {
    private static CacheProvider<?, ?> provider = new LocalCacheProvider<>();

    private CacheManager() {}

    @SuppressWarnings("unchecked")
    public static <K, V> CacheProvider<K, V> getProvider() {
        return (CacheProvider<K, V>) provider;
    }

    public static String getProviderClassName() {
        return provider == null ? "none" : provider.getClass().getName();
    }

    public static void setStrategy(CachingStrategy strategy) {
        try {
            switch (strategy) {
                case REDIS:
                    // Try to instantiate extension-provided RedisCacheProvider first
                    try {
                        Class<?> c = Class.forName("com.skyblockexp.ezeconomy.redis.cache.RedisCacheProvider");
                        provider = (CacheProvider<?, ?>) c.getDeclaredConstructor().newInstance();
                        return;
                    } catch (ClassNotFoundException ignored) {}
                    provider = new RedisCacheProvider<>();
                    break;
                case BUNGEECORD:
                    try {
                        Class<?> c = Class.forName("com.skyblockexp.ezeconomy.bungeecord.cache.BungeeCacheProvider");
                        provider = (CacheProvider<?, ?>) c.getDeclaredConstructor().newInstance();
                        return;
                    } catch (ClassNotFoundException ignored) {}
                    provider = new BungeeCacheProvider<>();
                    break;
                case DATABASE:
                    try {
                        Class<?> c = Class.forName("com.skyblockexp.ezeconomy.storage.cache.DatabaseCacheProvider");
                        provider = (CacheProvider<?, ?>) c.getDeclaredConstructor().newInstance();
                        return;
                    } catch (ClassNotFoundException ignored) {}
                    provider = new DatabaseCacheProvider<>();
                    break;
                case LOCAL:
                default:
                    provider = new LocalCacheProvider<>();
                    break;
            }
        } catch (Throwable t) {
            provider = new LocalCacheProvider<>();
        }
    }
}
