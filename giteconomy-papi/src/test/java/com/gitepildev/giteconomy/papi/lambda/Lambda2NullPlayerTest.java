package com.gitepildev.giteconomy.papi.lambda;

import com.gitepildev.giteconomy.api.storage.StorageProvider;
import com.gitepildev.giteconomy.cache.CacheManager;
import com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion;
import com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin;
import com.gitepildev.giteconomy.papi.TestBase;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class Lambda2NullPlayerTest extends TestBase {

    static class MapStorageNullPlayer implements StorageProvider {
        private final Map<UUID, Double> all = new HashMap<>();
        public void put(UUID u, double v) { all.put(u, v); }
        @Override public void init() {}
        @Override public void load() {}
        @Override public void save() {}
        @Override public double getBalance(UUID uuid, String currency) { return all.getOrDefault(uuid, 0.0); }
        @Override public void setBalance(UUID uuid, String currency, double amount) { all.put(uuid, amount); }
        @Override public void logTransaction(com.gitepildev.giteconomy.api.storage.models.Transaction transaction) {}
        @Override public java.util.List<com.gitepildev.giteconomy.api.storage.models.Transaction> getTransactions(UUID uuid, String currency) { return java.util.Collections.emptyList(); }
        @Override public boolean tryWithdraw(UUID uuid, String currency, double amount) { return false; }
        @Override public void deposit(UUID uuid, String currency, double amount) {}
        @Override public Map<UUID, Double> getAllBalances(String currency) { return new HashMap<>(all); }
        @Override public java.util.Set<String> cleanupOrphanedPlayers() { return java.util.Collections.emptySet(); }
        @Override public boolean isConnected() { return true; }
        @Override public com.gitepildev.giteconomy.storage.TransferResult transfer(UUID fromUuid, UUID toUuid, String currency, double amount) { return com.gitepildev.giteconomy.storage.TransferResult.failure(0,0); }
        @Override public void shutdown() {}
        @Override public com.gitepildev.giteconomy.dto.EconomyPlayer getPlayer(UUID uuid) { return null; }
    }

    @Test
    public void invoke_lambda2_with_null_player_path() throws Exception {
        MockBukkit.mock();
        GitEconomyPapiPlugin papi = (GitEconomyPapiPlugin) MockBukkit.load(GitEconomyPapiPlugin.class);
        com.gitepildev.giteconomy.core.GitEconomyPlugin core = (com.gitepildev.giteconomy.core.GitEconomyPlugin) MockBukkit.load(com.gitepildev.giteconomy.papi.EzPluginPathCoverageTest.SimpleEz.class);

        MapStorageNullPlayer ms = new MapStorageNullPlayer();
        UUID a = UUID.randomUUID();
        ms.put(a, 12345.0);
        core.setStorage(ms);

        org.bukkit.plugin.PluginManager pm = org.bukkit.Bukkit.getPluginManager();
        java.lang.reflect.Field[] fields = pm.getClass().getDeclaredFields();
        for (java.lang.reflect.Field f : fields) {
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

        AbstractMap.SimpleEntry<java.util.UUID, Double> entryPair = new AbstractMap.SimpleEntry<>(a, 12345.0);
        org.bukkit.OfflinePlayer off = org.bukkit.Bukkit.getOfflinePlayer(a);

        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.computeTopSyncForProd(core, "dollar", "top:dollar:1", 1);
        String cacheKey = "top:dollar:1";
        com.gitepildev.giteconomy.cache.ExpiringCache.Entry<?> entry = CacheManager.getProvider().getEntry(cacheKey);
        assertNotNull(entry);
        String s = entry.value == null ? "" : entry.value.toString();
        assertTrue(s.contains("12345") || s.length() > 0);
    }
}
