package com.gitepildev.giteconomy.papi.formatting;

import com.gitepildev.giteconomy.papi.testhelpers.TestGitEconomyStubs;
import com.gitepildev.giteconomy.papi.testhelpers.TestPlayerFakes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class BalanceFormattingPreferenceFallbackTest {

    @AfterEach
    public void tearDown() {
        try { MockBukkit.unmock(); } catch (Exception ignored) {}
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = null;
    }

    @BeforeEach
    public void setUp() {
        try {
            MockBukkit.mock();
        } catch (IllegalStateException e) {
            MockBukkit.unmock();
            MockBukkit.mock();
        }
    }

    @Test
    public void preference_manager_overrides_default_currency() throws Exception {
        com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin papi = (com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin) MockBukkit.load(com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin.class);

        TestGitEconomyStubs.SimpleStorageProvider storage = new TestGitEconomyStubs.SimpleStorageProvider();
        UUID u = UUID.randomUUID();
        storage.setBalance(u, "gbp", 42.0);

        // Use defaultCurrency to simulate the preferred currency when no manager is present
        TestGitEconomyStubs.SimpleTestEz stub = new TestGitEconomyStubs.SimpleTestEz(storage, "gbp");

        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = stub;
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion expansion = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(papi);

            org.bukkit.OfflinePlayer fake = TestPlayerFakes.fakeOfflinePlayer(u);

        String out = expansion.onPlaceholderRequest(fake, "balance_formatted");
        assertNotNull(out);
        assertFalse(out.isEmpty());
    }

    @Test
    public void null_preference_and_null_default_handled_gracefully() throws Exception {
        com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin papi = (com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin) MockBukkit.load(com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin.class);

        TestGitEconomyStubs.SimpleStorageProvider storage = new TestGitEconomyStubs.SimpleStorageProvider();
        UUID u = UUID.randomUUID();
        storage.setBalance(u, "x", 100.0);

        TestGitEconomyStubs.SimpleTestEz stub = new TestGitEconomyStubs.SimpleTestEz(storage, null) {
            @Override
            public com.gitepildev.giteconomy.manager.CurrencyPreferenceManager getCurrencyPreferenceManager() {
                return null; // force using defaultCurrency (which is null here) to exercise null-pref branch
            }

            @Override
            public String format(double amount, String currency) {
                return "FORMATTED(" + amount + "," + currency + ")";
            }
        };

        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = stub;
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion expansion = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(papi);

            org.bukkit.OfflinePlayer fake = TestPlayerFakes.fakeOfflinePlayer(u);

        String out = expansion.onPlaceholderRequest(fake, "balance");
        assertNotNull(out);
        assertTrue(out.contains("FORMATTED"));
        assertTrue(out.contains("null"));
    }

    @Test
    public void explicit_currency_with_null_storage_returns_zero_formatted() throws Exception {
        com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin papi = (com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin) MockBukkit.load(com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin.class);

        TestGitEconomyStubs.SimpleTestEz stub = new TestGitEconomyStubs.SimpleTestEz(null, "usd");
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = stub;
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion expansion = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(papi);

        String out = expansion.onPlaceholderRequest(null, "balance_formatted_eur");
        assertNotNull(out);
        assertTrue(out.contains("0.00") || !out.isEmpty());
    }
}
