package com.skyblockexp.ezeconomy.unit;

import com.skyblockexp.ezeconomy.api.storage.StorageProvider;
import com.skyblockexp.ezeconomy.feature.support.TestSupport;
import com.skyblockexp.ezeconomy.storage.TransferResult;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

public class StorageTransferUnitTest {
    private static final String CUR = "dollar";

    // Use shared in-memory MockStorage from TestSupport

    @Test
    public void testTransferSuccess() {
        TestSupport.MockStorage s = new TestSupport.MockStorage();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        s.setBalance(a, CUR, 10.0);
        s.setBalance(b, CUR, 0.0);

        TransferResult r = s.transfer(a, b, CUR, 5.0);
        assertTrue(r.isSuccess());
        assertEquals(5.0, s.getBalance(a, CUR), 0.0001);
        assertEquals(5.0, s.getBalance(b, CUR), 0.0001);
    }

    @Test
    public void testTransferInsufficientFunds() {
        TestSupport.MockStorage s = new TestSupport.MockStorage();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        s.setBalance(a, CUR, 1.0);
        s.setBalance(b, CUR, 0.0);

        TransferResult r = s.transfer(a, b, CUR, 5.0);
        assertFalse(r.isSuccess());
        assertEquals(1.0, s.getBalance(a, CUR), 0.0001);
        assertEquals(0.0, s.getBalance(b, CUR), 0.0001);
    }

    @Test
    public void testTransferNegativeAmounts() {
        TestSupport.MockStorage s = new TestSupport.MockStorage();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        s.setBalance(a, CUR, 10.0);
        s.setBalance(b, CUR, 0.0);

        TransferResult r = s.transfer(a, b, CUR, -5.0, -5.0);
        assertFalse(r.isSuccess());
        // balances unchanged
        assertEquals(10.0, s.getBalance(a, CUR), 0.0001);
        assertEquals(0.0, s.getBalance(b, CUR), 0.0001);
    }
}
