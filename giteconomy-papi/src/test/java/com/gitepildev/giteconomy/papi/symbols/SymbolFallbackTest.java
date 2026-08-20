package com.gitepildev.giteconomy.papi.symbols;

import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.*;

public class SymbolFallbackTest extends com.gitepildev.giteconomy.papi.TestBase {

    @Test
    public void symbol_fallback_to_default_when_unset() {
        MockBukkit.mock();
        String result = null;
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TestGitEconomy orig = com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS;
        try {
            com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TestGitEconomy() {
                @Override public com.gitepildev.giteconomy.api.storage.StorageProvider getStorageOrWarn() { return new com.gitepildev.giteconomy.papi.testhelpers.TestGitEconomyStubs.SimpleStorageProvider(); }
                @Override public String getDefaultCurrency() { return "dollar"; }
                @Override public String format(double amount, String currency) { return String.format("%.2f %s", amount, currency); }
                @Override public String formatShort(double amount, String currency) { return format(amount, currency); }
                @Override public String getCurrencySymbol(String currency) { return "$"; }
                @Override public com.gitepildev.giteconomy.manager.CurrencyPreferenceManager getCurrencyPreferenceManager() { return null; }
            };

            com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion expansion = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(null);

                result = expansion.onPlaceholderRequest(null, "symbol_dollar");
        } finally {
            com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = orig;
        }
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }
}
