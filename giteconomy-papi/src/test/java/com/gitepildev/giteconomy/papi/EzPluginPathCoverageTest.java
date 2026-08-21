package com.gitepildev.giteconomy.papi;

import com.gitepildev.giteconomy.api.storage.StorageProvider;
import com.gitepildev.giteconomy.dto.EconomyPlayer;
import org.bukkit.OfflinePlayer;
import com.gitepildev.giteconomy.papi.testhelpers.TestPlayerFakes;
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
        GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = null;
    }

    public static class TestStorage implements StorageProvider {
        private final Map<UUID, Double> balances = new HashMap<>();
        public void put(UUID u, double v) { balances.put(u, v); }
        @Override public void init() {}
        @Override public void load() {}
        @Override public void save() {}
        @Override public double getBalance(UUID uuid, String currency) { return balances.getOrDefault(uuid, 0.0); }
        @Override public void setBalance(UUID uuid, String currency, double amount) { balances.put(uuid, amount); }
        @Override public void logTransaction(com.gitepildev.giteconomy.api.storage.models.Transaction transaction) {}
        @Override public java.util.List<com.gitepildev.giteconomy.api.storage.models.Transaction> getTransactions(UUID uuid, String currency) { return Collections.emptyList(); }
        @Override public boolean tryWithdraw(UUID uuid, String currency, double amount) { Double v = balances.get(uuid); if (v==null||v<amount) return false; balances.put(uuid, v-amount); return true; }
        @Override public void deposit(UUID uuid, String currency, double amount) { balances.put(uuid, balances.getOrDefault(uuid,0.0)+amount); }
        @Override public Map<UUID, Double> getAllBalances(String currency) { return new HashMap<>(balances); }
        @Override public java.util.Set<String> cleanupOrphanedPlayers() { return Collections.emptySet(); }
        @Override public boolean isConnected() { return true; }
        @Override public com.gitepildev.giteconomy.storage.TransferResult transfer(UUID fromUuid, UUID toUuid, String currency, double amount) { return com.gitepildev.giteconomy.storage.TransferResult.failure(0,0); }
        @Override public void shutdown() {}
        @Override public EconomyPlayer getPlayer(UUID uuid) { return new EconomyPlayer(uuid, "Name", null); }
    }

    // Top-level static test plugin class so MockBukkit can create a proxy for it
    public static class SimpleEz extends com.gitepildev.giteconomy.core.GitEconomyPlugin {
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
        GitEconomyPapiPlugin papi = (GitEconomyPapiPlugin) MockBukkit.load(GitEconomyPapiPlugin.class);

        // Load a simple GitEconomy core plugin stub (static nested class)
        com.gitepildev.giteconomy.core.GitEconomyPlugin core = (com.gitepildev.giteconomy.core.GitEconomyPlugin) MockBukkit.load(SimpleEz.class);

        // Install a test storage into the core plugin via reflection (setStorage is available)
        TestStorage ts = new TestStorage();
        java.util.UUID u = java.util.UUID.randomUUID();
        ts.put(u, 777.0);
        core.setStorage(ts);

        // Now ensure plugin manager maps "GitEconomy" to our core plugin so expansion picks it up
        org.bukkit.plugin.PluginManager pm = org.bukkit.Bukkit.getPluginManager();
        Field[] fields = pm.getClass().getDeclaredFields();
        for (Field f : fields) {
            if (Map.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                Object map = f.get(pm);
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, org.bukkit.plugin.Plugin> m = (Map<String, org.bukkit.plugin.Plugin>) map;
                    m.put("GitEconomy", core);
                } catch (ClassCastException ignored) {}
            }
        }

        GitEconomyPAPIExpansion expansion = new GitEconomyPAPIExpansion(papi);

        OfflinePlayer fake = TestPlayerFakes.fakeOfflinePlayer(u);

        // Call balance which should use core.getStorageOrWarn path
        String bal = expansion.onPlaceholderRequest(fake, "balance");
        assertNotNull(bal);

        // Call top_ which should schedule async refresh; we exercise the scheduling path
        String first = expansion.onPlaceholderRequest(null, "top_1_dollar");
        assertNotNull(first);
    }
}
