package com.gitepildev.giteconomy.papi.symbols;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SymbolDollarDefaultTest {

    @Test
    public void default_symbol_for_dollar_is_dollar_sign() {
        org.mockbukkit.mockbukkit.MockBukkit.mock();
        try {
            com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin papi = (com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin) org.mockbukkit.mockbukkit.MockBukkit.load(com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin.class);
            com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TestGitEconomy orig = com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS;
            try {
                com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TestGitEconomy() {
                    private final com.gitepildev.giteconomy.papi.testhelpers.TestGitEconomyStubs.SimpleStorageProvider sp = new com.gitepildev.giteconomy.papi.testhelpers.TestGitEconomyStubs.SimpleStorageProvider();
                    @Override public com.gitepildev.giteconomy.api.storage.StorageProvider getStorageOrWarn() { return sp; }
                    @Override public String getDefaultCurrency() { return "dollar"; }
                    @Override public String format(double amount, String currency) { return String.format("%.2f %s", amount, currency); }
                    @Override public String formatShort(double amount, String currency) { return format(amount, currency); }
                    @Override public String getCurrencySymbol(String currency) { return "$"; }
                    @Override public com.gitepildev.giteconomy.manager.CurrencyPreferenceManager getCurrencyPreferenceManager() { return null; }
                };

                com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion expansion = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(papi);
                String out = expansion.onPlaceholderRequest(null, "symbol_dollar");
                assertEquals("$", out);
            } finally {
                com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = orig;
            }
        } finally {
            try { org.mockbukkit.mockbukkit.MockBukkit.unmock(); } catch (Exception ignored) {}
        }
    }
}
