package com.skyblockexp.ezeconomy.lock.transport;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * In-memory mock transport used for unit tests and local runs. Simulates a proxy lock store.
 */
public class MockTransport implements LockTransport {
    private static class Entry { String token; long expiresAt; }
    private final Map<UUID, Entry> locks = new ConcurrentHashMap<>();

    @Override
    public String acquire(UUID uuid, long ttlMs) throws InterruptedException {
        long now = System.currentTimeMillis();
        synchronized (locks) {
            Entry old = locks.get(uuid);
            if (old == null || old.expiresAt <= now) {
                Entry ne = new Entry();
                ne.token = Long.toHexString(ThreadLocalRandom.current().nextLong()) + Long.toHexString(now);
                ne.expiresAt = now + ttlMs;
                locks.put(uuid, ne);
                return ne.token;
            }
            return null;
        }
    }

    @Override
    public boolean release(UUID uuid, String token) {
        Entry e = locks.get(uuid);
        if (e == null) return false;
        if (e.token != null && e.token.equals(token)) {
            locks.remove(uuid);
            return true;
        }
        return false;
    }
}
