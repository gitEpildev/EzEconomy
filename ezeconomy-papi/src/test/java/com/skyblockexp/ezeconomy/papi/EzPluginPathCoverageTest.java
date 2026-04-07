package com.skyblockexp.ezeconomy.papi;

import com.skyblockexp.ezeconomy.api.storage.StorageProvider;
import com.skyblockexp.ezeconomy.dto.EconomyPlayer;
import org.bukkit.OfflinePlayer;
import com.skyblockexp.ezeconomy.papi.testhelpers.TestPlayerFakes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class EzPluginPathCoverageTest {

    @AfterEach
    public void tearDown() {
        try { MockBukkit.unmock(); } catch (Exception ignored) {}
        EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = null;
    }

    public static class TestStorage implements StorageProvider {
        private final Map<UUID, Double> balances = new HashMap<>();
        public void put(UUID u, double v) { balances.put(u, v); }
        @Override public void init() {}
        @Override public void load() {}
        @Override public void save() {}
        @Override public double getBalance(UUID uuid, String currency) { return balances.getOrDefault(uuid, 0.0); }
        @Override public void setBalance(UUID uuid, String currency, double amount) { balances.put(uuid, amount); }
        @Override public void logTransaction(com.skyblockexp.ezeconomy.api.storage.models.Transaction transaction) {}
        @Override public java.util.List<com.skyblockexp.ezeconomy.api.storage.models.Transaction> getTransactions(UUID uuid, String currency) { return Collections.emptyList(); }
        @Override public boolean tryWithdraw(UUID uuid, String currency, double amount) { Double v = balances.get(uuid); if (v==null||v<amount) return false; balances.put(uuid, v-amount); return true; }
        @Override public void deposit(UUID uuid, String currency, double amount) { balances.put(uuid, balances.getOrDefault(uuid,0.0)+amount); }
        @Override public Map<UUID, Double> getAllBalances(String currency) { return new HashMap<>(balances); }
        @Override public java.util.Set<String> cleanupOrphanedPlayers() { return Collections.emptySet(); }
        @Override public boolean isConnected() { return true; }
        @Override public com.skyblockexp.ezeconomy.storage.TransferResult transfer(UUID fromUuid, UUID toUuid, String currency, double amount) { return com.skyblockexp.ezeconomy.storage.TransferResult.failure(0,0); }
        @Override public void shutdown() {}
        @Override public EconomyPlayer getPlayer(UUID uuid) { return new EconomyPlayer(uuid, "Name", null); }
        @Override public boolean createBank(String name, UUID owner) { return false; }
        @Override public boolean deleteBank(String name) { return false; }
        @Override public boolean bankExists(String name) { return false; }
        @Override public double getBankBalance(String name, String currency) { return 0; }
        @Override public void setBankBalance(String name, String currency, double amount) {}
        @Override public boolean tryWithdrawBank(String name, String currency, double amount) { return false; }
        @Override public void depositBank(String name, String currency, double amount) {}
        @Override public java.util.Set<String> getBanks() { return Collections.emptySet(); }
        @Override public boolean isBankOwner(String name, UUID uuid) { return false; }
        @Override public boolean isBankMember(String name, UUID uuid) { return false; }
        @Override public boolean addBankMember(String name, UUID uuid) { return false; }
        @Override public boolean removeBankMember(String name, UUID uuid) { return false; }
        @Override public java.util.Set<UUID> getBankMembers(String name) { return Collections.emptySet(); }
    }

    // Top-level static test plugin class so MockBukkit can create a proxy for it
    public static class SimpleEz extends com.skyblockexp.ezeconomy.core.EzEconomyPlugin {
        @Override
        public void onEnable() {
            // no bootstrap for tests
        }

        @Override
        public void onDisable() {
            // no-op
        }
    }

    @Test
    public void ezPlugin_nonTestHook_path_executed_for_top_and_balance() throws Exception {
        MockBukkit.mock();

        // Load the PAPI plugin (parent) so expansion has a plugin instance
        EzEconomyPapiPlugin papi = (EzEconomyPapiPlugin) MockBukkit.load(EzEconomyPapiPlugin.class);

        // Load a simple EzEconomy core plugin stub (static nested class)
        com.skyblockexp.ezeconomy.core.EzEconomyPlugin core = (com.skyblockexp.ezeconomy.core.EzEconomyPlugin) MockBukkit.load(SimpleEz.class);

        // Install a test storage into the core plugin via reflection (setStorage is available)
        TestStorage ts = new TestStorage();
        java.util.UUID u = java.util.UUID.randomUUID();
        ts.put(u, 777.0);
        core.setStorage(ts);

        // Now ensure plugin manager maps "EzEconomy" to our core plugin so expansion picks it up
        org.bukkit.plugin.PluginManager pm = org.bukkit.Bukkit.getPluginManager();
        Field[] fields = pm.getClass().getDeclaredFields();
        for (Field f : fields) {
            if (Map.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                Object map = f.get(pm);
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, org.bukkit.plugin.Plugin> m = (Map<String, org.bukkit.plugin.Plugin>) map;
                    m.put("EzEconomy", core);
                } catch (ClassCastException ignored) {}
            }
        }

        EzEconomyPAPIExpansion expansion = new EzEconomyPAPIExpansion(papi);

        OfflinePlayer fake = TestPlayerFakes.fakeOfflinePlayer(u);

        // Call balance which should use core.getStorageOrWarn path
        String bal = expansion.onPlaceholderRequest(fake, "balance");
        assertNotNull(bal);

        // Call top_ which should schedule async refresh; we exercise the scheduling path
        String first = expansion.onPlaceholderRequest(null, "top_1_dollar");
        assertNotNull(first);
    }
}
