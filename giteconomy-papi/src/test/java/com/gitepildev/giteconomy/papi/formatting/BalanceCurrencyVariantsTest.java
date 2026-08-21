package com.gitepildev.giteconomy.papi.formatting;

import com.gitepildev.giteconomy.papi.testhelpers.TestGitEconomyStubs;
import com.gitepildev.giteconomy.papi.testhelpers.TestPlayerFakes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class BalanceCurrencyVariantsTest {

    @AfterEach
    public void tearDown() {
        try { MockBukkit.unmock(); } catch (Exception ignored) {}
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = null;
    }

    @Test
    public void balance_formatted_and_short_with_currency_suffix_are_handled() throws Exception {
        MockBukkit.mock();
        com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin papi = (com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin) MockBukkit.load(com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin.class);

        TestGitEconomyStubs.SimpleStorageProvider sp = new TestGitEconomyStubs.SimpleStorageProvider();
        UUID u = UUID.randomUUID();
        sp.setBalance(u, "gold", 2500.5);

        TestGitEconomyStubs.SimpleTestEz stub = new TestGitEconomyStubs.SimpleTestEz(sp, "dollar") {
            @Override public String format(double amount, String currency) { return String.format("FMT:%.1f:%s", amount, currency); }
            @Override public String formatShort(double amount, String currency) { return String.format("SRT:%.1f:%s", amount, currency); }
        };

        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = stub;
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion expansion = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(papi);

        org.bukkit.OfflinePlayer fake = TestPlayerFakes.fakeOfflinePlayer(u);

        String formatted = expansion.onPlaceholderRequest(fake, "balance_formatted_gold");
        assertNotNull(formatted);
        assertTrue(formatted.contains("gold") || formatted.contains("FMT:"));

        String shorted = expansion.onPlaceholderRequest(fake, "balance_short_gold");
        assertNotNull(shorted);
        assertTrue(shorted.contains("gold") || shorted.contains("SRT:"));
    }
}
