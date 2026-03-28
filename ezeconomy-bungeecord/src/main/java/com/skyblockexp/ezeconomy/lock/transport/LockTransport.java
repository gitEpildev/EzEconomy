package com.skyblockexp.ezeconomy.lock.transport;

import java.util.UUID;

/**
 * Transport abstraction for Bungeecord lock requests. Real implementations should
 * send requests to the proxy (via plugin messaging) and return tokens or success.
 */
public interface LockTransport {
    /**
     * Try to acquire a lock for uuid with ttl; returns token on success or null on failure.
     */
    String acquire(UUID uuid, long ttlMs) throws InterruptedException;

    /**
     * Release a lock for uuid with token; returns true if released.
     */
    boolean release(UUID uuid, String token);
}
