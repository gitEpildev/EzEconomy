package com.skyblockexp.ezeconomy.feature.support;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;

public class MockStoragePlayerExistsTest {
    @Test
    public void testPlayerExists_beforeAndAfterSetBalance() {
        TestSupport.MockStorage storage = new TestSupport.MockStorage();
        UUID u = UUID.randomUUID();
        assertFalse(storage.playerExists(u));

        storage.setBalance(u, "dollar", 0.0);
        assertTrue(storage.playerExists(u));
    }
}
