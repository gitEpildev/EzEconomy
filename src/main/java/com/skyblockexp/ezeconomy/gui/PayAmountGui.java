package com.skyblockexp.ezeconomy.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;

public class PayAmountGui {
    public static void open(EzEconomyPlugin plugin, Player player, String targetName) {
        var cfg = plugin.getUserGuiConfig();
        String payToPrefix = cfg.getString("title.pay_to_prefix", "EzEconomy - Pay to ");

        // GUI size (configurable, default 27 for a more professional layout)
        int size = Math.max(9, Math.min(54, cfg.getInt("pay.gui.size", 27)));

        // resolve a friendly display name for the target (UUID or username)
        String displayTarget = targetName;
        java.util.UUID targetUuid = null;
        try {
            targetUuid = java.util.UUID.fromString(targetName);
            var off = plugin.getServer().getOfflinePlayer(targetUuid);
            if (off != null && off.getName() != null) displayTarget = off.getName();
            else displayTarget = targetUuid.toString();
        } catch (IllegalArgumentException ex) {
            var p = plugin.getServer().getPlayerExact(targetName);
            if (p != null) displayTarget = p.getName();
            else {
                var off = plugin.getServer().getOfflinePlayer(targetName);
                if (off != null && off.getName() != null) displayTarget = off.getName();
            }
        }

        Inventory inv = Bukkit.createInventory(new GuiInventoryHolder("pay_to"), size, GuiUtils.formatMiniMessage(payToPrefix) + displayTarget);

        // --- header (center-top) with target info and player's balance ---
        String headerIconName = cfg.getString("pay.header.icon", "PLAYER_HEAD");
        Material headerMat = Material.PAPER;
        try { headerMat = Material.valueOf(headerIconName.toUpperCase()); } catch (Exception ex) {}
        ItemStack header = new ItemStack(headerMat);
        ItemMeta hm = header.getItemMeta();
        String headerDisplay = cfg.getString("pay.header.display", "<gold>Paying to</gold> {player}");
        hm.setDisplayName(GuiUtils.formatMiniMessage(headerDisplay.replace("{player}", displayTarget)));
        java.util.List<String> headerLore = new java.util.ArrayList<>();
        // show payer balance (optional)
        try {
            var storage = plugin.getStorageOrWarn();
                if (storage != null) {
                double bal = storage.getBalance(player.getUniqueId(), plugin.getDefaultCurrency());
                headerLore.add(plugin.getCurrencyFormatter().formatPriceForMessage(bal, plugin.getDefaultCurrency()));
            }
        } catch (Exception ignore) {}
        String headerHint = cfg.getString("pay.header.lore", "Click a preset or choose Custom to enter an amount");
        headerLore.add(GuiUtils.formatMiniMessage(headerHint));
        hm.setLore(headerLore);
        // try to set skull owner if we have a UUID
        try {
            if (hm instanceof org.bukkit.inventory.meta.SkullMeta && targetUuid != null) {
                ((org.bukkit.inventory.meta.SkullMeta) hm).setOwningPlayer(plugin.getServer().getOfflinePlayer(targetUuid));
            }
        } catch (Throwable ignore) {}
        header.setItemMeta(hm);
        // place header in slot 4 (top center)
        if (size >= 9) inv.setItem(4, header);

        // --- decoration filler ---
        String fillerIcon = cfg.getString("pay.decoration.filler-icon", "GRAY_STAINED_GLASS_PANE");
        Material fillerMat = Material.GRAY_STAINED_GLASS_PANE;
        try { fillerMat = Material.valueOf(fillerIcon.toUpperCase()); } catch (Exception ex) { fillerMat = Material.PAPER; }
        ItemStack filler = new ItemStack(fillerMat);
        ItemMeta fm = filler.getItemMeta();
        fm.setDisplayName(" ");
        filler.setItemMeta(fm);

        // --- presets (center row, leave center slot for Custom) ---
        // allow pay.amounts to be either a string-list (supports suffixes like 1k, 2.5m) or fall back to int list
        java.util.List<String> amountStrings = cfg.getStringList("pay.amounts");
        java.util.List<Integer> legacyAmounts = cfg.getIntegerList("pay.amounts");
        if ((amountStrings == null || amountStrings.isEmpty()) && (legacyAmounts == null || legacyAmounts.isEmpty())) {
            amountStrings = java.util.List.of("1","5","10","25","50","100","500","1k");
        } else if ((amountStrings == null || amountStrings.isEmpty()) && legacyAmounts != null && !legacyAmounts.isEmpty()) {
            amountStrings = new java.util.ArrayList<>();
            for (int v : legacyAmounts) amountStrings.add(String.valueOf(v));
        }

        String presetIconName = cfg.getString("pay.preset.icon", "GOLD_NUGGET");
        Material presetMat = Material.GOLD_NUGGET;
        try { presetMat = Material.valueOf(presetIconName.toUpperCase()); } catch (Exception ex) {}
        String presetFormat = cfg.getString("pay.preset.display-name", "Pay {amount}");
        String presetLore = cfg.getString("pay.preset.lore", "Click to pay {amount} to {target}");

        int rows = Math.max(1, size / 9);
        int middleRow = rows / 2;
        int rowStart = middleRow * 9;
        int centerIndex = rowStart + 4; // center slot for Custom

        // gather candidate slots for presets (middle row, excluding center)
        java.util.List<Integer> presetSlots = new java.util.ArrayList<>();
        for (int i = 0; i < 9; i++) {
            int idx = rowStart + i;
            if (idx == centerIndex) continue;
            presetSlots.add(idx);
        }

        int placed = 0;
        for (String s : amountStrings) {
            if (placed >= presetSlots.size()) break;
            var money = com.skyblockexp.ezeconomy.util.NumberUtil.parseMoney(s, plugin.getDefaultCurrency());
            if (money == null) continue;
            java.math.BigDecimal parsed = money.getAmount();
            ItemStack it = new ItemStack(presetMat);
            ItemMeta m = it.getItemMeta();
            String shortAmount = com.skyblockexp.ezeconomy.util.NumberUtil.formatShort(parsed);
            String display = presetFormat.replace("{amount}", shortAmount);
            m.setDisplayName(GuiUtils.formatMiniMessage(display));
            String fullAmount = plugin.getCurrencyFormatter().formatPriceForMessage(parsed.doubleValue(), plugin.getDefaultCurrency());
            String lore = presetLore.replace("{amount}", fullAmount).replace("{target}", displayTarget);
            m.setLore(java.util.List.of(GuiUtils.formatMiniMessage(lore)));
            GuiUtils.setGuiAction(m, plugin, "amount:" + parsed.toPlainString());
            it.setItemMeta(m);
            inv.setItem(presetSlots.get(placed), it);
            placed++;
        }

        // --- Custom entry (center) emphasized ---
        String customIcon = cfg.getString("pay.custom.icon", "PAPER");
        Material customMat = Material.PAPER;
        try { customMat = Material.valueOf(customIcon.toUpperCase()); } catch (Exception ex) {}
        ItemStack custom = new ItemStack(customMat);
        ItemMeta cm = custom.getItemMeta();
        String customDisplay = cfg.getString("pay.custom.display-name", "Custom Amount");
        cm.setDisplayName(GuiUtils.formatMiniMessage(customDisplay));
        java.util.List<String> customLore = cfg.getStringList("pay.custom.lore");
        if (customLore == null || customLore.isEmpty()) customLore = java.util.List.of("Click to enter a custom amount via chat");
        java.util.List<String> formattedCustomLore = new java.util.ArrayList<>();
        for (String l : customLore) formattedCustomLore.add(GuiUtils.formatMiniMessage(l).replace("{target}", displayTarget));
        cm.setLore(formattedCustomLore);
        // emphasize custom: add glow if available
        try {
            var ench = Enchantment.getByName("ARROW_INFINITE");
            if (ench != null) cm.addEnchant(ench, 1, true);
            cm.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } catch (Exception ignore) {}
        GuiUtils.setGuiAction(cm, plugin, "custom:" + displayTarget);
        custom.setItemMeta(cm);
        if (centerIndex < inv.getSize()) inv.setItem(centerIndex, custom);

        // --- Fill remaining slots with decorative filler ---
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, filler);
        }

        // --- show only the currently selected currency (click to open full currency selector) ---
        var mainCfg = plugin.getConfig();
        var section = mainCfg.getConfigurationSection("multi-currency.currencies");
        String selected = plugin.getPayFlowManager().getCurrency(player.getUniqueId());
        String displayKey = selected == null ? plugin.getDefaultCurrency() : selected;

        String currSelectedIcon = cfg.getString("pay.currency.selected.icon", "EMERALD");
        Material selMat = Material.EMERALD;
        try { selMat = Material.valueOf(currSelectedIcon.toUpperCase()); } catch (Exception ex) {}
        String currencyDisplay = cfg.getString("pay.currency.display", "Currency: {key}");
        String currencyLore = cfg.getString("pay.currency.lore", "Click to select currency");

        ItemStack activeCurItem = new ItemStack(selMat);
        ItemMeta meta = activeCurItem.getItemMeta();
        String d = currencyDisplay.replace("{key}", displayKey);
        meta.setDisplayName(GuiUtils.formatMiniMessage(d));
        meta.setLore(java.util.List.of(GuiUtils.formatMiniMessage(currencyLore)));
        activeCurItem.setItemMeta(meta);
        int currencySlot = Math.max(0, inv.getSize() - 2);
        inv.setItem(currencySlot, activeCurItem);

        // back button
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
        GuiUtils.setGuiAction(bm, plugin, "back");
        back.setItemMeta(bm);
        inv.setItem(inv.getSize()-1, back);

        player.openInventory(inv);
    }

}
