package com.skyblockexp.ezeconomy.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import java.util.ArrayList;

public class MainGui {
    private static final GuiAction[] ACTIONS = new GuiAction[]{
            new BalanceAction(),
            new PayAction(),
            new HistoryAction()
    };

    public static void open(EzEconomyPlugin plugin, Player player) {
        String title = plugin.getUserGuiConfig().getString("title.menu", "EzEconomy - Menu");
        String legacyTitle = GuiUtils.formatMiniMessage(title);
        Inventory inv = Bukkit.createInventory(new GuiInventoryHolder("menu"), 9, legacyTitle);
        int slot = 0;
        for (GuiAction action : ACTIONS) {
            String basePath = "actions." + action.getKey();
            String matName = plugin.getUserGuiConfig().getString(basePath + ".icon", "PAPER");
            Material mat = Material.PAPER;
            try { mat = Material.valueOf(matName.toUpperCase()); } catch (Exception ex) {}
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            String display = plugin.getUserGuiConfig().getString(basePath + ".display-name", action.getDisplayName());
            meta.setDisplayName(GuiUtils.formatMiniMessage(display));
            java.util.List<String> lore = plugin.getUserGuiConfig().getStringList(basePath + ".lore");
            if (lore == null || lore.isEmpty()) lore = java.util.List.of("Click to open");
            java.util.List<String> formattedLore = new ArrayList<>();
            for (String l : lore) formattedLore.add(GuiUtils.formatMiniMessage(l));
            meta.setLore(formattedLore);
            GuiUtils.setGuiAction(meta, plugin, action.getKey());
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }
        player.openInventory(inv);
    }

    
}
