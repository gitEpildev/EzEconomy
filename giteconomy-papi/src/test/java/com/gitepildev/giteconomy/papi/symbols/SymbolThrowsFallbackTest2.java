package com.gitepildev.giteconomy.papi.symbols;

import com.gitepildev.giteconomy.papi.testhelpers.TestGitEconomyStubs;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SymbolThrowsFallbackTest2 {

    @Test
    public void symbol_dollar_fallbacks_to_dollar_sign_when_stub_throws() {
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TestGitEconomy orig = com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS;
        try {
            com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TestGitEconomy() {
                private final TestGitEconomyStubs.SimpleStorageProvider sp = new TestGitEconomyStubs.SimpleStorageProvider();
                @Override public com.gitepildev.giteconomy.api.storage.StorageProvider getStorageOrWarn() { return sp; }
                @Override public String getDefaultCurrency() { return "dollar"; }
                @Override public String format(double amount, String currency) { return String.format("%.2f %s", amount, currency); }
                @Override public String formatShort(double amount, String currency) { return format(amount, currency); }
                @Override public String getCurrencySymbol(String currency) { throw new RuntimeException("boom"); }
                @Override public com.gitepildev.giteconomy.manager.CurrencyPreferenceManager getCurrencyPreferenceManager() { return null; }
            };

            com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion expansion = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(null);
            String out = expansion.onPlaceholderRequest(null, "symbol_dollar");
            assertEquals("$", out);
        } finally {
            com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = orig;
        }
    }
}
