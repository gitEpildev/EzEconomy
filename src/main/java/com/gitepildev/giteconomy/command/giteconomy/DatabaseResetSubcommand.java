package com.gitepildev.giteconomy.command.giteconomy;

import com.gitepildev.giteconomy.api.storage.StorageProvider;
import com.gitepildev.giteconomy.command.Subcommand;
import com.gitepildev.giteconomy.core.GitEconomyPlugin;
import com.gitepildev.giteconomy.core.MessageProvider;
import org.bukkit.command.CommandSender;

/**
 * Subcommand for /giteconomy database reset - resets database and rebuilds tables
 */
public class DatabaseResetSubcommand implements Subcommand {
    private final GitEconomyPlugin plugin;

    public DatabaseResetSubcommand(GitEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("giteconomy.database.reset")) {
            com.gitepildev.giteconomy.util.MessageUtils.send(sender, plugin, "no_permission");
            return true;
        }

        if (args.length < 1 || !args[0].equalsIgnoreCase("confirm")) {
            sender.sendMessage(com.gitepildev.giteconomy.util.MessageUtils.color(plugin, "&cThis command will reset the entire database and rebuild all tables."));
            sender.sendMessage(com.gitepildev.giteconomy.util.MessageUtils.color(plugin, "&cALL DATA WILL BE LOST! Use &f/giteconomy database reset confirm &cto proceed."));
            return true;
        }

        StorageProvider storage = plugin.getStorageOrWarn();
        if (storage == null) {
            com.gitepildev.giteconomy.util.MessageUtils.send(sender, plugin, "storage_unavailable");
            return true;
        }

        sender.sendMessage(com.gitepildev.giteconomy.util.MessageUtils.color(plugin, "&6Resetting database..."));

        try {
            // Shutdown current storage
            storage.shutdown();

            // Reinitialize storage (this should recreate tables)
            storage.init();

            sender.sendMessage(com.gitepildev.giteconomy.util.MessageUtils.color(plugin, "&aDatabase reset and rebuild complete."));

        } catch (Exception e) {
            sender.sendMessage(com.gitepildev.giteconomy.util.MessageUtils.color(plugin, "&cDatabase reset failed: " + e.getMessage()));
            return true;
        }

        return true;
    }
}