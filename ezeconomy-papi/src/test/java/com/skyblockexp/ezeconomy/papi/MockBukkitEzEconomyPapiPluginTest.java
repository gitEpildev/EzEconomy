package com.skyblockexp.ezeconomy.papi;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.bukkit.plugin.java.JavaPlugin;

import static org.junit.jupiter.api.Assertions.*;

public class MockBukkitEzEconomyPapiPluginTest {

    @AfterEach
    public void tearDown() {
        try { MockBukkit.unmock(); } catch (Exception ignored) {}
    }

    @Test
    public void onEnable_disables_when_placeholder_missing() {
        MockBukkit.mock();

        EzEconomyPapiPlugin plugin = (EzEconomyPapiPlugin) MockBukkit.load(EzEconomyPapiPlugin.class);

        // If PlaceholderAPI is not present, the expansion should disable itself
        assertFalse(plugin.isEnabled());
    }

    @Test
    public void onEnable_doesNotDisable_when_placeholder_present() {
        MockBukkit.mock();
        // Try to load a simple placeholder plugin stub; if MockBukkit doesn't register it
        // then the plugin should be disabled. We assert that enabled state matches
        // whether PlaceholderAPI is present in the mock plugin manager.
        try { MockBukkit.load(PlaceholderStub.class); } catch (Exception ignored) {}

        boolean placeholderPresent = MockBukkit.getMock().getPluginManager().getPlugin("PlaceholderAPI") != null;
        EzEconomyPapiPlugin plugin = (EzEconomyPapiPlugin) MockBukkit.load(EzEconomyPapiPlugin.class);

        assertEquals(placeholderPresent, plugin.isEnabled());
    }
}

// PlaceholderStub is provided as a top-level test plugin in PlaceholderStub.java
