package com.skyblockexp.ezeconomy.papi;

import com.skyblockexp.ezeconomy.papi.testhelpers.TestEzEconomyStubs;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SymbolFallbackTest {

    @Test
    public void symbol_dollar_fallbacks_to_dollar_sign_when_stub_throws() {
        EzEconomyPAPIExpansion.TestEzEconomy orig = EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS;
        try {
            EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new EzEconomyPAPIExpansion.TestEzEconomy() {
                private final TestEzEconomyStubs.SimpleStorageProvider sp = new TestEzEconomyStubs.SimpleStorageProvider();
                @Override public com.skyblockexp.ezeconomy.api.storage.StorageProvider getStorageOrWarn() { return sp; }
                @Override public String getDefaultCurrency() { return "dollar"; }
                @Override public String format(double amount, String currency) { return String.format("%.2f %s", amount, currency); }
                @Override public String formatShort(double amount, String currency) { return format(amount, currency); }
                @Override public String getCurrencySymbol(String currency) { throw new RuntimeException("boom"); }
                @Override public com.skyblockexp.ezeconomy.manager.CurrencyPreferenceManager getCurrencyPreferenceManager() { return null; }
            };

            EzEconomyPAPIExpansion expansion = new EzEconomyPAPIExpansion(null);
            String out = expansion.onPlaceholderRequest(null, "symbol_dollar");
            assertEquals("$", out);
        } finally {
            EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = orig;
        }
    }
}
