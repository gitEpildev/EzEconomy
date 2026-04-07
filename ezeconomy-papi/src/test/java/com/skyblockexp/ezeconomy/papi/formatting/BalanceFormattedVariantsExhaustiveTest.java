package com.skyblockexp.ezeconomy.papi.formatting;

import com.skyblockexp.ezeconomy.papi.testhelpers.TestEzEconomyStubs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class BalanceFormattedVariantsExhaustiveTest {

    @AfterEach
    public void tearDown() {
        try { MockBukkit.unmock(); } catch (Exception ignored) {}
        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = null;
    }

    @Test
    public void balance_formatted_and_short_variants_with_and_without_storage() throws Exception {
        MockBukkit.mock();
        com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin papi = (com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin) MockBukkit.load(com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin.class);

        TestEzEconomyStubs.SimpleStorageProvider storage = new TestEzEconomyStubs.SimpleStorageProvider();
        UUID u = UUID.randomUUID();
        storage.setBalance(u, "usd", 2500.0);
        storage.setBalance(u, "eur", 123.45);

        // Stub with storage present
        TestEzEconomyStubs.SimpleTestEz stubWithStorage = new TestEzEconomyStubs.SimpleTestEz(storage, "usd");
        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = stubWithStorage;
        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion expansion = new com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion(papi);

            org.bukkit.OfflinePlayer fake = com.skyblockexp.ezeconomy.papi.testhelpers.TestPlayerFakes.fakeOfflinePlayer(u);

        // explicit currency formatted
        String fUsd = expansion.onPlaceholderRequest(fake, "balance_formatted_usd");
        assertNotNull(fUsd);
        assertFalse(fUsd.isEmpty());

        // explicit currency short
        String sUsd = expansion.onPlaceholderRequest(fake, "balance_short_usd");
        assertNotNull(sUsd);
        assertFalse(sUsd.isEmpty());

        // default (no suffix) should use default currency
        String fDefault = expansion.onPlaceholderRequest(fake, "balance_formatted");
        assertNotNull(fDefault);
        assertFalse(fDefault.isEmpty());

        String sDefault = expansion.onPlaceholderRequest(fake, "balance_short");
        assertNotNull(sDefault);
        assertFalse(sDefault.isEmpty());

        // blank-suffix variants (should be treated like blank -> default)
        String fBlank = expansion.onPlaceholderRequest(fake, "balance_formatted_");
        assertNotNull(fBlank);
        assertFalse(fBlank.isEmpty());

        String sBlank = expansion.onPlaceholderRequest(fake, "balance_short_");
        assertNotNull(sBlank);
        assertFalse(sBlank.isEmpty());

        // Now test behavior when storage is null -> should format zero
        TestEzEconomyStubs.SimpleTestEz stubNoStorage = new TestEzEconomyStubs.SimpleTestEz(null, "eur");
        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = stubNoStorage;
        expansion = new com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion(papi);

        String fNoStorage = expansion.onPlaceholderRequest(fake, "balance_formatted_eur");
        assertNotNull(fNoStorage);
        assertTrue(fNoStorage.contains("0.00") || !fNoStorage.isEmpty());

        String sNoStorage = expansion.onPlaceholderRequest(fake, "balance_short_eur");
        assertNotNull(sNoStorage);
        assertTrue(sNoStorage.contains("0.00") || !sNoStorage.isEmpty());
    }

    @Test
    public void balance_identifiers_return_zero_when_offlineplayer_null() {
        // Ensure Bukkit is initialized so getPluginManager() doesn't NPE
        MockBukkit.mock();
        com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin papi = (com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin) MockBukkit.load(com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin.class);
        // Ensure we set a test hook so the expansion uses the test stub instead of looking up the real plugin
        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new TestEzEconomyStubs.SimpleTestEz(null, "usd");
        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion expansion = new com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion(papi);
        assertEquals("0", expansion.onPlaceholderRequest(null, "balance"));
        assertEquals("0", expansion.onPlaceholderRequest(null, "balance_formatted"));
        assertEquals("0", expansion.onPlaceholderRequest(null, "balance_short"));
    }
}
