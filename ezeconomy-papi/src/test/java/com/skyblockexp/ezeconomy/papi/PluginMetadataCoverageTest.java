package com.skyblockexp.ezeconomy.papi;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.*;

public class PluginMetadataCoverageTest {

    @AfterEach
    public void tearDown() {
        try { MockBukkit.unmock(); } catch (Exception ignored) {}
    }

    @Test
    public void expansion_metadata_and_flags_are_accessible() {
        MockBukkit.mock();
        EzEconomyPapiPlugin plugin = (EzEconomyPapiPlugin) MockBukkit.load(EzEconomyPapiPlugin.class);

        EzEconomyPAPIExpansion expansion = new EzEconomyPAPIExpansion(plugin);

        assertTrue(expansion.persist());
        assertTrue(expansion.canRegister());
        assertNotNull(expansion.getAuthor());
        assertEquals("ezeconomy", expansion.getIdentifier());
        assertNotNull(expansion.getVersion());
    }
}
