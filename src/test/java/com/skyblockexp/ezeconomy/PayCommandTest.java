package com.skyblockexp.ezeconomy;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.api.storage.StorageProvider;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PayCommandTest {
    // avoid declaring ServerMock as a field to prevent classloading issues during test discovery
    private Object server;
    private EzEconomyPlugin plugin;
    private MockStorage storage;

    private static final String CURRENCY = "dollar";

    @BeforeEach
    public void setup() throws Exception {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(EzEconomyPlugin.class);

        // Replace storage with a lightweight in-memory mock using reflection
        storage = new MockStorage();
        Field storageField = EzEconomyPlugin.class.getDeclaredField("storage");
        storageField.setAccessible(true);
        storageField.set(plugin, storage);

        // Ensure messages are loaded
        plugin.loadMessageProvider();
    }

    @AfterEach
    public void teardown() {
        MockBukkit.unmock();
    }

    @Test
    public void testPayCommand_transfersMoneyBetweenPlayers() {
        try {
            Object senderObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "payer");
            Object recipientObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "payee");
            org.bukkit.entity.Player sender = (org.bukkit.entity.Player) senderObj;
            org.bukkit.entity.Player recipient = (org.bukkit.entity.Player) recipientObj;
            // Give permission
            sender.setOp(true);

            // Initialize balances
            storage.setBalance(sender.getUniqueId(), CURRENCY, 10.0);
            storage.setBalance(recipient.getUniqueId(), CURRENCY, 0.0);

            // Run the command as the sender
            sender.performCommand("pay payee 5");

            // Assert balances updated
            assertEquals(5.0, storage.getBalance(sender.getUniqueId(), CURRENCY), 0.0001);
            assertEquals(5.0, storage.getBalance(recipient.getUniqueId(), CURRENCY), 0.0001);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    // Minimal in-memory StorageProvider used only by tests
    public static class MockStorage implements StorageProvider {
        private final Map<String, Double> balances = new HashMap<>();

        private String key(UUID uuid, String currency) {
            return uuid.toString() + ":" + currency;
        }

        @Override
        public void init() {}

        @Override
        public void load() {}

        @Override
        public void save() {}

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
        public java.util.Map<UUID, Double> getAllBalances(String currency) { return java.util.Collections.emptyMap(); }

        @Override
        public void shutdown() {}

        // Bank methods not used in this test - provide trivial implementations
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
