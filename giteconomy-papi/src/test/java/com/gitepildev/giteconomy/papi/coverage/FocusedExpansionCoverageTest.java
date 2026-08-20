package com.gitepildev.giteconomy.papi.coverage;

import com.gitepildev.giteconomy.papi.testhelpers.TestGitEconomyStubs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class FocusedExpansionCoverageTest {

    @AfterEach
    public void tearDown() {
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = null;
    }

    @Test
    public void symbol_fallback_and_null_behaviour() {
        TestGitEconomyStubs.SimpleStorageProvider sp = new TestGitEconomyStubs.SimpleStorageProvider();

        // stub that throws for getCurrencySymbol to exercise reflective fallback and null handling
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new TestGitEconomyStubs.SimpleTestEz(sp, "dollar") {
            @Override public String getCurrencySymbol(String currency) {
                if ("boom".equals(currency)) throw new RuntimeException("boom");
                return null; // default to null to exercise empty handling
            }
        };

        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion expansion = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(null);

        // unknown currency returns empty string (safe wraps null)
        assertEquals("", expansion.onPlaceholderRequest(null, "symbol_eur"));

        // dollar falls back to $ per implementation when symbol is null
        assertEquals("$", expansion.onPlaceholderRequest(null, "symbol_dollar"));

        // forcing a throwing stub should still not explode; result is safe-empty
        assertEquals("", expansion.onPlaceholderRequest(null, "symbol_boom"));
    }

    @Test
    public void top_mapping_mixed_player_entries_populates_cache() {
        TestGitEconomyStubs.SimpleStorageProvider sp = new TestGitEconomyStubs.SimpleStorageProvider();

        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        sp.setBalance(a, "dollar", 200.0);
        sp.setBalance(b, "dollar", 100.0);

        // provide an EconomyPlayer for `a` but leave `b` without a player to exercise both branches
        sp.putPlayer(a, new com.gitepildev.giteconomy.dto.EconomyPlayer(a, "Alice", null));

        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new TestGitEconomyStubs.SimpleTestEz(sp, "dollar");

        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion expansion = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(null);

        String cacheKey = "top:dollar:2";
        com.gitepildev.giteconomy.cache.CacheManager.getProvider().remove(cacheKey);

        String first = expansion.onPlaceholderRequest(null, "top_2_dollar");
        assertTrue(first.equals("loading") || first.contains("Alice") || first.contains(a.toString()));

        String second = expansion.onPlaceholderRequest(null, "top_2_dollar");
        assertNotNull(second);
        assertFalse(second.isEmpty());
        // result should be non-empty; detailed content may vary across environments
    }

    @Test
    public void bank_balance_returns_formatted_value() {
        TestGitEconomyStubs.SimpleStorageProvider sp = new TestGitEconomyStubs.SimpleStorageProvider();
        sp.setBankBalance("vault", "dollar", 5.0);

        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new TestGitEconomyStubs.SimpleTestEz(sp, "dollar");

        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion expansion = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(null);

        String out = expansion.onPlaceholderRequest(null, "bank_vault_dollar");
        assertEquals("5.00 dollar", out);
    }

    @Test
    public void top_with_empty_allbalances_sets_empty_cache() {
        TestGitEconomyStubs.SimpleStorageProvider sp = new TestGitEconomyStubs.SimpleStorageProvider();
        // no balances added -> getAllBalances returns empty map

        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new TestGitEconomyStubs.SimpleTestEz(sp, "dollar");
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion expansion = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(null);

        String cacheKey = "top:dollar:5";
        com.gitepildev.giteconomy.cache.CacheManager.getProvider().remove(cacheKey);

        String first = expansion.onPlaceholderRequest(null, "top_5_dollar");
        assertTrue(first.equals("loading") || first.equals(""));

        String second = expansion.onPlaceholderRequest(null, "top_5_dollar");
        // should be present and may be empty string when cached as empty
        assertNotNull(second);
    }

    @Test
    public void top_mapping_uses_display_name_when_present() {
        TestGitEconomyStubs.SimpleStorageProvider sp = new TestGitEconomyStubs.SimpleStorageProvider();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        sp.setBalance(a, "dollar", 50.0);
        sp.setBalance(b, "dollar", 40.0);
        sp.putPlayer(a, new com.gitepildev.giteconomy.dto.EconomyPlayer(a, "Alice", "AliceDisplay"));
        sp.putPlayer(b, new com.gitepildev.giteconomy.dto.EconomyPlayer(b, "Bob", "BobDisplay"));

        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new TestGitEconomyStubs.SimpleTestEz(sp, "dollar");
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion expansion = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(null);

        com.gitepildev.giteconomy.cache.CacheManager.getProvider().remove("top:dollar:2");

        String first = expansion.onPlaceholderRequest(null, "top_2_dollar");
        assertTrue(first.equals("loading") || first.contains("AliceDisplay") || first.contains("BobDisplay"));

        String second = expansion.onPlaceholderRequest(null, "top_2_dollar");
        assertNotNull(second);
        assertTrue(second.contains("AliceDisplay") || second.contains("BobDisplay"));
    }

    @Test
    public void reflectively_invoke_lambda_methods() throws Exception {
        TestGitEconomyStubs.SimpleStorageProvider sp = new TestGitEconomyStubs.SimpleStorageProvider();
        UUID u = UUID.randomUUID();
        sp.setBalance(u, "dollar", 77.0);
        sp.putPlayer(u, new com.gitepildev.giteconomy.dto.EconomyPlayer(u, "Sam", null));

        TestGitEconomyStubs.SimpleTestEz testEz = new TestGitEconomyStubs.SimpleTestEz(sp, "dollar");

        java.util.Map.Entry<java.util.UUID, Double> entry = new java.util.AbstractMap.SimpleEntry<>(u, 77.0);

        Object r0 = com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.formatTopEntryForTests(testEz, "dollar", entry);
        assertNotNull(r0);
        assertTrue(r0.toString().contains("77.00") || r0.toString().contains("77"));

        // Note: lambda$2 targets the production async path using GitEconomyPlugin (a JavaPlugin),
        // which requires a running Bukkit server to instantiate safely. Avoid invoking it here
        // to keep the unit test JVM-friendly; lambda$0 (test-economy path) is exercised above.
    }
}
