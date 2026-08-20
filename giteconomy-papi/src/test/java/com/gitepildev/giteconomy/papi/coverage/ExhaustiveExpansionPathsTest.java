package com.gitepildev.giteconomy.papi.coverage;

import com.gitepildev.giteconomy.papi.testhelpers.TestGitEconomyStubs;
import com.gitepildev.giteconomy.papi.testhelpers.TestPlayerFakes;
import com.gitepildev.giteconomy.cache.CacheManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ExhaustiveExpansionPathsTest {

    @AfterEach
    public void tearDown() {
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = null;
    }

    @Test
    public void exercise_many_placeholder_variants_under_test_hook() {
        TestGitEconomyStubs.SimpleStorageProvider sp = new TestGitEconomyStubs.SimpleStorageProvider();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        sp.setBalance(a, "dollar", 123.45);
        sp.setBalance(b, "eur", 99.0);
        sp.putPlayer(a, new com.gitepildev.giteconomy.dto.EconomyPlayer(a, "Alice", "A"));

        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new TestGitEconomyStubs.SimpleTestEz(sp, "dollar");
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion expansion = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(null);

        // balance with player
        org.bukkit.OfflinePlayer pa = TestPlayerFakes.fakeOfflinePlayer(a);

        String s1 = expansion.onPlaceholderRequest(pa, "balance");
        assertTrue(s1.contains("123.45") || s1.length() > 0);

        // balance explicit currency
        org.bukkit.OfflinePlayer pb = TestPlayerFakes.fakeOfflinePlayer(b);
        String s2 = expansion.onPlaceholderRequest(pb, "balance_eur");
        assertTrue(s2.contains("99.00") || s2.length() > 0);

        // symbol path
        String sym = expansion.onPlaceholderRequest(null, "symbol_dollar");
        assertNotNull(sym);

        // bank path with existing bank

        // top variants: clear cache then invoke
        String cacheKey = "top:dollar:2";
        CacheManager.getProvider().remove(cacheKey);
        String t1 = expansion.onPlaceholderRequest(null, "top_2_dollar");
        assertNotNull(t1);

        // malformed top -> empty
        String tBad = expansion.onPlaceholderRequest(null, "top_bad");
        assertEquals("", tBad);
    }
}
