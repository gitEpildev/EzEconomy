package com.skyblockexp.ezeconomy.cache;

/**
 * Local in-memory cache provider backed by ExpiringCache.
 */
public class LocalCacheProvider<K, V> implements CacheProvider<K, V> {
    private final ExpiringCache<K, V> cache = new ExpiringCache<>();

    @Override
    public ExpiringCache.Entry<V> getEntry(K key) {
        return cache.getEntry(key);
    }

    @Override
    public void put(K key, V value, long ttlMs) {
        cache.put(key, value, ttlMs);
    }

    @Override
    public void remove(K key) {
        cache.remove(key);
    }
}
