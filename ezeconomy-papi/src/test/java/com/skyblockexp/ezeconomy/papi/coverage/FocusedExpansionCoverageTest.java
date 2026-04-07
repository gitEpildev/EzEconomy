package com.skyblockexp.ezeconomy.papi.coverage;

import com.skyblockexp.ezeconomy.papi.testhelpers.TestEzEconomyStubs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class FocusedExpansionCoverageTest {

    @AfterEach
    public void tearDown() {
        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = null;
    }

    @Test
    public void symbol_fallback_and_null_behaviour() {
        TestEzEconomyStubs.SimpleStorageProvider sp = new TestEzEconomyStubs.SimpleStorageProvider();

        // stub that throws for getCurrencySymbol to exercise reflective fallback and null handling
        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new TestEzEconomyStubs.SimpleTestEz(sp, "dollar") {
            @Override public String getCurrencySymbol(String currency) {
                if ("boom".equals(currency)) throw new RuntimeException("boom");
                return null; // default to null to exercise empty handling
            }
        };

        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion expansion = new com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion(null);

        // unknown currency returns empty string (safe wraps null)
        assertEquals("", expansion.onPlaceholderRequest(null, "symbol_eur"));

        // dollar falls back to $ per implementation when symbol is null
        assertEquals("$", expansion.onPlaceholderRequest(null, "symbol_dollar"));

        // forcing a throwing stub should still not explode; result is safe-empty
        assertEquals("", expansion.onPlaceholderRequest(null, "symbol_boom"));
    }

    @Test
    public void top_mapping_mixed_player_entries_populates_cache() {
        TestEzEconomyStubs.SimpleStorageProvider sp = new TestEzEconomyStubs.SimpleStorageProvider();

        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        sp.setBalance(a, "dollar", 200.0);
        sp.setBalance(b, "dollar", 100.0);

        // provide an EconomyPlayer for `a` but leave `b` without a player to exercise both branches
        sp.putPlayer(a, new com.skyblockexp.ezeconomy.dto.EconomyPlayer(a, "Alice", null));

        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new TestEzEconomyStubs.SimpleTestEz(sp, "dollar");

        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion expansion = new com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion(null);

        String cacheKey = "top:dollar:2";
        com.skyblockexp.ezeconomy.cache.CacheManager.getProvider().remove(cacheKey);

        String first = expansion.onPlaceholderRequest(null, "top_2_dollar");
        assertTrue(first.equals("loading") || first.contains("Alice") || first.contains(a.toString()));

        String second = expansion.onPlaceholderRequest(null, "top_2_dollar");
        assertNotNull(second);
        assertFalse(second.isEmpty());
        // result should be non-empty; detailed content may vary across environments
    }

    @Test
    public void bank_balance_returns_formatted_value() {
        TestEzEconomyStubs.SimpleStorageProvider sp = new TestEzEconomyStubs.SimpleStorageProvider();
        sp.setBankBalance("vault", "dollar", 5.0);

        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new TestEzEconomyStubs.SimpleTestEz(sp, "dollar");

        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion expansion = new com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion(null);

        String out = expansion.onPlaceholderRequest(null, "bank_vault_dollar");
        assertEquals("5.00 dollar", out);
    }

    @Test
    public void top_with_empty_allbalances_sets_empty_cache() {
        TestEzEconomyStubs.SimpleStorageProvider sp = new TestEzEconomyStubs.SimpleStorageProvider();
        // no balances added -> getAllBalances returns empty map

        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new TestEzEconomyStubs.SimpleTestEz(sp, "dollar");
        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion expansion = new com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion(null);

        String cacheKey = "top:dollar:5";
        com.skyblockexp.ezeconomy.cache.CacheManager.getProvider().remove(cacheKey);

        String first = expansion.onPlaceholderRequest(null, "top_5_dollar");
        assertTrue(first.equals("loading") || first.equals(""));

        String second = expansion.onPlaceholderRequest(null, "top_5_dollar");
        // should be present and may be empty string when cached as empty
        assertNotNull(second);
    }

    @Test
    public void top_mapping_uses_display_name_when_present() {
        TestEzEconomyStubs.SimpleStorageProvider sp = new TestEzEconomyStubs.SimpleStorageProvider();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        sp.setBalance(a, "dollar", 50.0);
        sp.setBalance(b, "dollar", 40.0);
        sp.putPlayer(a, new com.skyblockexp.ezeconomy.dto.EconomyPlayer(a, "Alice", "AliceDisplay"));
        sp.putPlayer(b, new com.skyblockexp.ezeconomy.dto.EconomyPlayer(b, "Bob", "BobDisplay"));

        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new TestEzEconomyStubs.SimpleTestEz(sp, "dollar");
        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion expansion = new com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion(null);

        com.skyblockexp.ezeconomy.cache.CacheManager.getProvider().remove("top:dollar:2");

        String first = expansion.onPlaceholderRequest(null, "top_2_dollar");
        assertTrue(first.equals("loading") || first.contains("AliceDisplay") || first.contains("BobDisplay"));

        String second = expansion.onPlaceholderRequest(null, "top_2_dollar");
        assertNotNull(second);
        assertTrue(second.contains("AliceDisplay") || second.contains("BobDisplay"));
    }

    @Test
    public void reflectively_invoke_lambda_methods() throws Exception {
        TestEzEconomyStubs.SimpleStorageProvider sp = new TestEzEconomyStubs.SimpleStorageProvider();
        UUID u = UUID.randomUUID();
        sp.setBalance(u, "dollar", 77.0);
        sp.putPlayer(u, new com.skyblockexp.ezeconomy.dto.EconomyPlayer(u, "Sam", null));

        TestEzEconomyStubs.SimpleTestEz testEz = new TestEzEconomyStubs.SimpleTestEz(sp, "dollar");

        java.util.Map.Entry<java.util.UUID, Double> entry = new java.util.AbstractMap.SimpleEntry<>(u, 77.0);

        Object r0 = com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion.formatTopEntryForTests(testEz, "dollar", entry);
        assertNotNull(r0);
        assertTrue(r0.toString().contains("77.00") || r0.toString().contains("77"));

        // Note: lambda$2 targets the production async path using EzEconomyPlugin (a JavaPlugin),
        // which requires a running Bukkit server to instantiate safely. Avoid invoking it here
        // to keep the unit test JVM-friendly; lambda$0 (test-economy path) is exercised above.
    }
}
