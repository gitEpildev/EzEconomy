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
        MessageProvider messages = plugin.getMessageProvider();
        if (messages == null) {
            sender.sendMessage("[EzEconomy] Message system not initialized.");
            return;
        }
        sender.sendMessage(messages.get(key, placeholders));
    }
}
