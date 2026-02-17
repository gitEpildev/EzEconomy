package com.skyblockexp.ezeconomy.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;

/**
 * Player selection GUI for initiating a payment.
 */
public final class PayPlayerSelectionGui {
    private PayPlayerSelectionGui() {}

    public static void open(EzEconomyPlugin plugin, Player player) {
        open(plugin, player, 0);
    }

    public static void open(EzEconomyPlugin plugin, Player player, int page) {
        int size = 9;
        final int playersPerPage = 6; // slots 0-5 for players
        var cfg = plugin.getUserGuiConfig();
        String title = cfg.getString("title.pay", "EzEconomy - Pay");
        Inventory inv = Bukkit.createInventory(new GuiInventoryHolder("pay"), size, GuiUtils.formatMiniMessage(title));

        // Build a list of candidate player UUIDs: online players + stored players
        java.util.LinkedHashSet<java.util.UUID> uuids = new java.util.LinkedHashSet<>();
        for (var p : Bukkit.getOnlinePlayers()) {
            if (!p.getUniqueId().equals(player.getUniqueId())) uuids.add(p.getUniqueId());
        }

        var storage = plugin.getStorageOrWarn();
        String currency = plugin.getDefaultCurrency();
        if (storage != null) {
            try {
                var all = storage.getAllBalances(currency);
                if (all != null) {
                    for (var u : all.keySet()) {
                        if (!u.equals(player.getUniqueId())) uuids.add(u);
                    }
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to load known players from storage: " + ex.getMessage());
            }
        }

        java.util.List<java.util.UUID> entries = new java.util.ArrayList<>(uuids);
        // sort by name for stable ordering
        entries.sort((a, b) -> {
            String na = Bukkit.getOfflinePlayer(a).getName();
            String nb = Bukkit.getOfflinePlayer(b).getName();
            if (na == null) na = a.toString();
            if (nb == null) nb = b.toString();
            return na.compareToIgnoreCase(nb);
        });

        int total = entries.size();
        int totalPages = Math.max(0, (int) Math.ceil((double) total / playersPerPage) - 1);
        if (page < 0) page = 0;
        if (page > totalPages) page = totalPages;

        String playerIconName = cfg.getString("pay.player.icon", "PAPER");
        Material playerMat = Material.PAPER;
        try { playerMat = Material.valueOf(playerIconName.toUpperCase()); } catch (Exception ex) {}
        String playerDisplay = cfg.getString("pay.player.display-name", "&b{player}");
        java.util.List<String> playerLore = cfg.getStringList("pay.player.lore");
        if (playerLore == null || playerLore.isEmpty()) playerLore = java.util.List.of("Click to start a payment to this player");

        int start = page * playersPerPage;
        int slot = 0;
        for (int i = start; i < Math.min(start + playersPerPage, total); i++) {
            java.util.UUID uuid = entries.get(i);
            String name = null;
            var online = Bukkit.getPlayer(uuid);
            if (online != null) name = online.getDisplayName();
            if (name == null) {
                var off = Bukkit.getOfflinePlayer(uuid);
                if (off != null && off.getName() != null) name = off.getName();
            }
            // If this entry corresponds to the viewer, prefer their live display name
            if (name == null && uuid.equals(player.getUniqueId())) name = player.getDisplayName();
            if (name == null) name = uuid.toString();
            ItemStack item = new ItemStack(playerMat);
            ItemMeta meta = item.getItemMeta();
            // allow config to include {player} and/or {uuid} placeholders; replace first
            String display = playerDisplay.replace("{player}", name).replace("{uuid}", uuid.toString());
            meta.setDisplayName(GuiUtils.formatMiniMessage(display));
            java.util.List<String> loreFormatted = new java.util.ArrayList<>();
            for (String l : playerLore) {
                String repl = l.replace("{player}", name).replace("{uuid}", uuid.toString());
                loreFormatted.add(GuiUtils.formatMiniMessage(repl));
            }
            meta.setLore(loreFormatted);
            // store the target UUID on the item so clicks always resolve to the correct player (even if we don't have a name)
            GuiUtils.setGuiAction(meta, plugin, "payto:" + uuid.toString());
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }

        // Prev button (slot 6)
        String prevIcon = cfg.getString("pay.navigation.prev.icon", "ARROW");
        Material prevMat = Material.ARROW;
        try { prevMat = Material.valueOf(prevIcon.toUpperCase()); } catch (Exception ex) {}
        if (page > 0) {
            ItemStack prev = new ItemStack(prevMat);
            ItemMeta pm = prev.getItemMeta();
            pm.setDisplayName(GuiUtils.formatMiniMessage(cfg.getString("pay.navigation.prev.display-name", "&ePrevious")));
            java.util.List<String> pl = new java.util.ArrayList<>();
            for (String l : cfg.getStringList("pay.navigation.prev.lore")) pl.add(GuiUtils.formatMiniMessage(l));
            pm.setLore(pl);
            GuiUtils.setGuiAction(pm, plugin, "page:" + (page - 1));
            prev.setItemMeta(pm);
            inv.setItem(6, prev);
        }

        // Next button (slot 7)
        String nextIcon = cfg.getString("pay.navigation.next.icon", "ARROW");
        Material nextMat = Material.ARROW;
        try { nextMat = Material.valueOf(nextIcon.toUpperCase()); } catch (Exception ex) {}
        if (page < totalPages) {
            ItemStack next = new ItemStack(nextMat);
            ItemMeta nm = next.getItemMeta();
            nm.setDisplayName(GuiUtils.formatMiniMessage(cfg.getString("pay.navigation.next.display-name", "&eNext")));
            java.util.List<String> nl = new java.util.ArrayList<>();
            for (String l : cfg.getStringList("pay.navigation.next.lore")) nl.add(GuiUtils.formatMiniMessage(l));
            nm.setLore(nl);
            GuiUtils.setGuiAction(nm, plugin, "page:" + (page + 1));
            next.setItemMeta(nm);
            inv.setItem(7, next);
        }

        // back button (slot 8)
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
