package com.skyblockexp.ezeconomy.bungeecord.proxy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Framework-agnostic lock proxy logic. This class implements the lock store and
 * processes incoming plugin-message payloads (as byte arrays). It returns an
 * optional response payload for "ACQUIRE" requests that should be sent back
 * to the originating server.
 *
 * To integrate with Bungee/Waterfall, register a plugin message listener that
 * calls `processIncoming(channel, data)` and sends the returned payload (if
 * present) back on the same channel to the originating server.
 */
public class EzBungeeProxy implements AutoCloseable {
    private static class Entry { String token; long expiresAt; }
    private final Map<UUID, Entry> locks = new ConcurrentHashMap<>();
    private final String expectedSecret;
    private final ScheduledExecutorService cleanupScheduler;

    public EzBungeeProxy() { this("", 5000L); }

    public EzBungeeProxy(String expectedSecret) { this(expectedSecret, 5000L); }

    public EzBungeeProxy(String expectedSecret, long cleanupIntervalMs) {
        this.expectedSecret = expectedSecret == null ? "" : expectedSecret;
        this.cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "EzBungeeProxy-Cleanup");
            t.setDaemon(true);
            return t;
        });
        if (cleanupIntervalMs > 0) {
            this.cleanupScheduler.scheduleAtFixedRate(this::cleanupExpiredLocks, cleanupIntervalMs, cleanupIntervalMs, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Process an incoming plugin message payload. If a response payload should be
     * returned (ACQUIRE_RESPONSE), it will be returned; otherwise null.
     */
    public byte[] processIncoming(byte[] data) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            String action = in.readUTF();
            String incomingSecret = in.readUTF();
            if (this.expectedSecret != null && this.expectedSecret.length() > 0) {
                if (incomingSecret == null || !this.expectedSecret.equals(incomingSecret)) return null;
            }
            String requestId = in.readUTF();
            if ("ACQUIRE".equals(action)) {
                String uuidStr = in.readUTF();
                long ttl = in.readLong();
                UUID uuid = UUID.fromString(uuidStr);
                long now = System.currentTimeMillis();
                Entry created = null;
                Entry existing = locks.get(uuid);
                if (existing == null || existing.expiresAt <= now) {
                    Entry e = new Entry();
                    e.token = Long.toHexString(ThreadLocalRandomHolder.nextLong()) + Long.toHexString(now);
                    e.expiresAt = now + ttl;
                    locks.put(uuid, e);
                    created = e;
                }
                String token = created == null ? "" : created.token;
                return buildAcquireResponse(requestId, token);
            } else if ("RELEASE".equals(action)) {
                String uuidStr = in.readUTF();
                String token = in.readUTF();
                UUID uuid = UUID.fromString(uuidStr);
                Entry e = locks.get(uuid);
                if (e != null && e.token != null && e.token.equals(token)) {
                    locks.remove(uuid);
                }
                return null;
            }
        } catch (Exception ex) {
            // swallow - caller should log if desired
        }
        return null;
    }

    private byte[] buildAcquireResponse(String requestId, String token) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        out.writeUTF("ACQUIRE_RESPONSE");
        out.writeUTF(expectedSecret == null ? "" : expectedSecret);
        out.writeUTF(requestId);
        out.writeUTF(token == null ? "" : token);
        out.flush();
        return baos.toByteArray();
    }

    private static class ThreadLocalRandomHolder {
        static long nextLong() { return java.util.concurrent.ThreadLocalRandom.current().nextLong(); }
    }

    /**
     * Remove expired locks from the local store. Public so tests can trigger cleanup
     * deterministically; also run periodically by an internal scheduler when enabled.
     */
    public void cleanupExpiredLocks() {
        long now = System.currentTimeMillis();
        try {
            locks.entrySet().removeIf(e -> e.getValue() == null || e.getValue().expiresAt <= now);
        } catch (Throwable t) {
            // swallow to avoid scheduler termination
        }
    }

    @Override
    public void close() {
        try { cleanupScheduler.shutdownNow(); } catch (Exception ignored) {}
    }
}
