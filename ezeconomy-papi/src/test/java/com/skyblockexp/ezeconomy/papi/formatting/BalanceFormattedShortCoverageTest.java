package com.skyblockexp.ezeconomy.papi.formatting;

import com.skyblockexp.ezeconomy.papi.testhelpers.TestEzEconomyStubs;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class BalanceFormattedShortCoverageTest extends com.skyblockexp.ezeconomy.papi.TestBase {

    @Test
    public void test_balance_formatted_and_short_using_testHook() {
        TestEzEconomyStubs.SimpleStorageProvider sp = new TestEzEconomyStubs.SimpleStorageProvider();
        UUID u = UUID.randomUUID();
        sp.setBalance(u, "eur", 123.45);
        sp.setBalance(u, "big", 1500.0);

        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new TestEzEconomyStubs.SimpleTestEz(sp, "usd");
        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion expansion = new com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion(null);

        OfflinePlayer fake = com.skyblockexp.ezeconomy.papi.testhelpers.TestPlayerFakes.fakeOfflinePlayer(u);

        String f = expansion.onPlaceholderRequest(fake, "balance_formatted_eur");
        assertNotNull(f);
        assertFalse(f.isEmpty());

        String s = expansion.onPlaceholderRequest(fake, "balance_short_big");
        assertNotNull(s);
        assertFalse(s.isEmpty());
    }

    @Test
    public void test_balance_formatted_and_short_using_ezPlugin_path() throws Exception {
        MockBukkit.mock();
        com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin papi = (com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin) MockBukkit.load(com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin.class);
        com.skyblockexp.ezeconomy.core.EzEconomyPlugin core = (com.skyblockexp.ezeconomy.core.EzEconomyPlugin) MockBukkit.load(com.skyblockexp.ezeconomy.papi.EzPluginPathCoverageTest.SimpleEz.class);

        TestEzEconomyStubs.SimpleStorageProvider sp = new TestEzEconomyStubs.SimpleStorageProvider();
        UUID u = UUID.randomUUID();
        sp.setBalance(u, "eur", 77.0);
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

        OfflinePlayer fake = com.skyblockexp.ezeconomy.papi.testhelpers.TestPlayerFakes.fakeOfflinePlayer(u);

        String f = expansion.onPlaceholderRequest(fake, "balance_formatted_eur");
        assertNotNull(f);

        String s = expansion.onPlaceholderRequest(fake, "balance_short_eur");
        assertNotNull(s);
    }
}
