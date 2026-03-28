package com.skyblockexp.ezeconomy.cache;

/**
 * Bungee-backed cache provider. Stub implementation uses local fallback.
 */
public class BungeeCacheProvider<K, V> implements CacheProvider<K, V> {
    private final LocalCacheProvider<K, V> fallback = new LocalCacheProvider<>();

    @Override
    public ExpiringCache.Entry<V> getEntry(K key) {
        return fallback.getEntry(key);
    }

    @Override
    public void put(K key, V value, long ttlMs) {
        fallback.put(key, value, ttlMs);
    }

    @Override
    public void remove(K key) {
        fallback.remove(key);
    }
}
