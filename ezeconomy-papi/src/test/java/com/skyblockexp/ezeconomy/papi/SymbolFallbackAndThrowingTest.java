package com.skyblockexp.ezeconomy.papi;

import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class SymbolFallbackAndThrowingTest {

    @Test
    public void symbol_fallback_on_throw_and_null_returns_dollar() throws Exception {
        MockBukkit.mock();
        EzEconomyPapiPlugin papi = (EzEconomyPapiPlugin) MockBukkit.load(EzEconomyPapiPlugin.class);

        // Test stub that throws from getCurrencySymbol
        EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new EzEconomyPAPIExpansion.TestEzEconomy() {
            @Override public com.skyblockexp.ezeconomy.api.storage.StorageProvider getStorageOrWarn() { return null; }
            @Override public String getDefaultCurrency() { return "dollar"; }
            @Override public String format(double amount, String currency) { return Double.toString(amount); }
            @Override public String formatShort(double amount, String currency) { return Double.toString(amount); }
            @Override public String getCurrencySymbol(String currency) { throw new RuntimeException("boom"); }
            @Override public com.skyblockexp.ezeconomy.manager.CurrencyPreferenceManager getCurrencyPreferenceManager() { return null; }
        };

        EzEconomyPAPIExpansion expansion = new EzEconomyPAPIExpansion(papi);

        try {
            // When underlying method throws for 'dollar', expansion should return '$' fallback
            String symbol = expansion.onPlaceholderRequest(null, "symbol_dollar");
            assertEquals("$", symbol);

            // If symbol method returns null explicitly, prefer $ for dollar as well
            EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new EzEconomyPAPIExpansion.TestEzEconomy() {
                @Override public com.skyblockexp.ezeconomy.api.storage.StorageProvider getStorageOrWarn() { return null; }
                @Override public String getDefaultCurrency() { return "dollar"; }
                @Override public String format(double amount, String currency) { return ""; }
                @Override public String formatShort(double amount, String currency) { return ""; }
                @Override public String getCurrencySymbol(String currency) { return null; }
                @Override public com.skyblockexp.ezeconomy.manager.CurrencyPreferenceManager getCurrencyPreferenceManager() { return null; }
            };
            String symbol2 = expansion.onPlaceholderRequest(null, "symbol_dollar");
            assertEquals("$", symbol2);
        } finally {
            EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = null;
            try { MockBukkit.unmock(); } catch (Exception ignored) {}
        }
    }
}
