package com.skyblockexp.ezeconomy.papi.top;

import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TopInlineComplexTest {

    @Test
    public void top_inline_assembly_exercises_lambda_paths() throws Exception {
        MockBukkit.mock();
        com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin papi = (com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin) MockBukkit.load(com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin.class);

        // Build a TestEzEconomy that returns a storage with multiple balances
        final UUID a = UUID.randomUUID();
        final UUID b = UUID.randomUUID();

        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion.TestEzEconomy() {
            final com.skyblockexp.ezeconomy.papi.EzPluginPathCoverageTest.TestStorage ts = new com.skyblockexp.ezeconomy.papi.EzPluginPathCoverageTest.TestStorage();
            {
                ts.put(a, 500.0);
                ts.put(b, 300.0);
            }
            @Override public com.skyblockexp.ezeconomy.api.storage.StorageProvider getStorageOrWarn() { return ts; }
            @Override public String getDefaultCurrency() { return "dollar"; }
            @Override public String format(double amount, String currency) { return "FMT:" + ((int) amount); }
            @Override public String formatShort(double amount, String currency) { return "SHRT:" + ((int) amount); }
            @Override public String getCurrencySymbol(String currency) { return "$"; }
            @Override public com.skyblockexp.ezeconomy.manager.CurrencyPreferenceManager getCurrencyPreferenceManager() { return null; }
        };

        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion expansion = new com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion(papi);

        try {
            // First call may return the "loading" placeholder while inline refresh runs.
            // Poll until the cache is populated and expansion returns the assembled result.
            String result = null;
            long deadline = System.currentTimeMillis() + 1000;
            while (System.currentTimeMillis() < deadline) {
                result = expansion.onPlaceholderRequest(null, "top_2_dollar");
                if (result != null && result.contains("FMT:")) break;
                Thread.sleep(50);
            }
            assertNotNull(result);
            // Result should contain two entries joined by comma and each with a ' - ' separator
            assertTrue(result.contains(","), "expected comma-separated entries in: " + result);
            assertTrue(result.contains(" - "), "expected name/amount separator in: " + result);

            // Calling again should hit the cache and return same value
            String second = expansion.onPlaceholderRequest(null, "top_2_dollar");
            assertEquals(result, second);
        } finally {
            com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = null;
            try { MockBukkit.unmock(); } catch (Exception ignored) {}
        }
    }
}

