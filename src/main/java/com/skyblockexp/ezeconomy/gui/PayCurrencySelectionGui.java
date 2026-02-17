package com.skyblockexp.ezeconomy.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.UUID;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;

public final class PayCurrencySelectionGui {
    private PayCurrencySelectionGui() {}

    public static void open(EzEconomyPlugin plugin, Player player, String targetName) {
        var cfg = plugin.getUserGuiConfig();
        String titlePrefix = cfg.getString("pay.pay_currency_prefix", cfg.getString("pay_currency_prefix", "<yellow>EzEconomy</yellow> - <white>Select currency for "));

        // build a cleaned display name to show to users. input `targetName` may be a UUID
        // or include legacy ampersand color codes like "&bPlayer". Keep original `targetName`
        // for internal actions but show `displayName` in the UI.
        String displayName = targetName == null ? "" : targetName;
        // strip legacy ampersand color codes (e.g. &b)
        displayName = displayName.replaceAll("(?i)&[0-9A-FK-OR]", "");
        // if the cleaned name looks like a UUID, try to resolve to a player name
        try {
            UUID id = UUID.fromString(displayName);
            Player tp = Bukkit.getPlayer(id);
            if (tp != null) displayName = tp.getDisplayName();
            else {
                var off = Bukkit.getOfflinePlayer(id);
                if (off != null && off.getName() != null) displayName = off.getName();
            }
        } catch (Exception ignore) {
            // not a UUID, keep cleaned displayName
        }

        // use the same size/layout as the pay amount GUI for visual parity
        int size = Math.max(9, Math.min(54, cfg.getInt("pay.gui.size", 27)));
        String title = GuiUtils.formatMiniMessage(titlePrefix) + displayName;
        Inventory inv = Bukkit.createInventory(new GuiInventoryHolder("pay_currency"), size, title);

        // --- header (center-top) matching pay GUI header style ---
        String headerIconName = cfg.getString("pay.header.icon", "PLAYER_HEAD");
        Material headerMat = Material.PAPER;
        try { headerMat = Material.valueOf(headerIconName.toUpperCase()); } catch (Exception ex) {}
        ItemStack header = new ItemStack(headerMat);
        ItemMeta hm = header.getItemMeta();
        String headerDisplay = cfg.getString("pay.header.display", "<gold>Selecting currency for</gold> {player}");
        hm.setDisplayName(GuiUtils.formatMiniMessage(headerDisplay.replace("{player}", displayName)));
        java.util.List<String> headerLore = new java.util.ArrayList<>();
        String headerHint = cfg.getString("pay.header.lore", "Choose a currency to use for this payment");
        headerLore.add(GuiUtils.formatMiniMessage(headerHint));
        hm.setLore(headerLore);
        header.setItemMeta(hm);
        if (size >= 9) inv.setItem(4, header);

        // --- decorative filler ---
        String fillerIcon = cfg.getString("pay.decoration.filler-icon", "GRAY_STAINED_GLASS_PANE");
        Material fillerMat = Material.GRAY_STAINED_GLASS_PANE;
        try { fillerMat = Material.valueOf(fillerIcon.toUpperCase()); } catch (Exception ex) { fillerMat = Material.PAPER; }
        ItemStack filler = new ItemStack(fillerMat);
        ItemMeta fm = filler.getItemMeta();
        fm.setDisplayName(" ");
        filler.setItemMeta(fm);

        // --- currency items placed in middle row(s), center left empty for symmetry ---
        var mainCfg = plugin.getConfig();
        var section = mainCfg.getConfigurationSection("multi-currency.currencies");
        java.util.List<String> keys = section == null ? java.util.List.of() : new java.util.ArrayList<>(section.getKeys(false));

        int rows = Math.max(1, size / 9);
        int middleRow = rows / 2;
        int rowStart = middleRow * 9;
        int centerIndex = rowStart + 4; // keep center free for symmetry

        // prepare materials/icons
        String currIcon = cfg.getString("pay.currency.icon", "PAPER");
        Material curMat = Material.PAPER;
        try { curMat = Material.valueOf(currIcon.toUpperCase()); } catch (Exception ex) {}
        String currSelectedIcon = cfg.getString("pay.currency.selected.icon", "EMERALD");
        Material selMat = Material.EMERALD;
        try { selMat = Material.valueOf(currSelectedIcon.toUpperCase()); } catch (Exception ex) {}

        String currencyDisplay = cfg.getString("pay.currency.display", "Currency: {key}");
        String currencyLore = cfg.getString("pay.currency.lore", "Click to select currency");

        String selected = plugin.getPayFlowManager().getCurrency(player.getUniqueId());

        // compute candidate slots in the middle row excluding center
        java.util.List<Integer> slots = new java.util.ArrayList<>();
        for (int i = 0; i < 9; i++) {
            int idx = rowStart + i;
            if (idx == centerIndex) continue;
            if (idx < size) slots.add(idx);
        }

        int placed = 0;
        for (String key : keys) {
            if (placed >= slots.size()) break;
            ItemStack curItem = new ItemStack(key.equalsIgnoreCase(selected) ? selMat : curMat);
            ItemMeta meta = curItem.getItemMeta();
            String d = currencyDisplay.replace("{key}", key);
            meta.setDisplayName(GuiUtils.formatMiniMessage(d));
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add(GuiUtils.formatMiniMessage(currencyLore));
            var symbol = cfg.getString("multi-currency.currencies." + key + ".symbol", "");
            if (symbol != null && !symbol.isEmpty()) lore.add(symbol);
            meta.setLore(lore);
            GuiUtils.setGuiAction(meta, plugin, "select_currency:" + key);
            curItem.setItemMeta(meta);
            inv.setItem(slots.get(placed), curItem);
            placed++;
        }

        // place the active currency item near the bottom-left area (same slot as PayAmountGui)
        String displayKey = selected == null ? plugin.getDefaultCurrency() : selected;
        String currSelectedIconCfg = cfg.getString("pay.currency.selected.icon", "EMERALD");
        Material activeMat = Material.EMERALD;
        try { activeMat = Material.valueOf(currSelectedIconCfg.toUpperCase()); } catch (Exception ex) {}
        ItemStack activeCurItem = new ItemStack(activeMat);
        ItemMeta activeMeta = activeCurItem.getItemMeta();
        String d = currencyDisplay.replace("{key}", displayKey);
        activeMeta.setDisplayName(GuiUtils.formatMiniMessage(d));
        activeMeta.setLore(java.util.List.of(GuiUtils.formatMiniMessage(currencyLore)));
        GuiUtils.setGuiAction(activeMeta, plugin, "back_to_pay:" + targetName); // clicking active returns to pay with this currency
        activeCurItem.setItemMeta(activeMeta);
        int currencySlot = Math.max(0, inv.getSize() - 2);
        inv.setItem(currencySlot, activeCurItem);

        // fill remaining slots with decorative filler
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, filler);
        }

        // back -> return to pay amount GUI for the same target (placed at last slot)
        String backIcon = cfg.getString("back.icon", "ARROW");
        Material backMat = Material.ARROW;
        try { backMat = Material.valueOf(backIcon.toUpperCase()); } catch (Exception ex) {}
        ItemStack back = new ItemStack(backMat);
        ItemMeta bm = back.getItemMeta();
        bm.setDisplayName(GuiUtils.formatMiniMessage(cfg.getString("back.display-name", "<red>Back</red>")));
        java.util.List<String> backLore = cfg.getStringList("back.lore");
        if (backLore == null || backLore.isEmpty()) backLore = java.util.List.of("<gray>Return to menu</gray>");
        java.util.List<String> formatted = new java.util.ArrayList<>();
        for (String l : backLore) formatted.add(GuiUtils.formatMiniMessage(l));
        bm.setLore(formatted);
        GuiUtils.setGuiAction(bm, plugin, "back_to_pay:" + targetName);
        back.setItemMeta(bm);
        inv.setItem(inv.getSize() - 1, back);

        player.openInventory(inv);
    }
}
