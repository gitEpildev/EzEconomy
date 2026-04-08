package com.skyblockexp.ezeconomy.papi.testhelpers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class PAPIUnitHelpersTest {

    @AfterEach
    public void cleanup() {
        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = null;
    }

    @Test
    public void parseIntOrDefault_variousInputs() throws Exception {
        Method m = com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion.class.getDeclaredMethod("parseIntOrDefault", String.class, int.class);
        m.setAccessible(true);

        assertEquals(5, (int) m.invoke(null, "5", 10));
        assertEquals(10, (int) m.invoke(null, "bad", 10));
        assertEquals(-3, (int) m.invoke(null, "-3", 10));
    }

    @Test
    public void symbol_whenMethodThrows_fallsBackToDollar() {
        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion.TestEzEconomy() {
            @Override public com.skyblockexp.ezeconomy.api.storage.StorageProvider getStorageOrWarn() { return null; }
            @Override public String getDefaultCurrency() { return "dollar"; }
            @Override public String format(double amount, String currency) { return ""; }
            @Override public String formatShort(double amount, String currency) { return ""; }
            @Override public String getCurrencySymbol(String currency) { throw new RuntimeException("boom"); }
            @Override public com.skyblockexp.ezeconomy.manager.CurrencyPreferenceManager getCurrencyPreferenceManager() { return null; }
        };

        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion expansion = new com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion(null);
        String out = expansion.onPlaceholderRequest(null, "symbol_dollar");
        assertEquals("$", out);
    }

    @Test
    public void symbol_whenNull_returnsEmpty_forNonDollar() {
        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = com.skyblockexp.ezeconomy.papi.testhelpers.TestEzEconomyHelpers.emptyFormatting("euro");

        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion expansion = new com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion(null);
        String out = expansion.onPlaceholderRequest(null, "symbol_eur");
        assertEquals("", out);
    }

    @Test
    public void top_withBadNumber_returnsLoading_whenStorageNull() {
        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = com.skyblockexp.ezeconomy.papi.testhelpers.TestEzEconomyHelpers.emptyFormatting("dollar");

        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion expansion = new com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion(null);
        String out = expansion.onPlaceholderRequest(null, "top_x_dollar");
        assertEquals("loading", out);
    }
}
