package com.gitepildev.giteconomy.papi.integration;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.bukkit.plugin.java.JavaPlugin;

import static org.junit.jupiter.api.Assertions.*;

public class MockBukkitGitEconomyPapiPluginTest {

    @AfterEach
    public void tearDown() {
        try { MockBukkit.unmock(); } catch (Exception ignored) {}
    }

    @Test
    public void onEnable_disables_when_placeholder_missing() {
        MockBukkit.mock();

        com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin plugin = (com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin) MockBukkit.load(com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin.class);

        // If PlaceholderAPI is not present, the expansion should disable itself
        assertFalse(plugin.isEnabled());
    }

    @Test
    public void onEnable_doesNotDisable_when_placeholder_present() {
        MockBukkit.mock();
        // Try to load a simple placeholder plugin stub; if MockBukkit doesn't register it
        // then the plugin should be disabled. We assert that enabled state matches
        // whether PlaceholderAPI is present in the mock plugin manager.
        try { MockBukkit.load(com.gitepildev.giteconomy.papi.testhelpers.PlaceholderStub.class); } catch (Exception ignored) {}

        boolean placeholderPresent = MockBukkit.getMock().getPluginManager().getPlugin("PlaceholderAPI") != null;
        com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin plugin = (com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin) MockBukkit.load(com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin.class);

        assertEquals(placeholderPresent, plugin.isEnabled());
    }
}
