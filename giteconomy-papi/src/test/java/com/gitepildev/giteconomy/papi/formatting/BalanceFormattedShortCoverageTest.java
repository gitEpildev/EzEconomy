package com.gitepildev.giteconomy.papi.formatting;

import com.gitepildev.giteconomy.papi.testhelpers.TestGitEconomyStubs;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class BalanceFormattedShortCoverageTest extends com.gitepildev.giteconomy.papi.TestBase {

    @Test
    public void test_balance_formatted_and_short_using_testHook() {
        TestGitEconomyStubs.SimpleStorageProvider sp = new TestGitEconomyStubs.SimpleStorageProvider();
        UUID u = UUID.randomUUID();
        sp.setBalance(u, "eur", 123.45);
        sp.setBalance(u, "big", 1500.0);

        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new TestGitEconomyStubs.SimpleTestEz(sp, "usd");
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion expansion = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(null);

        OfflinePlayer fake = com.gitepildev.giteconomy.papi.testhelpers.TestPlayerFakes.fakeOfflinePlayer(u);

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
        com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin papi = (com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin) MockBukkit.load(com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin.class);
        com.gitepildev.giteconomy.core.GitEconomyPlugin core = (com.gitepildev.giteconomy.core.GitEconomyPlugin) MockBukkit.load(com.gitepildev.giteconomy.papi.EzPluginPathCoverageTest.SimpleEz.class);

        TestGitEconomyStubs.SimpleStorageProvider sp = new TestGitEconomyStubs.SimpleStorageProvider();
        UUID u = UUID.randomUUID();
        sp.setBalance(u, "eur", 77.0);
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

        OfflinePlayer fake = com.gitepildev.giteconomy.papi.testhelpers.TestPlayerFakes.fakeOfflinePlayer(u);

        String f = expansion.onPlaceholderRequest(fake, "balance_formatted_eur");
        assertNotNull(f);

        String s = expansion.onPlaceholderRequest(fake, "balance_short_eur");
        assertNotNull(s);
    }
}
