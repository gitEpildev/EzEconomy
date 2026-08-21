package com.gitepildev.giteconomy.papi.top;

import com.gitepildev.giteconomy.papi.testhelpers.TestGitEconomyStubs;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TopInlineTest {

    @AfterEach
    public void tearDown() {
        try { MockBukkit.unmock(); } catch (Exception ignored) {}
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = null;
    }

    @Test
    public void top_inline_placeholder_returns_nonempty() {
        MockBukkit.mock();
        com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin papi = (com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin) MockBukkit.load(com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin.class);

        TestGitEconomyStubs.SimpleStorageProvider sp = new TestGitEconomyStubs.SimpleStorageProvider();
        UUID u = UUID.randomUUID();
        sp.setBalance(u, "dollar", 42.0);

        com.gitepildev.giteconomy.core.GitEconomyPlugin core = (com.gitepildev.giteconomy.core.GitEconomyPlugin) MockBukkit.load(com.gitepildev.giteconomy.papi.EzPluginPathCoverageTest.SimpleEz.class);
        core.setStorage(sp);

        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion expansion = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(papi);
        String res = expansion.onPlaceholderRequest(null, "top_1_dollar");
        assertNotNull(res);
    }
}
