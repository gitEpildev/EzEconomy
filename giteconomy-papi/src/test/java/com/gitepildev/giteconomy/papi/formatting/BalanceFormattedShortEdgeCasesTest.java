package com.gitepildev.giteconomy.papi.formatting;

import org.bukkit.OfflinePlayer;
import com.gitepildev.giteconomy.papi.testhelpers.TestPlayerFakes;
import org.junit.jupiter.api.Test;

import java.util.UUID;
 

import static org.junit.jupiter.api.Assertions.*;

public class BalanceFormattedShortEdgeCasesTest {

    static class StubStorage implements com.gitepildev.giteconomy.api.storage.StorageProvider {
        private double val = 0d;
        public void set(double v) { val = v; }
        @Override public void init() {}
        @Override public void load() {}
        @Override public void save() {}
        @Override public double getBalance(UUID uuid, String currency) { return val; }
        @Override public void setBalance(UUID uuid, String currency, double amount) { val = amount; }
        @Override public void logTransaction(com.gitepildev.giteconomy.api.storage.models.Transaction transaction) {}
        @Override public java.util.List<com.gitepildev.giteconomy.api.storage.models.Transaction> getTransactions(UUID uuid, String currency) { return java.util.Collections.emptyList(); }
        @Override public boolean tryWithdraw(UUID uuid, String currency, double amount) { if (val < amount) return false; val -= amount; return true; }
        @Override public void deposit(UUID uuid, String currency, double amount) { val += amount; }
        @Override public java.util.Map<UUID, Double> getAllBalances(String currency) { return java.util.Collections.emptyMap(); }
        @Override public void shutdown() {}
        @Override public com.gitepildev.giteconomy.dto.EconomyPlayer getPlayer(UUID uuid) { return null; }
        @Override public java.util.Set<String> cleanupOrphanedPlayers() { return java.util.Collections.emptySet(); }
        @Override public boolean isConnected() { return true; }
        @Override public com.gitepildev.giteconomy.storage.TransferResult transfer(UUID fromUuid, UUID toUuid, String currency, double amount) { return com.gitepildev.giteconomy.storage.TransferResult.failure(0,0); }
    }

    static class StubEz implements com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TestGitEconomy {
        final StubStorage s = new StubStorage();
        @Override public com.gitepildev.giteconomy.api.storage.StorageProvider getStorageOrWarn() { return s; }
        @Override public String getDefaultCurrency() { return "euro"; }
        @Override public String format(double amount, String currency) { return String.format("%.2f %s", amount, currency); }
        @Override public String formatShort(double amount, String currency) { return String.format("%.1f%s", amount >= 1000 ? amount/1000.0 : amount, amount >= 1000 ? "K" : ""); }
        @Override public String getCurrencySymbol(String currency) { return "€"; }
        @Override public com.gitepildev.giteconomy.manager.CurrencyPreferenceManager getCurrencyPreferenceManager() { return null; }
    }

    private OfflinePlayer offlinePlayer(UUID id) {
        return TestPlayerFakes.fakeOfflinePlayer(id);
    }

    @Test
    public void balanceFormatted_and_short_edge_cases_useDefaultWhenBlankOrMissing() {
        StubEz stub = new StubEz();
        stub.s.set(50.0);
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = stub;

        UUID u = UUID.randomUUID();
        OfflinePlayer p = offlinePlayer(u);
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion exp = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(null);

        String f1 = exp.onPlaceholderRequest(p, "balance_formatted");
        assertTrue(f1.contains("50.00") && f1.contains("euro") || f1.contains("50.00"));

        String s1 = exp.onPlaceholderRequest(p, "balance_short");
        assertNotNull(s1);

        stub.s.set(123.45);
        String f2 = exp.onPlaceholderRequest(p, "balance_formatted_dollar");
        assertTrue(f2.contains("123.45") || f2.contains("123.5"));

        String s2 = exp.onPlaceholderRequest(p, "balance_short_dollar");
        assertNotNull(s2);

        stub.s.set(7.0);
        String f3 = exp.onPlaceholderRequest(p, "balance_formatted_");
        assertTrue(f3.contains("7.00") || f3.contains("7.0"));

        String s3 = exp.onPlaceholderRequest(p, "balance_short_");
        assertNotNull(s3);

        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = null;
    }

    @Test
    public void balanceFormatted_and_short_returnZeroWhenPlayerNull() {
        StubEz stub = new StubEz();
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = stub;
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion exp = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(null);

        assertEquals("0", exp.onPlaceholderRequest(null, "balance_formatted"));
        assertEquals("0", exp.onPlaceholderRequest(null, "balance_short"));

        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = null;
    }
}
