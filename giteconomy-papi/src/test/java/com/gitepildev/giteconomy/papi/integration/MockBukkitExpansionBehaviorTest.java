package com.gitepildev.giteconomy.papi.integration;

import com.gitepildev.giteconomy.papi.testhelpers.TestGitEconomyStubs;
import com.gitepildev.giteconomy.papi.testhelpers.TestPlayerFakes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.bukkit.OfflinePlayer;

import static org.junit.jupiter.api.Assertions.*;

public class MockBukkitExpansionBehaviorTest {

    @AfterEach
    public void tearDown() {
        try { MockBukkit.unmock(); } catch (Exception ignored) {}
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = null;
    }

    @Test
    public void placeholder_calls_work_when_plugin_enabled_and_placeholder_present() {
        try { MockBukkit.unmock(); } catch (Exception ignored) {}
        MockBukkit.mock();
        try { MockBukkit.load(com.gitepildev.giteconomy.papi.testhelpers.PlaceholderStub.class); } catch (Exception ignored) {}

        com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin parent = (com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin) MockBukkit.load(com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin.class);
        assertNotNull(parent);

        // Inject a simple test economy implementation to avoid relying on GitEconomy bootstrap
        TestGitEconomyStubs.SimpleStorageProvider sp = new TestGitEconomyStubs.SimpleStorageProvider();
        java.util.UUID id = java.util.UUID.randomUUID();
        sp.setBalance(id, "dollar", 42.0);
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new TestGitEconomyStubs.SimpleTestEz(sp, "dollar");

        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion expansion = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(parent);

            OfflinePlayer fake = TestPlayerFakes.fakeOfflinePlayer(id);

        String out = expansion.onPlaceholderRequest(fake, "balance");
        assertNotNull(out);
        assertTrue(out.contains("42") || out.contains("42.00"));
    }
}
