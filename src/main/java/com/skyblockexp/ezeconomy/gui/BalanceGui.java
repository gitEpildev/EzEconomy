package com.skyblockexp.ezeconomy.gui;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;

public class BalanceGui {
    public static void open(EzEconomyPlugin plugin, Player player, Map<String, Double> currencies, Map<String, Double> banks) {
        int size = 27;
        String title = plugin.getUserGuiConfig().getString("title.balance", "\u00A7aYour Balances");
        Inventory inv = Bukkit.createInventory(new GuiInventoryHolder("balance"), size, GuiUtils.formatMiniMessage(title));
        int slot = 0;
        for (Map.Entry<String, Double> entry : currencies.entrySet()) {
            ItemStack item = new ItemStack(Material.GOLD_INGOT);
            ItemMeta meta = item.getItemMeta();
            // Use plugin short-formatting (k/m/b/t) for GUI display
            String formatted = plugin.formatShort(entry.getValue(), entry.getKey());
            meta.setDisplayName("\u00A7e" + entry.getKey() + ": \u00A76" + formatted);
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }
        for (Map.Entry<String, Double> entry : banks.entrySet()) {
            ItemStack item = new ItemStack(Material.ENDER_CHEST);
            ItemMeta meta = item.getItemMeta();
            String formatted = plugin.formatShort(entry.getValue(), entry.getKey());
            meta.setDisplayName("\u00A7bBank: " + entry.getKey() + "\u00A7f - \u00A7a" + formatted);
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
        List<String> lore = plugin.getUserGuiConfig().getStringList("back.lore");
        if (lore == null || lore.isEmpty()) lore = List.of("&7Return to menu");
        List<String> formatted = new java.util.ArrayList<>();
        for (String l : lore) formatted.add(GuiUtils.formatMiniMessage(l));
        bm.setLore(formatted);
        GuiUtils.setGuiAction(bm, plugin, "back");
        back.setItemMeta(bm);
        inv.setItem(inv.getSize() - 1, back);

        player.openInventory(inv);
    }
}
