package com.skyblockexp.ezeconomy.feature;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.feature.support.TestSupport;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GuiCurrencyFeatureTest {
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
    public void testCurrencySelectionViaGui() throws Exception {
        Object senderObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "payer");
        Object recipientObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "payee");
        Player sender = (Player) senderObj;
        Player recipient = (Player) recipientObj;

        // Open the Pay Amount GUI for the recipient
        com.skyblockexp.ezeconomy.gui.PayAmountGui.open(plugin, sender, recipient.getName());
        InventoryView view = sender.getOpenInventory();
        // find slot with currency named "Currency: dollar" (default)
        int found = -1;
        for (int i = 0; i < view.getTopInventory().getSize(); i++) {
            var it = view.getTopInventory().getItem(i);
            if (it == null || !it.hasItemMeta()) continue;
            String name = it.getItemMeta().hasDisplayName() ? it.getItemMeta().getDisplayName() : "";
            if (name.contains("Currency: dollar")) { found = i; break; }
        }
        // click the currency slot
        InventoryClickEvent click = new InventoryClickEvent(view, InventoryType.SlotType.CONTAINER, found, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        plugin.getServer().getPluginManager().callEvent(click);

        // currency selection should be stored in PayFlowManager
        String selected = plugin.getPayFlowManager().getCurrency(sender.getUniqueId());
        assertEquals("dollar", selected);
    }

    @Test
    public void testPayConfirmFlowThroughGui() throws Exception {
        Object senderObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "payer");
        Object recipientObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "payee");
        Player sender = (Player) senderObj;
        Player recipient = (Player) recipientObj;

        // Give initial balances
        storage.setBalance(sender.getUniqueId(), "dollar", 10.0);
        storage.setBalance(recipient.getUniqueId(), "dollar", 0.0);

        // Open the Pay Amount GUI and click the preset amount (e.g., Pay 5)
        com.skyblockexp.ezeconomy.gui.PayAmountGui.open(plugin, sender, recipient.getName());
        InventoryView view = sender.getOpenInventory();
        int amtSlot = -1;
        for (int i = 0; i < view.getTopInventory().getSize(); i++) {
            var it = view.getTopInventory().getItem(i);
            if (it == null || !it.hasItemMeta()) continue;
            String name = it.getItemMeta().hasDisplayName() ? it.getItemMeta().getDisplayName() : "";
            if (name.contains("Pay 5")) { amtSlot = i; break; }
        }
        InventoryClickEvent clickAmt = new InventoryClickEvent(view, InventoryType.SlotType.CONTAINER, amtSlot, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        plugin.getServer().getPluginManager().callEvent(clickAmt);

        // After clicking amount, confirm GUI should open; click confirm (slot 4 by design)
        InventoryView confirmView = sender.getOpenInventory();
        InventoryClickEvent confirmClick = new InventoryClickEvent(confirmView, InventoryType.SlotType.CONTAINER, 4, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        plugin.getServer().getPluginManager().callEvent(confirmClick);

        // assert balances updated
        assertEquals(5.0, storage.getBalance(sender.getUniqueId(), "dollar"), 0.0001);
        assertEquals(5.0, storage.getBalance(recipient.getUniqueId(), "dollar"), 0.0001);
    }
}
