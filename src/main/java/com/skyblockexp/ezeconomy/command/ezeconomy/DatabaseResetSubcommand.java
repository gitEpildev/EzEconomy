package com.skyblockexp.ezeconomy.command.ezeconomy;

import com.skyblockexp.ezeconomy.api.storage.StorageProvider;
import com.skyblockexp.ezeconomy.command.Subcommand;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.core.MessageProvider;
import org.bukkit.command.CommandSender;

/**
 * Subcommand for /ezeconomy database reset - resets database and rebuilds tables
 */
public class DatabaseResetSubcommand implements Subcommand {
    private final EzEconomyPlugin plugin;

    public DatabaseResetSubcommand(EzEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ezeconomy.database.reset")) {
            com.skyblockexp.ezeconomy.util.MessageUtils.send(sender, plugin, "no_permission");
            return true;
        }

        if (args.length < 1 || !args[0].equalsIgnoreCase("confirm")) {
            sender.sendMessage(com.skyblockexp.ezeconomy.util.MessageUtils.color(plugin, "&cThis command will reset the entire database and rebuild all tables."));
            sender.sendMessage(com.skyblockexp.ezeconomy.util.MessageUtils.color(plugin, "&cALL DATA WILL BE LOST! Use &f/ezeconomy database reset confirm &cto proceed."));
            return true;
        }

        StorageProvider storage = plugin.getStorageOrWarn();
        if (storage == null) {
            com.skyblockexp.ezeconomy.util.MessageUtils.send(sender, plugin, "storage_unavailable");
            return true;
        }

        sender.sendMessage(com.skyblockexp.ezeconomy.util.MessageUtils.color(plugin, "&6Resetting database..."));

        try {
            // Shutdown current storage
            storage.shutdown();

            // Reinitialize storage (this should recreate tables)
            storage.init();

            sender.sendMessage(com.skyblockexp.ezeconomy.util.MessageUtils.color(plugin, "&aDatabase reset and rebuild complete."));

        } catch (Exception e) {
            sender.sendMessage(com.skyblockexp.ezeconomy.util.MessageUtils.color(plugin, "&cDatabase reset failed: " + e.getMessage()));
            return true;
        }

        return true;
    }
}