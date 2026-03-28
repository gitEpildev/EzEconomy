package com.skyblockexp.ezeconomy.lock;

import com.skyblockexp.ezeconomy.lock.transport.LockTransport;
import com.skyblockexp.ezeconomy.lock.transport.MockTransport;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class BungeeLockManagerTest {

    @Test
    public void acquireAndReleaseSucceeds() throws InterruptedException {
        LockTransport transport = new MockTransport();
        BungeeLockManager mgr = new BungeeLockManager(transport, 5000, 10, 3);

        UUID id = UUID.randomUUID();
        String token = mgr.acquire(id, 5000, 10, 3);
        assertNotNull(token, "Expected token from acquire");

        boolean released = mgr.release(id, token);
        assertTrue(released, "Expected release to succeed with correct token");
    }

    @Test
    public void acquireFailsWhenLocked() throws InterruptedException {
        MockTransport transport = new MockTransport();
        BungeeLockManager mgr1 = new BungeeLockManager(transport, 5000, 10, 1);
        BungeeLockManager mgr2 = new BungeeLockManager(transport, 5000, 10, 1);

        UUID id = UUID.randomUUID();
        String t1 = mgr1.acquire(id, 5000, 10, 1);
        assertNotNull(t1);

        String t2 = mgr2.acquire(id, 5000, 10, 1);
        assertNull(t2, "Second acquire should fail immediately with maxAttempts=1");

        assertTrue(mgr1.release(id, t1));
    }
}
