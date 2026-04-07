package com.skyblockexp.ezeconomy.papi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DebugExpansionTest {
    @Test
    public void debugSymbolFallback() {
        EzEconomyPAPIExpansion.TestEzEconomy stub = new EzEconomyPAPIExpansion.TestEzEconomy() {
            @Override public com.skyblockexp.ezeconomy.api.storage.StorageProvider getStorageOrWarn() { return null; }
            @Override public String getDefaultCurrency() { return "dollar"; }
            @Override public String format(double amount, String currency) { return String.format("%.2f %s", amount, currency); }
            @Override public String formatShort(double amount, String currency) { return String.format("%.1f%s", amount >= 1000 ? amount/1000.0 : amount, amount >= 1000 ? "K" : ""); }
            @Override public String getCurrencySymbol(String currency) { return "$"; }
            @Override public com.skyblockexp.ezeconomy.manager.CurrencyPreferenceManager getCurrencyPreferenceManager() { return null; }
        };

        EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = stub;
        EzEconomyPAPIExpansion exp = new EzEconomyPAPIExpansion(null);
        String sym = exp.onPlaceholderRequest(null, "symbol_dollar");
        System.out.println("DEBUG harness symbol -> '" + sym + "'");
        org.junit.jupiter.api.Assertions.assertNotNull(sym);
    }
}
