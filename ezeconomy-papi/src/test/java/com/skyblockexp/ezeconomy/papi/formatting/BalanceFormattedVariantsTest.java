package com.skyblockexp.ezeconomy.papi.formatting;

import com.skyblockexp.ezeconomy.api.storage.StorageProvider;
import org.bukkit.OfflinePlayer;
import com.skyblockexp.ezeconomy.papi.testhelpers.TestPlayerFakes;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class BalanceFormattedVariantsTest extends com.skyblockexp.ezeconomy.papi.TestBase {

    static class SimpleStorage implements StorageProvider {
        private final Map<UUID, Double> balances = new HashMap<>();
        public void put(UUID u, double v) { balances.put(u, v); }
        @Override public void init() {}
        @Override public void load() {}
        @Override public void save() {}
        @Override public double getBalance(UUID uuid, String currency) { return balances.getOrDefault(uuid, 0.0); }
        @Override public void setBalance(UUID uuid, String currency, double amount) { balances.put(uuid, amount); }
        @Override public void logTransaction(com.skyblockexp.ezeconomy.api.storage.models.Transaction transaction) {}
        @Override public java.util.List<com.skyblockexp.ezeconomy.api.storage.models.Transaction> getTransactions(UUID uuid, String currency) { return java.util.Collections.emptyList(); }
        @Override public boolean tryWithdraw(UUID uuid, String currency, double amount) { return false; }
        @Override public void deposit(UUID uuid, String currency, double amount) {}
        @Override public Map<UUID, Double> getAllBalances(String currency) { return new HashMap<>(balances); }
        @Override public java.util.Set<String> cleanupOrphanedPlayers() { return java.util.Collections.emptySet(); }
        @Override public boolean isConnected() { return true; }
        @Override public com.skyblockexp.ezeconomy.storage.TransferResult transfer(UUID fromUuid, UUID toUuid, String currency, double amount) { return com.skyblockexp.ezeconomy.storage.TransferResult.failure(0,0); }
        @Override public void shutdown() {}
        @Override public com.skyblockexp.ezeconomy.dto.EconomyPlayer getPlayer(UUID uuid) { return new com.skyblockexp.ezeconomy.dto.EconomyPlayer(uuid, "User", null); }
    }

    @Test
    public void balance_formatted_with_explicit_currency_usesEzPluginPath() throws Exception {
        try { MockBukkit.mock(); } catch (IllegalStateException ignored) {}
        com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin papi = (com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin) MockBukkit.load(com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin.class);
        com.skyblockexp.ezeconomy.core.EzEconomyPlugin core = (com.skyblockexp.ezeconomy.core.EzEconomyPlugin) MockBukkit.load(com.skyblockexp.ezeconomy.papi.EzPluginPathCoverageTest.SimpleEz.class);

        SimpleStorage sp = new SimpleStorage();
        UUID u = UUID.randomUUID();
        sp.put(u, 1234.5);
        core.setStorage(sp);

        // Ensure plugin manager mapping
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

        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion expansion = new com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion(papi);

        OfflinePlayer fake = TestPlayerFakes.fakeOfflinePlayer(u);

        String out = expansion.onPlaceholderRequest(fake, "balance_formatted_eur");
        assertNotNull(out);
    }
}
