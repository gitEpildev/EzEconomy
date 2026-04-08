package com.skyblockexp.ezeconomy.papi.integration;

import com.skyblockexp.ezeconomy.papi.testhelpers.TestEzEconomyStubs;
import com.skyblockexp.ezeconomy.papi.testhelpers.TestPlayerFakes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.bukkit.OfflinePlayer;

import static org.junit.jupiter.api.Assertions.*;

public class MockBukkitExpansionBehaviorTest {

    @AfterEach
    public void tearDown() {
        try { MockBukkit.unmock(); } catch (Exception ignored) {}
        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = null;
    }

    @Test
    public void placeholder_calls_work_when_plugin_enabled_and_placeholder_present() {
        try { MockBukkit.unmock(); } catch (Exception ignored) {}
        MockBukkit.mock();
        try { MockBukkit.load(com.skyblockexp.ezeconomy.papi.testhelpers.PlaceholderStub.class); } catch (Exception ignored) {}

        com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin parent = (com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin) MockBukkit.load(com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin.class);
        assertNotNull(parent);

        // Inject a simple test economy implementation to avoid relying on EzEconomy bootstrap
        TestEzEconomyStubs.SimpleStorageProvider sp = new TestEzEconomyStubs.SimpleStorageProvider();
        java.util.UUID id = java.util.UUID.randomUUID();
        sp.setBalance(id, "dollar", 42.0);
        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new TestEzEconomyStubs.SimpleTestEz(sp, "dollar");

        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion expansion = new com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion(parent);

            OfflinePlayer fake = TestPlayerFakes.fakeOfflinePlayer(id);

        String out = expansion.onPlaceholderRequest(fake, "balance");
        assertNotNull(out);
        assertTrue(out.contains("42") || out.contains("42.00"));
    }
}
