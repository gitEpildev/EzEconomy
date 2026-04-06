package com.skyblockexp.ezeconomy.papi;

import com.skyblockexp.ezeconomy.papi.testhelpers.TestEzEconomyStubs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.*;

public class SymbolDollarDefaultTest {

    @AfterEach
    public void tearDown() {
        try { MockBukkit.unmock(); } catch (Exception ignored) {}
        EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = null;
    }

    @Test
    public void symbol_dollar_returns_dollar_sign_when_null() throws Exception {
        MockBukkit.mock();
        EzEconomyPapiPlugin papi = (EzEconomyPapiPlugin) MockBukkit.load(EzEconomyPapiPlugin.class);

        // Storage not needed for this test; currency symbol stub returns null
        TestEzEconomyStubs.SimpleTestEz stub = new TestEzEconomyStubs.SimpleTestEz(null, "dollar") {
            @Override public String getCurrencySymbol(String currency) { return null; }
        };

        EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = stub;
        EzEconomyPAPIExpansion expansion = new EzEconomyPAPIExpansion(papi);

        String out = expansion.onPlaceholderRequest(null, "symbol_dollar");
        assertEquals("$", out);
    }
}
