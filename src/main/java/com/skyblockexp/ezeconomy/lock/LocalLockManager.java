package com.skyblockexp.ezeconomy.lock;

import com.skyblockexp.ezeconomy.storage.TransferLockManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class LocalLockManager implements LockManager {
    private final Map<UUID, String> tokens = new ConcurrentHashMap<>();

    @Override
    public String acquire(UUID uuid, long ttlMs, long retryMs, int maxAttempts) throws InterruptedException {
        ReentrantLock lock = TransferLockManager.getLock(uuid);
        int attempts = 0;
        while (maxAttempts <= 0 || attempts < maxAttempts) {
            // try immediate acquisition
            if (lock.tryLock()) {
                String token = "local-" + UUID.randomUUID();
                tokens.put(uuid, token);
                return token;
            }
            attempts++;
            if (retryMs > 0) Thread.sleep(retryMs);
            // if maxAttempts <= 0 we treat as infinite attempts (but still yield between tries)
        }
        return null;
    }

    @Override
    public boolean release(UUID uuid, String token) {
        String existing = tokens.get(uuid);
        if (existing == null) return false;
        if (!existing.equals(token)) return false;
        ReentrantLock lock = TransferLockManager.getLock(uuid);
        try {
            tokens.remove(uuid);
            lock.unlock();
            return true;
        } catch (IllegalMonitorStateException ex) {
            return false;
        }
    }
}
