package com.gitepildev.giteconomy.util;

import com.gitepildev.giteconomy.core.GitEconomyPlugin;
import com.gitepildev.giteconomy.core.MessageProvider;
import java.util.Map;
import org.bukkit.command.CommandSender;

public final class MessageUtils {

    private MessageUtils() {}

    public static void send(CommandSender sender, GitEconomyPlugin plugin, String key) {
        send(sender, plugin, key, Map.of());
    }

    public static void send(CommandSender sender, GitEconomyPlugin plugin, String key, Map<String, String> placeholders) {
        MessageProvider messages = provider(plugin);
        if (messages == null) {
            sender.sendMessage("[GitEconomy] Message system not initialized.");
            return;
        }
        sender.sendMessage(messages.get(key, placeholders));
    }

    public static String format(GitEconomyPlugin plugin, String key) {
        return format(plugin, key, Map.of());
    }

    public static String format(GitEconomyPlugin plugin, String key, Map<String, String> placeholders) {
        MessageProvider messages = provider(plugin);
        if (messages == null) {
            return "[GitEconomy] Message system not initialized.";
        }
        return messages.get(key, placeholders);
    }

    public static String color(GitEconomyPlugin plugin, String raw) {
        MessageProvider messages = provider(plugin);
        if (messages == null) {
            return raw;
        }
        return messages.color(raw);
    }

    private static MessageProvider provider(GitEconomyPlugin plugin) {
        return plugin == null ? null : plugin.getMessageProvider();
    }
}
