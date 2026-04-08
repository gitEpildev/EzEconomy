package com.skyblockexp.ezeconomy.papi.metadata;

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
        com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin plugin = (com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin) MockBukkit.load(com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin.class);

        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion expansion = new com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion(plugin);

        assertTrue(expansion.persist());
        assertTrue(expansion.canRegister());
        assertNotNull(expansion.getAuthor());
        assertEquals("ezeconomy", expansion.getIdentifier());
        assertNotNull(expansion.getVersion());
    }
}
