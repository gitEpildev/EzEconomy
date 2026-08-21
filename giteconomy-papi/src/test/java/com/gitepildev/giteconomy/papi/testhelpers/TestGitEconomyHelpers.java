package com.gitepildev.giteconomy.papi.testhelpers;

public final class TestGitEconomyHelpers {

    private TestGitEconomyHelpers() {}

    /**
     * Create a simple TestGitEconomy that uses the provided currency and symbol formatting.
     * StorageProvider is null (tests that use this should not rely on storage).
     */
    public static com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TestGitEconomy formatting(final String defaultCurrency, final String symbol) {
        return new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TestGitEconomy() {
            @Override public com.gitepildev.giteconomy.api.storage.StorageProvider getStorageOrWarn() { return null; }
            @Override public String getDefaultCurrency() { return defaultCurrency; }
            @Override public String format(double amount, String currency) { return String.format("%s%.2f", symbol, amount); }
            @Override public String formatShort(double amount, String currency) { return String.format("%s%.0f", symbol, amount); }
            @Override public String getCurrencySymbol(String currency) { return symbol; }
            @Override public com.gitepildev.giteconomy.manager.CurrencyPreferenceManager getCurrencyPreferenceManager() { return null; }
        };
    }

    /**
     * Create a TestGitEconomy that returns empty formatted strings and an empty currency symbol.
     * Useful for tests that assert fallback behavior when expansion returns blank values.
     */
    public static com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TestGitEconomy emptyFormatting(final String defaultCurrency) {
        return new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TestGitEconomy() {
            @Override public com.gitepildev.giteconomy.api.storage.StorageProvider getStorageOrWarn() { return null; }
            @Override public String getDefaultCurrency() { return defaultCurrency; }
            @Override public String format(double amount, String currency) { return ""; }
            @Override public String formatShort(double amount, String currency) { return ""; }
            @Override public String getCurrencySymbol(String currency) { return ""; }
            @Override public com.gitepildev.giteconomy.manager.CurrencyPreferenceManager getCurrencyPreferenceManager() { return null; }
        };
    }
}
