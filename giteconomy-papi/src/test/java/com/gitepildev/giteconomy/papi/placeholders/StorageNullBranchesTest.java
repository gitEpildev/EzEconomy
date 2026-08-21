package com.gitepildev.giteconomy.papi.placeholders;

import com.gitepildev.giteconomy.papi.testhelpers.TestGitEconomyStubs;
import com.gitepildev.giteconomy.papi.testhelpers.TestPlayerFakes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StorageNullBranchesTest {

    @Test
    public void storage_null_paths_return_expected_defaults() {
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TestGitEconomy orig = com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS;
        try {
            com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TestGitEconomy() {
                @Override public com.gitepildev.giteconomy.api.storage.StorageProvider getStorageOrWarn() { return null; }
                @Override public String getDefaultCurrency() { return "dollar"; }
                @Override public String format(double amount, String currency) { return String.format("%.2f %s", amount, currency); }
                @Override public String formatShort(double amount, String currency) { return format(amount, currency); }
                @Override public String getCurrencySymbol(String currency) { return null; }
                @Override public com.gitepildev.giteconomy.manager.CurrencyPreferenceManager getCurrencyPreferenceManager() { return null; }
            };

            com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion expansion = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(null);

            java.util.UUID u = java.util.UUID.randomUUID();
            org.bukkit.OfflinePlayer fakePlayer = TestPlayerFakes.fakeOfflinePlayer(u);

            assertEquals("0.00 dollar", expansion.onPlaceholderRequest(fakePlayer, "balance"));
            assertEquals("0.00 eur", expansion.onPlaceholderRequest(fakePlayer, "balance_eur"));
            assertEquals("0.00 formatted", expansion.onPlaceholderRequest(fakePlayer, "balance_formatted"));
            assertEquals("0.00 formatted_eur", expansion.onPlaceholderRequest(fakePlayer, "balance_formatted_eur"));
            assertEquals("0.00 short", expansion.onPlaceholderRequest(fakePlayer, "balance_short"));
            assertEquals("0.00 short_eur", expansion.onPlaceholderRequest(fakePlayer, "balance_short_eur"));

            assertEquals("", expansion.onPlaceholderRequest(fakePlayer, "top_10"));

            String topResult = expansion.onPlaceholderRequest(fakePlayer, "top_5_dollar");
            assertEquals("loading", topResult);

            assertEquals("", expansion.onPlaceholderRequest(fakePlayer, "bank_main_dollar"));

        } finally {
            com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = orig;
        }
    }
}
