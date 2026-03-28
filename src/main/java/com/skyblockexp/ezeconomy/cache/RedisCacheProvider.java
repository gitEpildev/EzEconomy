package com.skyblockexp.ezeconomy.cache;

/**
 * Redis-backed cache provider. This is a lightweight stub that currently falls
 * back to a local in-memory cache. Replace with a real Redis client implementation
 * when integrating with the Redis extension.
 */
public class RedisCacheProvider<K, V> implements CacheProvider<K, V> {
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
