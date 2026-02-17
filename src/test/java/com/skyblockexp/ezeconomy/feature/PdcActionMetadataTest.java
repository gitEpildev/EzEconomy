package com.skyblockexp.ezeconomy.feature;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.feature.support.TestSupport;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;

import static org.junit.jupiter.api.Assertions.*;

public class PdcActionMetadataTest {
    private Object server;
    private EzEconomyPlugin plugin;
    private TestSupport.MockStorage storage;

    @BeforeEach
    public void setup() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(EzEconomyPlugin.class);
        storage = new TestSupport.MockStorage();
        TestSupport.injectField(plugin, "storage", storage);
        plugin.loadMessageProvider();
    }

    @AfterEach
    public void teardown() {
        MockBukkit.unmock();
    }

    @Test
    public void testPdcActionKeysPresentOnMainGui() throws Exception {
        Object pObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "pdcTester");
        Player p = (Player) pObj;

        com.skyblockexp.ezeconomy.gui.MainGui.open(plugin, p);
        InventoryView view = p.getOpenInventory();

        assertNotNull(view, "Player should have an open inventory after MainGui.open");

        var it0 = view.getTopInventory().getItem(0);
        var it1 = view.getTopInventory().getItem(1);
        var it2 = view.getTopInventory().getItem(2);
        assertNotNull(it0, "Slot 0 item should exist");
        assertNotNull(it1, "Slot 1 item should exist");
        assertNotNull(it2, "Slot 2 item should exist");

        assertTrue(it0.hasItemMeta(), "Slot 0 should have item meta");
        assertTrue(it1.hasItemMeta(), "Slot 1 should have item meta");
        assertTrue(it2.hasItemMeta(), "Slot 2 should have item meta");

        // GuiUtils.getGuiAction should return the PDC-stored key (or fallback to lore). We assert presence and expected keys.
        var g0 = com.skyblockexp.ezeconomy.gui.GuiUtils.getGuiAction(it0.getItemMeta(), plugin);
        var g1 = com.skyblockexp.ezeconomy.gui.GuiUtils.getGuiAction(it1.getItemMeta(), plugin);
        var g2 = com.skyblockexp.ezeconomy.gui.GuiUtils.getGuiAction(it2.getItemMeta(), plugin);

        assertTrue(g0.isPresent(), "Gui action key should be present for slot 0");
        assertTrue(g1.isPresent(), "Gui action key should be present for slot 1");
        assertTrue(g2.isPresent(), "Gui action key should be present for slot 2");

        assertEquals("balance", g0.get(), "Expected action key 'balance' in slot 0");
        assertEquals("pay", g1.get(), "Expected action key 'pay' in slot 1");
        assertEquals("history", g2.get(), "Expected action key 'history' in slot 2");
    }
}
