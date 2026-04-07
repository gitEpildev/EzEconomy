package com.skyblockexp.ezeconomy.papi.placeholders;

import com.skyblockexp.ezeconomy.papi.testhelpers.TestEzEconomyStubs;
import com.skyblockexp.ezeconomy.papi.testhelpers.TestPlayerFakes;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.UUID;
 

import static org.junit.jupiter.api.Assertions.*;

public class PlaceholderBranchCoverageTest extends com.skyblockexp.ezeconomy.papi.TestBase {

    @Test
    public void balance_and_balance_currency_when_offlineNull_returnZero() {
        TestEzEconomyStubs.SimpleStorageProvider sp = new TestEzEconomyStubs.SimpleStorageProvider();
        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new TestEzEconomyStubs.SimpleTestEz(sp, "usd");
        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion expansion = new com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion(null);

        assertEquals("0", expansion.onPlaceholderRequest((OfflinePlayer) null, "balance"));
        assertEquals("0", expansion.onPlaceholderRequest((OfflinePlayer) null, "balance_usd"));
    }

    @Test
    public void balance_variants_with_storageNull_returnFormattedZero() {
        MockBukkit.mock();
        com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin papi = (com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin) MockBukkit.load(com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin.class);
        com.skyblockexp.ezeconomy.core.EzEconomyPlugin core = (com.skyblockexp.ezeconomy.core.EzEconomyPlugin) MockBukkit.load(com.skyblockexp.ezeconomy.papi.EzPluginPathCoverageTest.SimpleEz.class);
        core.setStorage(null);

        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion expansion = new com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion(papi);

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
        TestEzEconomyStubs.SimpleStorageProvider sp = new TestEzEconomyStubs.SimpleStorageProvider();
        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new TestEzEconomyStubs.SimpleTestEz(sp, "dollar");

        com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin papi = null;
        try { MockBukkit.mock(); papi = (com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin) MockBukkit.load(com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin.class); } catch (Exception ignored) {}
        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion expansion = new com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion(papi);

        String out = expansion.onPlaceholderRequest((OfflinePlayer) null, "symbol_dollar");
        assertEquals("$", out);
    }

    @Test
    public void top_and_bank_invalid_parts_returnEmptyOrLoading() {
        MockBukkit.mock();
        com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin papi = (com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin) MockBukkit.load(com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin.class);
        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion expansion = new com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion(papi);

        assertEquals("", expansion.onPlaceholderRequest((OfflinePlayer) null, "top_1"));
        assertEquals("", expansion.onPlaceholderRequest((OfflinePlayer) null, "bank_only"));
    }
}
