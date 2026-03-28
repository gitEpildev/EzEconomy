package com.skyblockexp.ezeconomy.cache;

/**
 * Database-backed cache provider. Current implementation is a lightweight
 * local fallback. Swap to a real DB-backed store when integrating with
 * the StorageProvider or direct JDBC access.
 */
public class DatabaseCacheProvider<K, V> implements CacheProvider<K, V> {
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
