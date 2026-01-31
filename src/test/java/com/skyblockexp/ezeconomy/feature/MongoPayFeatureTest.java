package com.skyblockexp.ezeconomy.feature;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.api.storage.StorageProvider;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MongoPayFeatureTest {
    private Object server;
    private EzEconomyPlugin plugin;

    @BeforeEach
    public void setup() throws Exception {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(EzEconomyPlugin.class);

        // Force test mode to avoid real network init
        System.setProperty("ezeconomy.test", "true");

        // Inject a lightweight in-memory StorageProvider that simulates MongoDB behavior
        TestMongoStorage provider = new TestMongoStorage();
        Field storageField = EzEconomyPlugin.class.getDeclaredField("storage");
        storageField.setAccessible(true);
        storageField.set(plugin, provider);

        plugin.loadMessageProvider();
    }

    @AfterEach
    public void teardown() {
        System.clearProperty("ezeconomy.test");
        MockBukkit.unmock();
    }

    @Test
    public void testPayCommand_offlineRecipient_withMongoStorage() throws Exception {
        // create offline recipient
        Object offlineObj = null;
        try {
            java.lang.reflect.Method addOffline = server.getClass().getMethod("addOfflinePlayer", String.class);
            offlineObj = addOffline.invoke(server, "mongoOffline");
        } catch (NoSuchMethodException ignored) {
            offlineObj = org.bukkit.Bukkit.getOfflinePlayer("mongoOffline");
        }
        org.bukkit.OfflinePlayer offline = (org.bukkit.OfflinePlayer) offlineObj;

        // create sender
        Object senderObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "mongoSender");
        org.bukkit.entity.Player sender = (org.bukkit.entity.Player) senderObj;
        sender.setOp(true);

        // get provider and initialize balances
        Field storageField = EzEconomyPlugin.class.getDeclaredField("storage");
        storageField.setAccessible(true);
        TestMongoStorage storage = (TestMongoStorage) storageField.get(plugin);

        storage.setBalance(sender.getUniqueId(), "dollar", 10.0);
        storage.setBalance(offline.getUniqueId(), "dollar", 0.0);

        // perform command which will invoke storage transfer path
        sender.performCommand("pay mongoOffline 5");

        // wait briefly for any async actions
        Thread.sleep(300);

        assertEquals(5.0, storage.getBalance(sender.getUniqueId(), "dollar"), 0.0001);
        assertEquals(5.0, storage.getBalance(offline.getUniqueId(), "dollar"), 0.0001);
    }

    // Simple in-memory test storage that implements the StorageProvider contract
    public static class TestMongoStorage implements StorageProvider {
        private final Map<String, Double> balances = new HashMap<>();

        private String key(UUID uuid, String currency) {
            return uuid.toString() + ":" + currency;
        }

        @Override public void init() {}
        @Override public void load() {}
        @Override public void save() {}

        @Override
        public double getBalance(UUID uuid, String currency) {
            return balances.getOrDefault(key(uuid, currency), 0.0);
        }

        @Override
        public void setBalance(UUID uuid, String currency, double amount) {
            balances.put(key(uuid, currency), amount);
        }

        @Override
        public void logTransaction(com.skyblockexp.ezeconomy.api.storage.models.Transaction transaction) {}

        @Override
        public java.util.List<com.skyblockexp.ezeconomy.api.storage.models.Transaction> getTransactions(UUID uuid, String currency) { return java.util.Collections.emptyList(); }

        @Override
        public boolean tryWithdraw(UUID uuid, String currency, double amount) {
            double bal = getBalance(uuid, currency);
            if (bal < amount) return false;
            setBalance(uuid, currency, bal - amount);
            return true;
        }

        @Override
        public void deposit(UUID uuid, String currency, double amount) {
            double bal = getBalance(uuid, currency);
            setBalance(uuid, currency, bal + amount);
        }

        @Override
        public java.util.Map<UUID, Double> getAllBalances(String currency) {
            java.util.Map<UUID, Double> out = new java.util.HashMap<>();
            for (java.util.Map.Entry<String, Double> e : balances.entrySet()) {
                String k = e.getKey();
                int idx = k.lastIndexOf(":");
                if (idx <= 0) continue;
                String uuidStr = k.substring(0, idx);
                String cur = k.substring(idx + 1);
                if (!currency.equals(cur)) continue;
                try {
                    UUID id = UUID.fromString(uuidStr);
                    out.put(id, e.getValue());
                } catch (IllegalArgumentException ignore) {}
            }
            return out;
        }

        @Override public void shutdown() {}

        // Bank methods trivial
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
}
