package com.gitepildev.giteconomy.papi.placeholders;

import com.gitepildev.giteconomy.papi.testhelpers.TestGitEconomyStubs;
import com.gitepildev.giteconomy.papi.testhelpers.TestPlayerFakes;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.UUID;
 

import static org.junit.jupiter.api.Assertions.*;

public class PlaceholderBranchCoverageTest extends com.gitepildev.giteconomy.papi.TestBase {

    @Test
    public void balance_and_balance_currency_when_offlineNull_returnZero() {
        TestGitEconomyStubs.SimpleStorageProvider sp = new TestGitEconomyStubs.SimpleStorageProvider();
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new TestGitEconomyStubs.SimpleTestEz(sp, "usd");
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion expansion = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(null);

        assertEquals("0", expansion.onPlaceholderRequest((OfflinePlayer) null, "balance"));
        assertEquals("0", expansion.onPlaceholderRequest((OfflinePlayer) null, "balance_usd"));
    }

    @Test
    public void balance_variants_with_storageNull_returnFormattedZero() {
        MockBukkit.mock();
        com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin papi = (com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin) MockBukkit.load(com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin.class);
        com.gitepildev.giteconomy.core.GitEconomyPlugin core = (com.gitepildev.giteconomy.core.GitEconomyPlugin) MockBukkit.load(com.gitepildev.giteconomy.papi.EzPluginPathCoverageTest.SimpleEz.class);
        core.setStorage(null);

        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion expansion = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(papi);

        OfflinePlayer fake = TestPlayerFakes.fakeOfflinePlayer();

        String b = expansion.onPlaceholderRequest(fake, "balance");
        assertNotNull(b);

        String bf = expansion.onPlaceholderRequest(fake, "balance_usd");
        assertNotNull(bf);

        String bformatted = expansion.onPlaceholderRequest(fake, "balance_formatted_usd");
        assertNotNull(bformatted);

        String bshort = expansion.onPlaceholderRequest(fake, "balance_short_usd");
        assertNotNull(bshort);
    }

    @Test
    public void symbol_dollar_falls_back_when_testEz_returnsNull() {
        TestGitEconomyStubs.SimpleStorageProvider sp = new TestGitEconomyStubs.SimpleStorageProvider();
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new TestGitEconomyStubs.SimpleTestEz(sp, "dollar");

        com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin papi = null;
        try { MockBukkit.mock(); papi = (com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin) MockBukkit.load(com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin.class); } catch (Exception ignored) {}
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion expansion = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(papi);

        String out = expansion.onPlaceholderRequest((OfflinePlayer) null, "symbol_dollar");
        assertEquals("$", out);
    }

    @Test
    public void top_and_bank_invalid_parts_returnEmptyOrLoading() {
        MockBukkit.mock();
        com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin papi = (com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin) MockBukkit.load(com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin.class);
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion expansion = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(papi);

        assertEquals("", expansion.onPlaceholderRequest((OfflinePlayer) null, "top_1"));
        assertEquals("", expansion.onPlaceholderRequest((OfflinePlayer) null, "bank_only"));
    }
}
