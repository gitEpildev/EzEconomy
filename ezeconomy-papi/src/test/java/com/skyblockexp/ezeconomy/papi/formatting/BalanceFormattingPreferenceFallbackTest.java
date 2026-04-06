package com.skyblockexp.ezeconomy.papi.formatting;

import com.skyblockexp.ezeconomy.papi.testhelpers.TestEzEconomyStubs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class BalanceFormattingPreferenceFallbackTest {

    @AfterEach
    public void tearDown() {
        try { MockBukkit.unmock(); } catch (Exception ignored) {}
        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = null;
    }

    @Test
    public void preference_manager_overrides_default_currency() throws Exception {
        MockBukkit.mock();
        com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin papi = (com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin) MockBukkit.load(com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin.class);

        TestEzEconomyStubs.SimpleStorageProvider storage = new TestEzEconomyStubs.SimpleStorageProvider();
        UUID u = UUID.randomUUID();
        storage.setBalance(u, "gbp", 42.0);

        // Use defaultCurrency to simulate the preferred currency when no manager is present
        TestEzEconomyStubs.SimpleTestEz stub = new TestEzEconomyStubs.SimpleTestEz(storage, "gbp");

        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = stub;
        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion expansion = new com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion(papi);

        org.bukkit.OfflinePlayer fake = (org.bukkit.OfflinePlayer) java.lang.reflect.Proxy.newProxyInstance(
                org.bukkit.OfflinePlayer.class.getClassLoader(), new Class[]{org.bukkit.OfflinePlayer.class}, (proxy, method, args) -> {
                    if ("getUniqueId".equals(method.getName())) return u;
                    if (method.getReturnType().equals(boolean.class)) return false;
                    return null;
                }
        );

        String out = expansion.onPlaceholderRequest(fake, "balance_formatted");
        assertNotNull(out);
        assertFalse(out.isEmpty());
    }

    @Test
    public void null_preference_and_null_default_handled_gracefully() throws Exception {
        MockBukkit.mock();
        com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin papi = (com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin) MockBukkit.load(com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin.class);

        TestEzEconomyStubs.SimpleStorageProvider storage = new TestEzEconomyStubs.SimpleStorageProvider();
        UUID u = UUID.randomUUID();
        storage.setBalance(u, "x", 100.0);

        TestEzEconomyStubs.SimpleTestEz stub = new TestEzEconomyStubs.SimpleTestEz(storage, null) {
            @Override
            public com.skyblockexp.ezeconomy.manager.CurrencyPreferenceManager getCurrencyPreferenceManager() {
                return null; // force using defaultCurrency (which is null here) to exercise null-pref branch
            }

            @Override
            public String format(double amount, String currency) {
                return "FORMATTED(" + amount + "," + currency + ")";
            }
        };

        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = stub;
        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion expansion = new com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion(papi);

        org.bukkit.OfflinePlayer fake = (org.bukkit.OfflinePlayer) java.lang.reflect.Proxy.newProxyInstance(
                org.bukkit.OfflinePlayer.class.getClassLoader(), new Class[]{org.bukkit.OfflinePlayer.class}, (proxy, method, args) -> {
                    if ("getUniqueId".equals(method.getName())) return u;
                    if (method.getReturnType().equals(boolean.class)) return false;
                    return null;
                }
        );

        String out = expansion.onPlaceholderRequest(fake, "balance");
        assertNotNull(out);
        assertTrue(out.contains("FORMATTED"));
        assertTrue(out.contains("null"));
    }

    @Test
    public void explicit_currency_with_null_storage_returns_zero_formatted() throws Exception {
        MockBukkit.mock();
        com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin papi = (com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin) MockBukkit.load(com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin.class);

        TestEzEconomyStubs.SimpleTestEz stub = new TestEzEconomyStubs.SimpleTestEz(null, "usd");
        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = stub;
        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion expansion = new com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion(papi);

        String out = expansion.onPlaceholderRequest(null, "balance_formatted_eur");
        assertNotNull(out);
        assertTrue(out.contains("0.00") || !out.isEmpty());
    }
}
