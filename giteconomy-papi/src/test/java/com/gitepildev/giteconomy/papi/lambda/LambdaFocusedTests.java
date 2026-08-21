package com.gitepildev.giteconomy.papi.lambda;

import com.gitepildev.giteconomy.api.storage.StorageProvider;
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

public class LambdaFocusedTests extends TestBase {

    static class SimpleStorage implements StorageProvider {
        private final Map<UUID, Double> balances = new HashMap<>();
        public void put(UUID u, double v) { balances.put(u, v); }
        @Override public void init() {}
        @Override public void load() {}
        @Override public void save() {}
        @Override public double getBalance(UUID uuid, String currency) { return balances.getOrDefault(uuid, 0.0); }
        @Override public void setBalance(UUID uuid, String currency, double amount) { balances.put(uuid, amount); }
        @Override public void logTransaction(com.gitepildev.giteconomy.api.storage.models.Transaction transaction) {}
        @Override public java.util.List<com.gitepildev.giteconomy.api.storage.models.Transaction> getTransactions(UUID uuid, String currency) { return java.util.Collections.emptyList(); }
        @Override public boolean tryWithdraw(UUID uuid, String currency, double amount) { return false; }
        @Override public void deposit(UUID uuid, String currency, double amount) {}
        @Override public Map<UUID, Double> getAllBalances(String currency) { return new HashMap<>(balances); }
        @Override public java.util.Set<String> cleanupOrphanedPlayers() { return java.util.Collections.emptySet(); }
        @Override public boolean isConnected() { return true; }
        @Override public com.gitepildev.giteconomy.storage.TransferResult transfer(UUID fromUuid, UUID toUuid, String currency, double amount) { return com.gitepildev.giteconomy.storage.TransferResult.failure(0,0); }
        @Override public void shutdown() {}
        @Override public com.gitepildev.giteconomy.dto.EconomyPlayer getPlayer(UUID uuid) { return new com.gitepildev.giteconomy.dto.EconomyPlayer(uuid, "User", null); }
    }

    @Test
    public void invoke_lambda1_with_and_without_storage() throws Exception {
        MockBukkit.mock();
        com.gitepildev.giteconomy.core.GitEconomyPlugin core = (com.gitepildev.giteconomy.core.GitEconomyPlugin) MockBukkit.load(com.gitepildev.giteconomy.papi.EzPluginPathCoverageTest.SimpleEz.class);
        GitEconomyPapiPlugin papi = (GitEconomyPapiPlugin) MockBukkit.load(GitEconomyPapiPlugin.class);
        GitEconomyPAPIExpansion expansion = new GitEconomyPAPIExpansion(papi);

        java.util.UUID u1 = java.util.UUID.randomUUID();
        org.bukkit.OfflinePlayer off1 = org.bukkit.Bukkit.getOfflinePlayer(u1);

        core.setStorage(null);
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.computeTopSyncForProd(core, "usd", "top:usd:1", 1);

        SimpleStorage ss = new SimpleStorage();
        core.setStorage(ss);
        expansion.handlePlaceholderRequestForTests(off1, "top_1_usd");
    }

    @Test
    public void invoke_lambda2_name_fallbacks() throws Exception {
        MockBukkit.mock();
        com.gitepildev.giteconomy.core.GitEconomyPlugin core = (com.gitepildev.giteconomy.core.GitEconomyPlugin) MockBukkit.load(com.gitepildev.giteconomy.papi.EzPluginPathCoverageTest.SimpleEz.class);
        GitEconomyPapiPlugin papi = (GitEconomyPapiPlugin) MockBukkit.load(GitEconomyPapiPlugin.class);
        GitEconomyPAPIExpansion expansion = new GitEconomyPAPIExpansion(papi);

        AbstractMap.SimpleEntry<java.util.UUID, Double> entry = new AbstractMap.SimpleEntry<>(java.util.UUID.randomUUID(), 42.0);
        org.bukkit.OfflinePlayer off2 = org.bukkit.Bukkit.getOfflinePlayer(entry.getKey());

        core.setStorage(null);
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.computeTopSyncForProd(core, "usd", "top:usd:1", 1);
        com.gitepildev.giteconomy.cache.ExpiringCache.Entry<?> e1 = com.gitepildev.giteconomy.cache.CacheManager.getProvider().getEntry("top:usd:1");
        assertNotNull(e1);

        SimpleStorage ss2 = new SimpleStorage();
        core.setStorage(ss2);
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.computeTopSyncForProd(core, "usd", "top:usd:1", 1);
        com.gitepildev.giteconomy.cache.ExpiringCache.Entry<?> e2 = com.gitepildev.giteconomy.cache.CacheManager.getProvider().getEntry("top:usd:1");
        assertNotNull(e2);
    }
}
