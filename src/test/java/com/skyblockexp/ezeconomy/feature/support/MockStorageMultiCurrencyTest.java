package com.skyblockexp.ezeconomy.feature.support;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;

public class MockStorageMultiCurrencyTest {
    @Test
    public void testPlayerExists_whenBalanceInDifferentCurrency() {
        TestSupport.MockStorage storage = new TestSupport.MockStorage();
        UUID u = UUID.randomUUID();
        assertFalse(storage.playerExists(u));

        // set balance in a non-default currency
        storage.setBalance(u, "euro", 5.0);

        // playerExists should be true regardless of which currency the balance was set in
        assertTrue(storage.playerExists(u));
    }
}
