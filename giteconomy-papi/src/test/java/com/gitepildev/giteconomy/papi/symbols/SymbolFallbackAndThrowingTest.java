package com.gitepildev.giteconomy.papi.symbols;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SymbolFallbackAndThrowingTest {

    @Test
    public void expansion_uses_fallback_symbol_when_ez_throws() {
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TestGitEconomy orig = com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS;
        try {
            com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TestGitEconomy() {
                @Override public com.gitepildev.giteconomy.api.storage.StorageProvider getStorageOrWarn() { return new com.gitepildev.giteconomy.papi.testhelpers.TestGitEconomyStubs.SimpleStorageProvider(); }
                @Override public String getDefaultCurrency() { return "dollar"; }
                @Override public String format(double amount, String currency) { return String.format("%.2f %s", amount, currency); }
                @Override public String formatShort(double amount, String currency) { return format(amount, currency); }
                @Override public String getCurrencySymbol(String currency) { throw new RuntimeException("no symbol"); }
                @Override public com.gitepildev.giteconomy.manager.CurrencyPreferenceManager getCurrencyPreferenceManager() { return null; }
            };

            com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion expansion = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(null);
            assertEquals("$", expansion.onPlaceholderRequest(null, "symbol_dollar"));
        } finally {
            com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = orig;
        }
    }
}
