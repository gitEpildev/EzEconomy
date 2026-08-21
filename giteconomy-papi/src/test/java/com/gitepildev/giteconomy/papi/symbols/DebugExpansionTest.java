package com.gitepildev.giteconomy.papi.symbols;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DebugExpansionTest {
    @Test
    public void debugSymbolFallback() {
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TestGitEconomy stub = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TestGitEconomy() {
            @Override public com.gitepildev.giteconomy.api.storage.StorageProvider getStorageOrWarn() { return null; }
            @Override public String getDefaultCurrency() { return "dollar"; }
            @Override public String format(double amount, String currency) { return String.format("%.2f %s", amount, currency); }
            @Override public String formatShort(double amount, String currency) { return String.format("%.1f%s", amount >= 1000 ? amount/1000.0 : amount, amount >= 1000 ? "K" : ""); }
            @Override public String getCurrencySymbol(String currency) { return "$"; }
            @Override public com.gitepildev.giteconomy.manager.CurrencyPreferenceManager getCurrencyPreferenceManager() { return null; }
        };

        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = stub;
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion exp = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(null);
        String sym = exp.onPlaceholderRequest(null, "symbol_dollar");
        System.out.println("DEBUG harness symbol -> '" + sym + "'");
        org.junit.jupiter.api.Assertions.assertNotNull(sym);
    }
}
