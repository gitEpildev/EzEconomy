package com.skyblockexp.ezeconomy.lock;

import java.util.UUID;

public interface LockManager {
    /**
     * Acquire a lock for the given UUID. Returns a token that must be supplied to release.
     * Returns null if lock couldn't be acquired after attempts.
     */
    String acquire(UUID uuid, long ttlMs, long retryMs, int maxAttempts) throws InterruptedException;

    /**
     * Release the lock for the given UUID using the token returned by acquire.
     */
    boolean release(UUID uuid, String token);

    default String[] acquireOrdered(UUID[] uuids, long ttlMs, long retryMs, int maxAttempts) throws InterruptedException {
        String[] tokens = new String[uuids.length];
        for (int i = 0; i < uuids.length; i++) {
            tokens[i] = acquire(uuids[i], ttlMs, retryMs, maxAttempts);
            if (tokens[i] == null) {
                // release previously acquired
                for (int j = 0; j < i; j++) {
                    try { release(uuids[j], tokens[j]); } catch (Exception ignored) {}
                }
                return null;
            }
        }
        return tokens;
    }

    default void releaseOrdered(UUID[] uuids, String[] tokens) {
        if (uuids == null || tokens == null) return;
        for (int i = 0; i < uuids.length; i++) {
            try { release(uuids[i], tokens[i]); } catch (Exception ignored) {}
        }
    }
}
