package com.skyblockexp.ezeconomy.lock;

import com.skyblockexp.ezeconomy.lock.transport.LockTransport;
import com.skyblockexp.ezeconomy.lock.transport.MockTransport;

import java.util.UUID;

/**
 * Transport-backed BungeeLockManager. Currently uses an in-process MockTransport by default.
 * Later this should be replaced by a plugin-messaging transport that communicates with the proxy.
 */
public class BungeeLockManager implements LockManager {
    private final LockTransport transport;
    private final long defaultTtlMs;
    private final long defaultRetryMs;
    private final int defaultMaxAttempts;
    private static volatile LockTransport globalTransport = null;

    public BungeeLockManager() {
        this.transport = globalTransport != null ? globalTransport : new MockTransport();
        this.defaultTtlMs = 60000;
        this.defaultRetryMs = 150;
        this.defaultMaxAttempts = 5;
    }

    // Visible for testing / alternate transport injection
    public BungeeLockManager(LockTransport transport, long ttlMs, long retryMs, int maxAttempts) {
        this.transport = transport;
        this.defaultTtlMs = ttlMs;
        this.defaultRetryMs = retryMs;
        this.defaultMaxAttempts = maxAttempts;
    }

    @Override
    public String acquire(UUID uuid, long ttlMs, long retryMs, int maxAttempts) throws InterruptedException {
        long useTtl = ttlMs <= 0 ? this.defaultTtlMs : ttlMs;
        long useRetry = retryMs <= 0 ? this.defaultRetryMs : retryMs;
        int useMax = maxAttempts <= 0 ? this.defaultMaxAttempts : maxAttempts;

        for (int attempt = 0; attempt < useMax; attempt++) {
            String token = transport.acquire(uuid, useTtl);
            if (token != null) return token;
            Thread.sleep(useRetry);
        }
        return null;
    }

    @Override
    public boolean release(UUID uuid, String token) {
        try {
            return transport.release(uuid, token);
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Set a global transport to be used by no-arg constructors. Intended to be called
     * by the bootstrap (LockingComponent) when a plugin messaging transport is available.
     */
    public static void setGlobalTransport(LockTransport transport) {
        globalTransport = transport;
    }
}