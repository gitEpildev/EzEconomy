package com.skyblockexp.ezeconomy.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;

public class HistoryAction extends GuiAction {
    public HistoryAction() {
        super("history", "\u00A7cHistory");
    }

    @Override
    public void open(EzEconomyPlugin plugin, Player player) {
        String currency = plugin.getDefaultCurrency();
        var txs = plugin.getTransactions(player.getUniqueId(), currency);
        int size = 9 * Math.max(1, (int) Math.ceil(txs.size() / 9.0));
        String title = plugin.getUserGuiConfig().getString("title.history", "EzEconomy - History");
        Inventory inv = Bukkit.createInventory(new GuiInventoryHolder("history"), Math.min(54, size), GuiUtils.formatMiniMessage(title));
        int slot = 0;
        for (var tx : txs) {
            if (slot >= inv.getSize()) break;
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            String name = "TX " + tx.getTimestamp();
            meta.setDisplayName(name);
            meta.setLore(java.util.List.of("Amount: " + tx.getAmount(), "Currency: " + tx.getCurrency(), "Id: " + tx.getUuid()));
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }
        // back button
        String backIcon = plugin.getUserGuiConfig().getString("back.icon", "ARROW");
        Material mat = Material.ARROW;
        try { mat = Material.valueOf(backIcon.toUpperCase()); } catch (Exception ex) {}
        ItemStack back = new ItemStack(mat);
        ItemMeta bm = back.getItemMeta();
        bm.setDisplayName(GuiUtils.formatMiniMessage(plugin.getUserGuiConfig().getString("back.display-name", "&cBack")));
        java.util.List<String> lore = plugin.getUserGuiConfig().getStringList("back.lore");
        if (lore == null || lore.isEmpty()) lore = java.util.List.of("&7Return to menu");
        java.util.List<String> formatted = new java.util.ArrayList<>();
        for (String l : lore) formatted.add(GuiUtils.formatMiniMessage(l));
        bm.setLore(formatted);
        GuiUtils.setGuiAction(bm, plugin, "back");
        back.setItemMeta(bm);
        inv.setItem(inv.getSize()-1, back);
        player.openInventory(inv);
    }
}
