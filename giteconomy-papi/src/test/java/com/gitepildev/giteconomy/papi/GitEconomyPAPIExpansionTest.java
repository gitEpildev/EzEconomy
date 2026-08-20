package com.gitepildev.giteconomy.papi;

import com.gitepildev.giteconomy.api.storage.StorageProvider;
import com.gitepildev.giteconomy.dto.EconomyPlayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.bukkit.OfflinePlayer;
import com.gitepildev.giteconomy.papi.testhelpers.TestPlayerFakes;

 
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

public class GitEconomyPAPIExpansionTest {

    @AfterEach
    public void tearDown() {
        GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = null;
    }

    private OfflinePlayer offlinePlayer(UUID id) {
        return TestPlayerFakes.fakeOfflinePlayer(id);
    }

    @Test
    public void balance_returnsZeroWhenPlayerNull() {
        GitEconomyPAPIExpansion exp = new GitEconomyPAPIExpansion(null);
        // Provide a test hook so the code path doesn't attempt to access Bukkit static server
        GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = com.gitepildev.giteconomy.papi.testhelpers.TestGitEconomyHelpers.formatting("dollar", "$");

        assertEquals("0", exp.onPlaceholderRequest((OfflinePlayer) null, "balance"));
    }

    @Test
    public void balance_usesTestEconomyStorage() {
        GitEconomyPAPIExpansion expansion = new GitEconomyPAPIExpansion(null);
        UUID id = UUID.randomUUID();

        StorageProvider stub = new SimpleStorageProvider();
        stub.setBalance(id, "dollar", 123.45);

        GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new GitEconomyPAPIExpansion.TestGitEconomy() {
            @Override
            public StorageProvider getStorageOrWarn() { return stub; }

            @Override
            public String getDefaultCurrency() { return "dollar"; }

            @Override
            public String format(double amount, String currency) { return String.format("$%.2f", amount); }

            @Override
            public String formatShort(double amount, String currency) { return String.format("$%.0f", amount); }

            @Override
            public String getCurrencySymbol(String currency) { return "$"; }

            @Override
            public com.gitepildev.giteconomy.manager.CurrencyPreferenceManager getCurrencyPreferenceManager() { return null; }
        };

        OfflinePlayer p = offlinePlayer(id);
        String out = expansion.onPlaceholderRequest(p, "balance");
        assertEquals("$123.45", out);
    }

    @Test
    public void symbol_fallsBackToDollarWhenNullOrThrows() {
        GitEconomyPAPIExpansion expansion = new GitEconomyPAPIExpansion(null);

        GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new GitEconomyPAPIExpansion.TestGitEconomy() {
            @Override public StorageProvider getStorageOrWarn() { return null; }
            @Override public String getDefaultCurrency() { return "dollar"; }
            @Override public String format(double amount, String currency) { return ""; }
            @Override public String formatShort(double amount, String currency) { return ""; }
            @Override public String getCurrencySymbol(String currency) { throw new RuntimeException("boom"); }
            @Override public com.gitepildev.giteconomy.manager.CurrencyPreferenceManager getCurrencyPreferenceManager() { return null; }
        };

        String out = expansion.onPlaceholderRequest(null, "symbol_dollar");
        assertEquals("$", out);
    }

    @Test
    public void top_buildsAndCachesResult() {
        com.gitepildev.giteconomy.cache.CacheManager.setStrategy(com.gitepildev.giteconomy.cache.CachingStrategy.LOCAL);
        GitEconomyPAPIExpansion expansion = new GitEconomyPAPIExpansion(null);

        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();

        SimpleStorageProvider stub = new SimpleStorageProvider();
        stub.setBalance(a, "dollar", 50.0);
        stub.setBalance(b, "dollar", 100.0);
        stub.putPlayer(a, new EconomyPlayer(a, "Alice", null));
        stub.putPlayer(b, new EconomyPlayer(b, "Bob", null));

        GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new GitEconomyPAPIExpansion.TestGitEconomy() {
            @Override public StorageProvider getStorageOrWarn() { return stub; }
            @Override public String getDefaultCurrency() { return "dollar"; }
            @Override public String format(double amount, String currency) { return String.format("$%.0f", amount); }
            @Override public String formatShort(double amount, String currency) { return String.format("$%.0f", amount); }
            @Override public String getCurrencySymbol(String currency) { return "$"; }
            @Override public com.gitepildev.giteconomy.manager.CurrencyPreferenceManager getCurrencyPreferenceManager() { return null; }
        };

        // First call returns a previous/cached value (likely "loading").
        String first = expansion.onPlaceholderRequest(null, "top_2_dollar");
        // Trigger a refresh by calling again; the second call should return the computed result
        String second = expansion.onPlaceholderRequest(null, "top_2_dollar");
        assertTrue(second.contains("Bob") && second.contains("Alice") && second.contains("$100") && second.contains("$50"));

        String third = expansion.onPlaceholderRequest(null, "top_2_dollar");
        assertEquals(second, third);
    }

    // Minimal test StorageProvider to support the tests above
    static class SimpleStorageProvider implements StorageProvider {
        private final Map<UUID, Map<String, Double>> balances = new ConcurrentHashMap<>();
        private final Map<UUID, EconomyPlayer> players = new ConcurrentHashMap<>();

        public void putPlayer(UUID id, EconomyPlayer p) { players.put(id, p); }

        @Override public void init() {}
        @Override public void load() {}
        @Override public void save() {}

        @Override
        public double getBalance(UUID uuid, String currency) {
            return balances.getOrDefault(uuid, Collections.emptyMap()).getOrDefault(currency, 0d);
        }

        @Override public void setBalance(UUID uuid, String currency, double amount) { balances.computeIfAbsent(uuid, k->new ConcurrentHashMap<>()).put(currency, amount); }
        @Override public void logTransaction(com.gitepildev.giteconomy.api.storage.models.Transaction transaction) {}
        @Override public java.util.List<com.gitepildev.giteconomy.api.storage.models.Transaction> getTransactions(UUID uuid, String currency) { return Collections.emptyList(); }
        @Override public boolean tryWithdraw(UUID uuid, String currency, double amount) {
            double cur = getBalance(uuid, currency);
            if (cur < amount) return false;
            setBalance(uuid, currency, cur - amount);
            return true;
        }
        @Override public void deposit(UUID uuid, String currency, double amount) { setBalance(uuid, currency, getBalance(uuid, currency) + amount); }
        @Override public java.util.Map<UUID, Double> getAllBalances(String currency) {
            Map<UUID, Double> out = new HashMap<>();
            balances.forEach((k, v) -> out.put(k, v.getOrDefault(currency, 0d)));
            return out;
        }

        @Override public void shutdown() {}
        @Override public EconomyPlayer getPlayer(UUID uuid) { return players.get(uuid); }
    }
}

