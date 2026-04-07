package com.skyblockexp.ezeconomy.lock;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class RedisLockManagerTest {

    @Test
    void integrationAcquireReleaseIfRedisAvailable() throws InterruptedException {
        RedisLockManager rlm;
        try {
            rlm = new RedisLockManager("localhost", 6379, null, 0);
        } catch (Exception ex) {
            Assumptions.assumeTrue(false, "Redis not available locally: " + ex.getMessage());
            return;
        }

        UUID id = UUID.randomUUID();
        String token = rlm.acquire(id, 1000, 50, 5);
        assertNotNull(token);
        assertTrue(rlm.release(id, token));
        rlm.close();
    }
}
