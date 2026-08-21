package com.gitepildev.giteconomy.papi.top;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.*;

public class TopLambdaNullPlayerTest {

    @AfterEach
    public void tearDown() {
        try { MockBukkit.unmock(); } catch (Exception ignored) {}
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = null;
    }

    @Test
    public void handles_null_offline_player_in_lambda() throws Exception {
        MockBukkit.mock();
        com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin papi = (com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin) MockBukkit.load(com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin.class);

        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion expansion = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(papi);
        String out = expansion.onPlaceholderRequest(null, "top_1_dollar");
        assertNotNull(out);
    }
}
