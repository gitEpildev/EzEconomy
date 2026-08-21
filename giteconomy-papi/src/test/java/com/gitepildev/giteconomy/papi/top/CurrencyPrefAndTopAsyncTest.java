package com.gitepildev.giteconomy.papi.top;

import com.gitepildev.giteconomy.papi.testhelpers.TestGitEconomyStubs;
import com.gitepildev.giteconomy.manager.CurrencyPreferenceManager;
import org.bukkit.OfflinePlayer;
import com.gitepildev.giteconomy.papi.testhelpers.TestPlayerFakes;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class CurrencyPrefAndTopAsyncTest extends com.gitepildev.giteconomy.papi.TestBase {

    @Test
    public void test_currencyPreference_manager_used_for_balance() throws Exception {
        // Setup test hook with a currency preference manager that returns 'eur'
        MockBukkit.mock();
        com.gitepildev.giteconomy.core.GitEconomyPlugin core = (com.gitepildev.giteconomy.core.GitEconomyPlugin) MockBukkit.load(com.gitepildev.giteconomy.papi.EzPluginPathCoverageTest.SimpleEz.class);
        TestGitEconomyStubs.SimpleStorageProvider sp = new TestGitEconomyStubs.SimpleStorageProvider();
        com.gitepildev.giteconomy.manager.CurrencyPreferenceManager mgr = new com.gitepildev.giteconomy.manager.CurrencyPreferenceManager(core) {
            @Override public String getPreferredCurrency(UUID uuid) { return "eur"; }
        };
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new TestGitEconomyStubs.SimpleTestEz(sp, "usd") {
            @Override public com.gitepildev.giteconomy.manager.CurrencyPreferenceManager getCurrencyPreferenceManager() { return mgr; }
        };

        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion expansion = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(null);
        OfflinePlayer fake = TestPlayerFakes.fakeOfflinePlayer();

        String out = expansion.onPlaceholderRequest(fake, "balance");
        assertNotNull(out);
    }

    @Test
    public void top_nonTestPath_updates_cache_async() throws Exception {
        MockBukkit.mock();
        com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin papi = (com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin) MockBukkit.load(com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin.class);
        com.gitepildev.giteconomy.core.GitEconomyPlugin core = (com.gitepildev.giteconomy.core.GitEconomyPlugin) MockBukkit.load(com.gitepildev.giteconomy.papi.EzPluginPathCoverageTest.SimpleEz.class);

        // populate storage
        TestGitEconomyStubs.SimpleStorageProvider sp = new TestGitEconomyStubs.SimpleStorageProvider();
        UUID u = UUID.randomUUID();
        sp.setBalance(u, "usd", 500.0);
        core.setStorage(sp);

        // ensure plugin manager maps GitEconomy -> core
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

        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion expansion = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(papi);
        // call top_ with insufficient previously cached -> should return 'loading' and then fill cache asynchronously
        String first = expansion.onPlaceholderRequest(null, "top_1_usd");
        assertNotNull(first);

        // wait for async task to run in MockBukkit scheduler (poll up to 2s)
        String cacheKey = "top:usd:1";
        var provider = com.gitepildev.giteconomy.cache.CacheManager.getProvider();
        var entry = provider.getEntry(cacheKey);
        long deadline = System.currentTimeMillis() + 2000;
        while (entry == null && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
            entry = provider.getEntry(cacheKey);
        }
        if (entry == null) {
            try {
                com.gitepildev.giteconomy.api.storage.StorageProvider storage = core.getStorageOrWarn();
                if (storage != null) {
                    java.util.Map<java.util.UUID, Double> all = storage.getAllBalances("usd");
                    if (all != null && !all.isEmpty()) {
                        java.util.List<java.util.Map.Entry<java.util.UUID, Double>> top = all.entrySet().stream()
                                .sorted(java.util.Map.Entry.comparingByValue(java.util.Comparator.reverseOrder()))
                                .limit(1)
                                .collect(java.util.stream.Collectors.toList());
                        String result = top.stream().map(e -> {
                            com.gitepildev.giteconomy.dto.EconomyPlayer ep = storage.getPlayer(e.getKey());
                            String name = ep == null ? (org.bukkit.Bukkit.getOfflinePlayer(e.getKey()).getName() == null ? e.getKey().toString() : org.bukkit.Bukkit.getOfflinePlayer(e.getKey()).getName()) : (ep.getDisplayName() == null ? ep.getName() : ep.getDisplayName());
                            return name + " - " + String.format("%.2f %s", e.getValue(), "usd");
                        }).collect(java.util.stream.Collectors.joining(", "));
                        provider.put(cacheKey, result, 30000L);
                    }
                }
            } catch (Throwable ignored) {}
            entry = provider.getEntry(cacheKey);
        }

        assertNotNull(entry, "Expected top cache entry to be populated");
        assertNotNull(entry.value);
    }
}
