package com.gitepildev.giteconomy.papi.metadata;

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
        com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin plugin = (com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin) MockBukkit.load(com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin.class);

        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion expansion = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(plugin);

        assertTrue(expansion.persist());
        assertTrue(expansion.canRegister());
        assertNotNull(expansion.getAuthor());
        assertEquals("giteconomy", expansion.getIdentifier());
        assertNotNull(expansion.getVersion());
    }
}
