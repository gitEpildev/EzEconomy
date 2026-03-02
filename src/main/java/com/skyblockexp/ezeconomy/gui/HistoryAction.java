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
        String currency = com.skyblockexp.ezeconomy.core.Registry.get(com.skyblockexp.ezeconomy.manager.CurrencyManager.class).getDefaultCurrency();
        var storage = com.skyblockexp.ezeconomy.core.Registry.get(com.skyblockexp.ezeconomy.api.storage.StorageProvider.class);
        var txs = storage == null ? java.util.Collections.emptyList() : storage.getTransactions(player.getUniqueId(), currency);
        int size = 9 * Math.max(1, (int) Math.ceil(txs.size() / 9.0));
        var cfg = com.skyblockexp.ezeconomy.core.Registry.get(org.bukkit.configuration.file.FileConfiguration.class);
        String title = cfg.getString("title.history", "EzEconomy - History");
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
        var cfg = com.skyblockexp.ezeconomy.core.Registry.get(org.bukkit.configuration.file.FileConfiguration.class);
        String backIcon = cfg.getString("back.icon", "ARROW");
        Material mat = Material.ARROW;
        try { mat = Material.valueOf(backIcon.toUpperCase()); } catch (Exception ex) {}
        ItemStack back = new ItemStack(mat);
        ItemMeta bm = back.getItemMeta();
        bm.setDisplayName(GuiUtils.formatMiniMessage(cfg.getString("back.display-name", "&cBack")));
        java.util.List<String> lore = cfg.getStringList("back.lore");
        if (lore == null || lore.isEmpty()) lore = java.util.List.of("&7Return to menu");
        java.util.List<String> formatted = new java.util.ArrayList<>();
        for (String l : lore) formatted.add(GuiUtils.formatMiniMessage(l));
        bm.setLore(formatted);
        GuiUtils.setGuiAction(bm, com.skyblockexp.ezeconomy.core.Registry.getPlugin(), "back");
        back.setItemMeta(bm);
        inv.setItem(inv.getSize()-1, back);
        player.openInventory(inv);
    }
}
