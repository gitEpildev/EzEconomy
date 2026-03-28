package com.skyblockexp.ezeconomy.bungeecord.proxy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.UUID;

public class EzBungeeProxySecretTest {
    @Test
    public void testAcquireWithCorrectSecret() throws Exception {
        String secret = "s3cr3t";
        EzBungeeProxy proxy = new EzBungeeProxy(secret);

        String requestId = "req-1";
        UUID id = UUID.randomUUID();
        byte[] payload = buildAcquirePayload(secret, requestId, id, 60000L);
        byte[] response = proxy.processIncoming(payload);
        Assertions.assertNotNull(response, "Expected a response payload when secret matches");
        // basic validation: response must contain ACQUIRE_RESPONSE and same requestId
        // We'll parse minimal fields to assert requestId presence
        java.io.DataInputStream in = new java.io.DataInputStream(new java.io.ByteArrayInputStream(response));
        String action = in.readUTF();
        String respSecret = in.readUTF();
        String rid = in.readUTF();
        Assertions.assertEquals("ACQUIRE_RESPONSE", action);
        Assertions.assertEquals(secret, respSecret);
        Assertions.assertEquals(requestId, rid);
    }

    @Test
    public void testAcquireWithIncorrectSecret() throws Exception {
        String secret = "s3cr3t";
        EzBungeeProxy proxy = new EzBungeeProxy(secret);

        String requestId = "req-2";
        UUID id = UUID.randomUUID();
        byte[] payload = buildAcquirePayload("wrong", requestId, id, 60000L);
        byte[] response = proxy.processIncoming(payload);
        Assertions.assertNull(response, "Expected null response when secret does not match");
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
