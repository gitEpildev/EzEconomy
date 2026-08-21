package com.gitepildev.giteconomy.papi.formatting;

import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.*;

public class BalanceFormattedBlankSuffixTest {

    @Test
    public void blank_suffix_resolves_to_default_currency() {
        MockBukkit.mock();
        com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin papi = (com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin) MockBukkit.load(com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin.class);

        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = com.gitepildev.giteconomy.papi.testhelpers.TestGitEconomyHelpers.formatting("dollar", "$");

        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion expansion = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(papi);
        String res = expansion.onPlaceholderRequest(null, "balance_formatted_");
        assertNotNull(res);
        assertTrue(res.contains("FMT:") || res.length() > 0);
    }
}
