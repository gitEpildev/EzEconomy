package com.skyblockexp.ezeconomy.util;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.core.MessageProvider;
import java.util.Map;
import org.bukkit.command.CommandSender;

public final class MessageUtils {

    private MessageUtils() {}

    public static void send(CommandSender sender, EzEconomyPlugin plugin, String key) {
        send(sender, plugin, key, Map.of());
    }

    public static void send(CommandSender sender, EzEconomyPlugin plugin, String key, Map<String, String> placeholders) {
        MessageProvider messages = provider(plugin);
        if (messages == null) {
            sender.sendMessage("[EzEconomy] Message system not initialized.");
            return;
        }
        sender.sendMessage(messages.get(key, placeholders));
    }

    public static String format(EzEconomyPlugin plugin, String key) {
        return format(plugin, key, Map.of());
    }

    public static String format(EzEconomyPlugin plugin, String key, Map<String, String> placeholders) {
        MessageProvider messages = provider(plugin);
        if (messages == null) {
            return "[EzEconomy] Message system not initialized.";
        }
        return messages.get(key, placeholders);
    }

    public static String color(EzEconomyPlugin plugin, String raw) {
        MessageProvider messages = provider(plugin);
        if (messages == null) {
            return raw;
        }
        return messages.color(raw);
    }

    private static MessageProvider provider(EzEconomyPlugin plugin) {
        return plugin == null ? null : plugin.getMessageProvider();
    }
}
