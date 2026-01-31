package com.skyblockexp.ezeconomy;

import com.skyblockexp.ezeconomy.api.storage.StorageProvider;
import com.skyblockexp.ezeconomy.storage.TransferResult;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

public class StorageTransferUnitTest {
    private static final String CUR = "dollar";

    static class InMemoryStorage implements StorageProvider {
        private final Map<UUID, Double> balances = new ConcurrentHashMap<>();

        @Override public void init() {}
        @Override public void load() {}
        @Override public void save() {}
        @Override public void logTransaction(com.skyblockexp.ezeconomy.api.storage.models.Transaction transaction) {}
        @Override public java.util.List<com.skyblockexp.ezeconomy.api.storage.models.Transaction> getTransactions(UUID uuid, String currency) { return java.util.Collections.emptyList(); }
        @Override public java.util.Map<UUID, Double> getAllBalances(String currency) { return java.util.Collections.unmodifiableMap(balances); }
        @Override public void shutdown() {}

        @Override
        public double getBalance(UUID uuid, String currency) {
            return balances.getOrDefault(uuid, 0.0);
        }

        @Override
        public void setBalance(UUID uuid, String currency, double amount) {
            balances.put(uuid, amount);
        }

        @Override
        public boolean tryWithdraw(UUID uuid, String currency, double amount) {
            double b = getBalance(uuid, currency);
            if (b < amount) return false;
            setBalance(uuid, currency, b - amount);
            return true;
        }

        @Override
        public void deposit(UUID uuid, String currency, double amount) {
            double b = getBalance(uuid, currency);
            setBalance(uuid, currency, b + amount);
        }

        // Banks - no-op stubs
        @Override public boolean createBank(String name, UUID owner) { return false; }
        @Override public boolean deleteBank(String name) { return false; }
        @Override public boolean bankExists(String name) { return false; }
        @Override public double getBankBalance(String name, String currency) { return 0; }
        @Override public void setBankBalance(String name, String currency, double amount) {}
        @Override public boolean tryWithdrawBank(String name, String currency, double amount) { return false; }
        @Override public void depositBank(String name, String currency, double amount) {}
        @Override public java.util.Set<String> getBanks() { return java.util.Collections.emptySet(); }
        @Override public boolean isBankOwner(String name, UUID uuid) { return false; }
        @Override public boolean isBankMember(String name, UUID uuid) { return false; }
        @Override public boolean addBankMember(String name, UUID uuid) { return false; }
        @Override public boolean removeBankMember(String name, UUID uuid) { return false; }
        @Override public java.util.Set<UUID> getBankMembers(String name) { return java.util.Collections.emptySet(); }
    }

    @Test
    public void testTransferSuccess() {
        InMemoryStorage s = new InMemoryStorage();
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
        InMemoryStorage s = new InMemoryStorage();
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
        InMemoryStorage s = new InMemoryStorage();
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
