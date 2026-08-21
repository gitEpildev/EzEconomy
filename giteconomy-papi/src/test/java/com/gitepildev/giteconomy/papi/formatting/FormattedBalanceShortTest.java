package com.gitepildev.giteconomy.papi.formatting;

import com.gitepildev.giteconomy.papi.testhelpers.TestGitEconomyStubs;
import com.gitepildev.giteconomy.papi.testhelpers.TestPlayerFakes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class FormattedBalanceShortTest {

    @AfterEach
    public void tearDown() {
        try { MockBukkit.unmock(); } catch (Exception ignored) {}
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = null;
    }

    @Test
    public void balance_formatted_and_short_with_null_storage_returns_zero_formats() throws Exception {
        MockBukkit.mock();
        com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin papi = (com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin) MockBukkit.load(com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin.class);

        TestGitEconomyStubs.SimpleTestEz stub = new TestGitEconomyStubs.SimpleTestEz(null, "dollar") {
            @Override public String format(double amount, String currency) { return "FMT:" + amount + ":" + currency; }
            @Override public String formatShort(double amount, String currency) { return "SRT:" + amount + ":" + currency; }
        };

        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = stub;
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion expansion = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(papi);

        java.util.UUID u = UUID.randomUUID();
        org.bukkit.OfflinePlayer fake = TestPlayerFakes.fakeOfflinePlayer(u);

        String f = expansion.onPlaceholderRequest(fake, "balance_formatted");
        assertNotNull(f);
        assertFalse(f.isEmpty());

        String s = expansion.onPlaceholderRequest(fake, "balance_short");
        assertNotNull(s);
        assertFalse(s.isEmpty());
    }
}
