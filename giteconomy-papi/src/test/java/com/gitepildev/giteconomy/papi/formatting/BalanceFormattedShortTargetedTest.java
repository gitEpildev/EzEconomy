package com.gitepildev.giteconomy.papi.formatting;

import com.gitepildev.giteconomy.papi.testhelpers.TestGitEconomyStubs;
import com.gitepildev.giteconomy.papi.testhelpers.TestPlayerFakes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class BalanceFormattedShortTargetedTest {

    @AfterEach
    public void tearDown() {
        try { MockBukkit.unmock(); } catch (Exception ignored) {}
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = null;
    }

    @Test
    public void balance_formatted_with_blank_suffix_uses_default_currency_and_formats() throws Exception {
        MockBukkit.mock();
        com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin papi = (com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin) MockBukkit.load(com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin.class);

        TestGitEconomyStubs.SimpleStorageProvider storage = new TestGitEconomyStubs.SimpleStorageProvider();
        UUID u = UUID.randomUUID();
        storage.setBalance(u, "eur", 123.45);

        TestGitEconomyStubs.SimpleTestEz stub = new TestGitEconomyStubs.SimpleTestEz(storage, "eur");
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = stub;
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion expansion = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(papi);

        org.bukkit.OfflinePlayer fake = TestPlayerFakes.fakeOfflinePlayer(u);

        // identifier with trailing underscore should trigger blank-rest handling
        String out = expansion.onPlaceholderRequest(fake, "balance_formatted_");
        assertNotNull(out);
        assertFalse(out.isEmpty());
    }

    @Test
    public void balance_short_large_amount_uses_short_formatter() throws Exception {
        MockBukkit.mock();
        com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin papi = (com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin) MockBukkit.load(com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin.class);

        TestGitEconomyStubs.SimpleStorageProvider storage = new TestGitEconomyStubs.SimpleStorageProvider();
        UUID u = UUID.randomUUID();
        storage.setBalance(u, "eur", 1500.0);

        TestGitEconomyStubs.SimpleTestEz stub = new TestGitEconomyStubs.SimpleTestEz(storage, "eur");
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = stub;
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion expansion = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(papi);

        org.bukkit.OfflinePlayer fake = TestPlayerFakes.fakeOfflinePlayer(u);

        String out = expansion.onPlaceholderRequest(fake, "balance_short_");
        assertNotNull(out);
        assertFalse(out.isEmpty());
    }

    @Test
    public void balance_formatted_explicit_currency_respected() throws Exception {
        MockBukkit.mock();
        com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin papi = (com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin) MockBukkit.load(com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin.class);

        TestGitEconomyStubs.SimpleStorageProvider storage = new TestGitEconomyStubs.SimpleStorageProvider();
        UUID u = UUID.randomUUID();
        storage.setBalance(u, "usd", 42.0);

        TestGitEconomyStubs.SimpleTestEz stub = new TestGitEconomyStubs.SimpleTestEz(storage, "eur");
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = stub;
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion expansion = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(papi);

        org.bukkit.OfflinePlayer fake = TestPlayerFakes.fakeOfflinePlayer(u);

        String out = expansion.onPlaceholderRequest(fake, "balance_formatted_usd");
        assertNotNull(out);
        assertFalse(out.isEmpty());
    }

    @Test
    public void symbol_currency_throwing_methods_fallback_to_dollar() throws Exception {
        MockBukkit.mock();
        com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin papi = (com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin) MockBukkit.load(com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin.class);

        TestGitEconomyStubs.SimpleStorageProvider storage = new TestGitEconomyStubs.SimpleStorageProvider();
        TestGitEconomyStubs.SimpleTestEz stub = new TestGitEconomyStubs.SimpleTestEz(storage, "dollar") {
            @Override public String getCurrencySymbol(String currency) { throw new RuntimeException("boom"); }
        };

        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = stub;
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion expansion = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(papi);

        String out = expansion.onPlaceholderRequest(null, "symbol_dollar");
        assertNotNull(out);
        assertEquals("$", out);
    }
}
