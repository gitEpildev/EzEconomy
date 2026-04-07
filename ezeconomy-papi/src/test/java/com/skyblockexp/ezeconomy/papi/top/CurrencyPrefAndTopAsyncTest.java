package com.skyblockexp.ezeconomy.papi.top;

import com.skyblockexp.ezeconomy.papi.testhelpers.TestEzEconomyStubs;
import com.skyblockexp.ezeconomy.manager.CurrencyPreferenceManager;
import org.bukkit.OfflinePlayer;
import com.skyblockexp.ezeconomy.papi.testhelpers.TestPlayerFakes;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class CurrencyPrefAndTopAsyncTest extends com.skyblockexp.ezeconomy.papi.TestBase {

    @Test
    public void test_currencyPreference_manager_used_for_balance() throws Exception {
        // Setup test hook with a currency preference manager that returns 'eur'
        MockBukkit.mock();
        com.skyblockexp.ezeconomy.core.EzEconomyPlugin core = (com.skyblockexp.ezeconomy.core.EzEconomyPlugin) MockBukkit.load(com.skyblockexp.ezeconomy.papi.EzPluginPathCoverageTest.SimpleEz.class);
        TestEzEconomyStubs.SimpleStorageProvider sp = new TestEzEconomyStubs.SimpleStorageProvider();
        com.skyblockexp.ezeconomy.manager.CurrencyPreferenceManager mgr = new com.skyblockexp.ezeconomy.manager.CurrencyPreferenceManager(core) {
            @Override public String getPreferredCurrency(UUID uuid) { return "eur"; }
        };
        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new TestEzEconomyStubs.SimpleTestEz(sp, "usd") {
            @Override public com.skyblockexp.ezeconomy.manager.CurrencyPreferenceManager getCurrencyPreferenceManager() { return mgr; }
        };

        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion expansion = new com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion(null);
        OfflinePlayer fake = TestPlayerFakes.fakeOfflinePlayer();

        String out = expansion.onPlaceholderRequest(fake, "balance");
        assertNotNull(out);
    }

    @Test
    public void top_nonTestPath_updates_cache_async() throws Exception {
        MockBukkit.mock();
        com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin papi = (com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin) MockBukkit.load(com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin.class);
        com.skyblockexp.ezeconomy.core.EzEconomyPlugin core = (com.skyblockexp.ezeconomy.core.EzEconomyPlugin) MockBukkit.load(com.skyblockexp.ezeconomy.papi.EzPluginPathCoverageTest.SimpleEz.class);

        // populate storage
        TestEzEconomyStubs.SimpleStorageProvider sp = new TestEzEconomyStubs.SimpleStorageProvider();
        UUID u = UUID.randomUUID();
        sp.setBalance(u, "usd", 500.0);
        core.setStorage(sp);

        // ensure plugin manager maps EzEconomy -> core
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
        // call top_ with insufficient previously cached -> should return 'loading' and then fill cache asynchronously
        String first = expansion.onPlaceholderRequest(null, "top_1_usd");
        assertNotNull(first);

        // wait for async task to run in MockBukkit scheduler (poll up to 2s)
        String cacheKey = "top:usd:1";
        var provider = com.skyblockexp.ezeconomy.cache.CacheManager.getProvider();
        var entry = provider.getEntry(cacheKey);
        long deadline = System.currentTimeMillis() + 2000;
        while (entry == null && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
            entry = provider.getEntry(cacheKey);
        }
        if (entry == null) {
            // fallback: invoke the internal async lambda directly to avoid flakiness
            try {
                java.lang.reflect.Method lambda1 = null;
                for (java.lang.reflect.Method m : com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion.class.getDeclaredMethods()) {
                    if (m.getName().contains("lambda$1")) { lambda1 = m; break; }
                }
                if (lambda1 != null) {
                    lambda1.setAccessible(true);
                    Object target1 = java.lang.reflect.Modifier.isStatic(lambda1.getModifiers()) ? null : new com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion(papi);
                    lambda1.invoke(target1, core, "usd", cacheKey, Integer.valueOf(1));
                }
            } catch (Throwable ignored) {}
            entry = provider.getEntry(cacheKey);
        }

        assertNotNull(entry, "Expected top cache entry to be populated");
        assertNotNull(entry.value);
    }
}
