package com.gitepildev.giteconomy.papi;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TargetedPAPIExpansionTests {

    @Test
    public void top_withEmptyStorage_returnsLoading_thenEmpty() {
        GitEconomyPAPIExpansion expansion = new GitEconomyPAPIExpansion(null);

        GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new GitEconomyPAPIExpansion.TestGitEconomy() {
            @Override public com.gitepildev.giteconomy.api.storage.StorageProvider getStorageOrWarn() {
                return new com.gitepildev.giteconomy.api.storage.StorageProvider() {
                    @Override public void init() {}
                    @Override public void load() {}
                    @Override public void save() {}
                    @Override public double getBalance(UUID uuid, String currency) { return 0; }
                    @Override public void setBalance(UUID uuid, String currency, double amount) {}
                    @Override public void logTransaction(com.gitepildev.giteconomy.api.storage.models.Transaction transaction) {}
                    @Override public java.util.List<com.gitepildev.giteconomy.api.storage.models.Transaction> getTransactions(UUID uuid, String currency) { return Collections.emptyList(); }
                    @Override public boolean tryWithdraw(UUID uuid, String currency, double amount) { return false; }
                    @Override public void deposit(UUID uuid, String currency, double amount) {}
                    @Override public java.util.Map<UUID, Double> getAllBalances(String currency) { return Collections.emptyMap(); }
                    @Override public void shutdown() {}
                    @Override public com.gitepildev.giteconomy.dto.EconomyPlayer getPlayer(UUID uuid) { return null; }
                    @Override public java.util.Set<String> cleanupOrphanedPlayers() { return Collections.emptySet(); }
                    @Override public boolean isConnected() { return true; }
                    @Override public com.gitepildev.giteconomy.storage.TransferResult transfer(UUID fromUuid, UUID toUuid, String currency, double amount) { return com.gitepildev.giteconomy.storage.TransferResult.failure(0,0); }
                };
            }

            @Override public String getDefaultCurrency() { return "dollar"; }
            @Override public String format(double amount, String currency) { return String.format("$%.2f", amount); }
            @Override public String formatShort(double amount, String currency) { return String.format("$%.0f", amount); }
            @Override public String getCurrencySymbol(String currency) { return "$"; }
            @Override public com.gitepildev.giteconomy.manager.CurrencyPreferenceManager getCurrencyPreferenceManager() { return null; }
        };

        String first = expansion.onPlaceholderRequest(null, "top_3_dollar");
        assertEquals("loading", first);

        String second = expansion.onPlaceholderRequest(null, "top_3_dollar");
        // After refresh with empty results the cache stores empty string
        assertEquals("", second);
    }

    @Test
    public void symbol_emptyString_returnsDollarFallback() {
        GitEconomyPAPIExpansion expansion = new GitEconomyPAPIExpansion(null);
        GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = com.gitepildev.giteconomy.papi.testhelpers.TestGitEconomyHelpers.emptyFormatting("dollar");

        String out = expansion.onPlaceholderRequest(null, "symbol_dollar");
        assertEquals("$", out);
    }
}
