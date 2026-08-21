package com.gitepildev.giteconomy.papi.lambda;

import com.gitepildev.giteconomy.api.storage.StorageProvider;
import com.gitepildev.giteconomy.cache.CacheManager;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class Lambda1EdgeCasesTest extends com.gitepildev.giteconomy.papi.TestBase {

    static class StorageNullAll implements StorageProvider {
        @Override public void init() {}
        @Override public void load() {}
        @Override public void save() {}
        @Override public double getBalance(UUID uuid, String currency) { return 0; }
        @Override public void setBalance(UUID uuid, String currency, double amount) {}
        @Override public void logTransaction(com.gitepildev.giteconomy.api.storage.models.Transaction transaction) {}
        @Override public java.util.List<com.gitepildev.giteconomy.api.storage.models.Transaction> getTransactions(UUID uuid, String currency) { return java.util.Collections.emptyList(); }
        @Override public boolean tryWithdraw(UUID uuid, String currency, double amount) { return false; }
        @Override public void deposit(UUID uuid, String currency, double amount) {}
        @Override public Map<UUID, Double> getAllBalances(String currency) { return null; }
        @Override public java.util.Set<String> cleanupOrphanedPlayers() { return java.util.Collections.emptySet(); }
        @Override public boolean isConnected() { return true; }
        @Override public com.gitepildev.giteconomy.storage.TransferResult transfer(UUID fromUuid, UUID toUuid, String currency, double amount) { return com.gitepildev.giteconomy.storage.TransferResult.failure(0,0); }
        @Override public void shutdown() {}
        @Override public com.gitepildev.giteconomy.dto.EconomyPlayer getPlayer(UUID uuid) { return null; }
    }

    static class StorageEmptyAll implements StorageProvider {
        private final Map<UUID, Double> data = new HashMap<>();
        @Override public void init() {}
        @Override public void load() {}
        @Override public void save() {}
        @Override public double getBalance(UUID uuid, String currency) { return data.getOrDefault(uuid, 0.0); }
        @Override public void setBalance(UUID uuid, String currency, double amount) { data.put(uuid, amount); }
        @Override public void logTransaction(com.gitepildev.giteconomy.api.storage.models.Transaction transaction) {}
        @Override public java.util.List<com.gitepildev.giteconomy.api.storage.models.Transaction> getTransactions(UUID uuid, String currency) { return java.util.Collections.emptyList(); }
        @Override public boolean tryWithdraw(UUID uuid, String currency, double amount) { return false; }
        @Override public void deposit(UUID uuid, String currency, double amount) {}
        @Override public Map<UUID, Double> getAllBalances(String currency) { return new HashMap<>(data); }
        @Override public java.util.Set<String> cleanupOrphanedPlayers() { return java.util.Collections.emptySet(); }
        @Override public boolean isConnected() { return true; }
        @Override public com.gitepildev.giteconomy.storage.TransferResult transfer(UUID fromUuid, UUID toUuid, String currency, double amount) { return com.gitepildev.giteconomy.storage.TransferResult.failure(0,0); }
        @Override public void shutdown() {}
        @Override public com.gitepildev.giteconomy.dto.EconomyPlayer getPlayer(UUID uuid) { return null; }
    }

    @Test
    public void lambda1_writes_empty_for_null_storage() throws Exception {
        try { MockBukkit.mock(); } catch (IllegalStateException ignored) {}
        com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin papi = (com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin) MockBukkit.load(com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin.class);
        com.gitepildev.giteconomy.core.GitEconomyPlugin core = (com.gitepildev.giteconomy.core.GitEconomyPlugin) MockBukkit.load(com.gitepildev.giteconomy.papi.EzPluginPathCoverageTest.SimpleEz.class);

        core.setStorage(null);

        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion expansion = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(papi);

        String currency = "edgecase_currency_for_tests";
        String cacheKey = "top:edgecase_currency_for_tests:1";

        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.computeTopSyncForProd(core, currency, cacheKey, 1);

        var entry = CacheManager.getProvider().getEntry(cacheKey);
        assertNotNull(entry);
        assertEquals("", entry.value);
    }

    @Test
    public void lambda1_handles_null_and_empty_allBalances() throws Exception {
        try { MockBukkit.mock(); } catch (IllegalStateException ignored) {}
        com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin papi = (com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin) MockBukkit.load(com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin.class);
        com.gitepildev.giteconomy.core.GitEconomyPlugin core = (com.gitepildev.giteconomy.core.GitEconomyPlugin) MockBukkit.load(com.gitepildev.giteconomy.papi.EzPluginPathCoverageTest.SimpleEz.class);

        StorageNullAll sNull = new StorageNullAll();
        core.setStorage(sNull);

        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion expansion = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(papi);

        String currency = "edgecase_currency_for_tests";
        String cacheKey = "top:edgecase_currency_for_tests:2";

        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.computeTopSyncForProd(core, currency, cacheKey, 2);

        var entry = CacheManager.getProvider().getEntry(cacheKey);
        assertNotNull(entry);
        assertEquals("", entry.value);

        StorageEmptyAll sEmpty = new StorageEmptyAll();
        core.setStorage(sEmpty);
        String cacheKey2 = "top:edgecase_currency_for_tests:3";
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.computeTopSyncForProd(core, currency, cacheKey2, 3);
        var entry2 = CacheManager.getProvider().getEntry(cacheKey2);
        assertNotNull(entry2);
        assertEquals("", entry2.value);
    }
}
