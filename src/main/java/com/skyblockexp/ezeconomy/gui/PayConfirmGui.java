package com.skyblockexp.ezeconomy.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.core.Registry;
import org.bukkit.configuration.file.FileConfiguration;

public class PayConfirmGui {
    public static void open(EzEconomyPlugin plugin, Player player, String targetName, String amount) {
        FileConfiguration cfg = Registry.get(FileConfiguration.class);
        String title = cfg.getString("title.confirm_pay", "EzEconomy - Confirm Pay");
        Inventory inv = Bukkit.createInventory(new GuiInventoryHolder("confirm_pay"), 9, GuiUtils.formatMiniMessage(title));
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName(GuiUtils.formatMiniMessage(cfg.getString("pay.confirm.info.display-name", "Confirm Payment")));
        im.setLore(java.util.List.of(GuiUtils.formatMiniMessage(cfg.getString("pay.confirm.info.lore", "Pay {amount} to {target}")).replace("{amount}", amount).replace("{target}", targetName)));
        info.setItemMeta(im);
        inv.setItem(3, info);

        ItemStack confirm = new ItemStack(Material.valueOf(cfg.getString("pay.confirm.confirm.icon", "EMERALD")));
        ItemMeta cm = confirm.getItemMeta();
        cm.setDisplayName(GuiUtils.formatMiniMessage(cfg.getString("pay.confirm.confirm.display-name", "Confirm")));
        cm.setLore(java.util.List.of(GuiUtils.formatMiniMessage(cfg.getString("pay.confirm.confirm.lore", "Click to confirm payment"))));
        confirm.setItemMeta(cm);
        inv.setItem(4, confirm);

        ItemStack cancel = new ItemStack(Material.valueOf(cfg.getString("pay.confirm.cancel.icon", "BARRIER")));
        ItemMeta xm = cancel.getItemMeta();
        xm.setDisplayName(GuiUtils.formatMiniMessage(cfg.getString("pay.confirm.cancel.display-name", "Cancel")));
        xm.setLore(java.util.List.of(GuiUtils.formatMiniMessage(cfg.getString("pay.confirm.cancel.lore", "Click to cancel"))));
        cancel.setItemMeta(xm);
        inv.setItem(5, cancel);

        player.openInventory(inv);
        // store the selected payment info in PayFlowManager via plugin if needed
    }
}
