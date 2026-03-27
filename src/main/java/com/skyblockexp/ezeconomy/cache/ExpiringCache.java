package com.skyblockexp.ezeconomy.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple expiring cache utility. Thread-safe, lightweight; entries store an expiry timestamp.
 */
public class ExpiringCache<K, V> {
    private final Map<K, Entry<V>> map = new ConcurrentHashMap<>();

    public static final class Entry<V> {
        public final V value;
        public final long expiresAt;

        public Entry(V value, long expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }
    }

    /**
     * Returns the entry snapshot (value + expiresAt) or null if absent.
     * The caller may check expiresAt to decide freshness.
     */
    public Entry<V> getEntry(K key) {
        return map.get(key);
    }

    /** Put a value with a TTL in milliseconds. */
    public void put(K key, V value, long ttlMs) {
        long expiresAt = System.currentTimeMillis() + Math.max(0, ttlMs);
        map.put(key, new Entry<>(value, expiresAt));
    }

    /** Remove an entry. */
    public void remove(K key) {
        map.remove(key);
    }
}
