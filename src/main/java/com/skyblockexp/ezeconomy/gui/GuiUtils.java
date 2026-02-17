package com.skyblockexp.ezeconomy.gui;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import java.util.Optional;

public final class GuiUtils {
    private GuiUtils() {}

    public static String formatMiniMessage(String input) {
        if (input == null) return "";
        // first convert legacy ampersand color codes so config values like "&b{player}" render correctly
        String ampersandConverted = org.bukkit.ChatColor.translateAlternateColorCodes('&', input);
        // If the input looks like MiniMessage (uses tags like <red>), prefer MiniMessage parsing.
        // Otherwise return the legacy ampersand-converted string to avoid treating '&' as literal.
        if (input.contains("<")) {
            try {
                var comp = MiniMessage.miniMessage().deserialize(input);
                return LegacyComponentSerializer.legacySection().serialize(comp);
            } catch (NoClassDefFoundError | Exception ex) {
                return ampersandConverted;
            }
        }
        return ampersandConverted;
    }

    public static void setGuiAction(ItemMeta meta, Plugin plugin, String action) {
        if (meta == null || plugin == null || action == null) return;
        try {
            NamespacedKey key = new NamespacedKey(plugin, "action");
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, action);
            // remove any visible lore markers of the older format
            if (meta.hasLore()) {
                java.util.List<String> lore = new java.util.ArrayList<>();
                for (String l : meta.getLore()) {
                    if (l == null) continue;
                    if (l.contains("|action:")) continue;
                    lore.add(l);
                }
                meta.setLore(lore);
            }
        } catch (NoClassDefFoundError | Exception ex) {
            // PDC not available or other error; fall back to keeping lore marker
            try {
                java.util.List<String> lore = meta.hasLore() ? new java.util.ArrayList<>(meta.getLore()) : new java.util.ArrayList<>();
                lore.add("\u00A7r|action:" + action);
                meta.setLore(lore);
            } catch (Exception ignore) {}
        }
    }

    public static Optional<String> getGuiAction(ItemMeta meta, Plugin plugin) {
        if (meta == null) return Optional.empty();
        try {
            if (plugin != null) {
                NamespacedKey key = new NamespacedKey(plugin, "action");
                var c = meta.getPersistentDataContainer();
                if (c.has(key, PersistentDataType.STRING)) {
                    return Optional.ofNullable(c.get(key, PersistentDataType.STRING));
                }
            }
        } catch (NoClassDefFoundError | Exception ex) {
            // ignore and fall back to lore parsing
        }
        if (meta.hasLore()) {
            for (String l : meta.getLore()) {
                if (l != null && l.contains("|action:")) {
                    String rest = l.substring(l.indexOf("|action:") + "|action:".length()).trim();
                    return Optional.of(rest);
                }
            }
        }
        return Optional.empty();
    }
}
