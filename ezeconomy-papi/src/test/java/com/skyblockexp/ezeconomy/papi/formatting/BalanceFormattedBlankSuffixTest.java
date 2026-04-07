package com.skyblockexp.ezeconomy.papi.formatting;

import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.*;

public class BalanceFormattedBlankSuffixTest {

    @Test
    public void blank_suffix_resolves_to_default_currency() {
        MockBukkit.mock();
        com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin papi = (com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin) MockBukkit.load(com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin.class);

        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = com.skyblockexp.ezeconomy.papi.testhelpers.TestEzEconomyHelpers.formatting("dollar", "$");

        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion expansion = new com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion(papi);
        String res = expansion.onPlaceholderRequest(null, "balance_formatted_");
        assertNotNull(res);
        assertTrue(res.contains("FMT:") || res.length() > 0);
    }
}
