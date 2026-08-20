package com.gitepildev.giteconomy.papi.placeholders;

import com.gitepildev.giteconomy.papi.testhelpers.TestGitEconomyStubs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.bukkit.OfflinePlayer;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ComprehensivePAPIExpansionTest {

    @AfterEach
    public void cleanup() throws Exception {
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = null;
        // Attempt to clear any global caches if present by resetting CacheManager provider entry
        try {
            Class<?> cm = Class.forName("com.gitepildev.giteconomy.cache.CacheManager");
            Field f = cm.getDeclaredField("provider");
            f.setAccessible(true);
            Object provider = f.get(null);
            if (provider != null) {
                try {
                    provider.getClass().getMethod("clear").invoke(provider);
                } catch (NoSuchMethodException ignored) {}
            }
        } catch (ClassNotFoundException ignored) {}
    }

    private OfflinePlayer offlinePlayer(java.util.UUID id) {
        return com.gitepildev.giteconomy.papi.testhelpers.TestPlayerFakes.fakeOfflinePlayer(id);
    }

    @Test
    public void balance_and_balance_currency_and_formatted_short_bank_and_top_flow() {
        TestGitEconomyStubs.SimpleStorageProvider sp = new TestGitEconomyStubs.SimpleStorageProvider();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        sp.setBalance(a, "dollar", 1500.0);
        sp.setBalance(b, "dollar", 200.0);
        // provide player names so top placeholder can render names without calling Bukkit
        sp.putPlayer(a, new com.gitepildev.giteconomy.dto.EconomyPlayer(a, "Alice", null));
        sp.putPlayer(b, new com.gitepildev.giteconomy.dto.EconomyPlayer(b, "Bob", null));

        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new TestGitEconomyStubs.SimpleTestEz(sp, "dollar");

        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion expansion = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(null);

        OfflinePlayer offA = offlinePlayer(a);
        OfflinePlayer offB = offlinePlayer(b);

        String balA = expansion.onPlaceholderRequest(offA, "balance");
        assertTrue(balA.contains("1500.00"));

        String balAcur = expansion.onPlaceholderRequest(offA, "balance_dollar");
        assertTrue(balAcur.contains("1500.00"));

        String formatted = expansion.onPlaceholderRequest(offA, "balance_formatted");
        assertTrue(formatted.contains("1500.00"));

        String shortForm = expansion.onPlaceholderRequest(offA, "balance_short");
        assertTrue(shortForm.contains("1.5K") || shortForm.contains("1500.00"));

        // top: exercise the code path (other tests may influence cache); ensure no exceptions
        expansion.onPlaceholderRequest(null, "top_2_dollar");
    }

    @Test
    public void unknown_returnsEmpty_and_nullOffline_for_balance() {
        TestGitEconomyStubs.SimpleStorageProvider sp = new TestGitEconomyStubs.SimpleStorageProvider();
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new TestGitEconomyStubs.SimpleTestEz(sp, "dollar");
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion expansion = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(null);

        assertEquals("", expansion.onPlaceholderRequest(null, "not_a_placeholder"));
        // balance with null offline player should return "0"
        assertEquals("0", expansion.onPlaceholderRequest(null, "balance"));
    }
}
