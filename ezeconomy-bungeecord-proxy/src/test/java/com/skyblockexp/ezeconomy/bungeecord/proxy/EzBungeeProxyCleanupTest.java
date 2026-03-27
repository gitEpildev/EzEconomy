package com.skyblockexp.ezeconomy.bungeecord.proxy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.UUID;

public class EzBungeeProxyCleanupTest {
    @Test
    public void testCleanupRemovesExpiredLock() throws Exception {
        EzBungeeProxy proxy = new EzBungeeProxy("");

        String requestId = "req-clean-1";
        UUID id = UUID.randomUUID();
        long ttl = 100L; // 100ms
        byte[] payload = buildAcquirePayload("", requestId, id, ttl);
        byte[] response = proxy.processIncoming(payload);
        Assertions.assertNotNull(response, "Expected initial acquire response");

        // immediate second acquire should fail (locked)
        byte[] response2 = proxy.processIncoming(buildAcquirePayload("", "req-clean-2", id, ttl));
        java.io.DataInputStream in2 = new java.io.DataInputStream(new java.io.ByteArrayInputStream(response2));
        String action2 = in2.readUTF();
        String secret2 = in2.readUTF();
        String rid2 = in2.readUTF();
        String token2 = in2.readUTF();
        Assertions.assertEquals("ACQUIRE_RESPONSE", action2);
        Assertions.assertEquals("", token2);

        // wait for TTL to expire, then trigger cleanup
        Thread.sleep(ttl + 50);
        proxy.cleanupExpiredLocks();

        // now acquire should succeed again
        byte[] response3 = proxy.processIncoming(buildAcquirePayload("", "req-clean-3", id, ttl));
        java.io.DataInputStream in3 = new java.io.DataInputStream(new java.io.ByteArrayInputStream(response3));
        String action3 = in3.readUTF();
        String secret3 = in3.readUTF();
        String rid3 = in3.readUTF();
        String token3 = in3.readUTF();
        Assertions.assertEquals("ACQUIRE_RESPONSE", action3);
        Assertions.assertNotEquals("", token3);
    }

    private byte[] buildAcquirePayload(String secret, String requestId, UUID uuid, long ttl) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        out.writeUTF("ACQUIRE");
        out.writeUTF(secret == null ? "" : secret);
        out.writeUTF(requestId);
        out.writeUTF(uuid.toString());
        out.writeLong(ttl);
        out.flush();
        return baos.toByteArray();
    }
}
