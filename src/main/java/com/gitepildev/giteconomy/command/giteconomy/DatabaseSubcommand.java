package com.gitepildev.giteconomy.command.giteconomy;

import com.gitepildev.giteconomy.api.storage.StorageProvider;
import com.gitepildev.giteconomy.command.Subcommand;
import com.gitepildev.giteconomy.core.GitEconomyPlugin;
import com.gitepildev.giteconomy.core.MessageProvider;
import org.bukkit.command.CommandSender;

import java.util.Map;

/**
 * Subcommand for /giteconomy database - shows database information
 */
public class DatabaseSubcommand implements Subcommand {
    private final GitEconomyPlugin plugin;

    public DatabaseSubcommand(GitEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("giteconomy.database")) {
            com.gitepildev.giteconomy.util.MessageUtils.send(sender, plugin, "no_permission");
            return true;
        }

        StorageProvider storage = plugin.getStorageOrWarn();
        if (storage == null) {
            com.gitepildev.giteconomy.util.MessageUtils.send(sender, plugin, "storage_unavailable");
            return true;
        }

        Object storageCfg = plugin.getConfig().get("storage");
        String storageType;
        if (storageCfg instanceof String) {
            storageType = ((String) storageCfg).toUpperCase();
        } else {
            storageType = plugin.getConfig().getString("storage.type", "yml").toUpperCase();
        }
        sender.sendMessage(com.gitepildev.giteconomy.util.MessageUtils.color(plugin, "&6=== Database Information ==="));
        sender.sendMessage(com.gitepildev.giteconomy.util.MessageUtils.color(plugin, "&eStorage Type: &f" + storageType));
        boolean connected = false;
        String statusColor = "&cDisconnected";
        try {
            connected = storage.isConnected();
            statusColor = connected ? "&aConnected" : "&cDisconnected";
        } catch (Exception ex) {
            statusColor = "&cError";
        }
        sender.sendMessage(com.gitepildev.giteconomy.util.MessageUtils.color(plugin, "&eConnection Status: " + statusColor));
        sender.sendMessage(com.gitepildev.giteconomy.util.MessageUtils.color(plugin, "&eAvailable Subcommands:"));
        sender.sendMessage(com.gitepildev.giteconomy.util.MessageUtils.color(plugin, "&f  /giteconomy database test &7- Test database functions"));
        sender.sendMessage(com.gitepildev.giteconomy.util.MessageUtils.color(plugin, "&f  /giteconomy database reset &7- Reset database tables"));

        return true;
    }
}