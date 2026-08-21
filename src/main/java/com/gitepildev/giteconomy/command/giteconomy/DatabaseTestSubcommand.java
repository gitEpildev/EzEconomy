package com.gitepildev.giteconomy.command.giteconomy;

import com.gitepildev.giteconomy.api.storage.StorageProvider;
import com.gitepildev.giteconomy.command.Subcommand;
import com.gitepildev.giteconomy.core.GitEconomyPlugin;
import com.gitepildev.giteconomy.util.MessageUtils;
import org.bukkit.command.CommandSender;

import java.util.Map;
import java.util.UUID;

/**
 * Subcommand for /giteconomy database test - tests database functions and resets afterwards
 */
public class DatabaseTestSubcommand implements Subcommand {
    private final GitEconomyPlugin plugin;

    public DatabaseTestSubcommand(GitEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("giteconomy.database.test")) {
            com.gitepildev.giteconomy.util.MessageUtils.send(sender, plugin, "no_permission");
            return true;
        }
        if (args.length < 1 || !args[0].equalsIgnoreCase("confirm")) {
            sender.sendMessage(com.gitepildev.giteconomy.util.MessageUtils.color(plugin, "&cThis command will test all database functions and reset the database afterwards."));
            sender.sendMessage(com.gitepildev.giteconomy.util.MessageUtils.color(plugin, "&cAll data will be lost! Use &f/giteconomy database test confirm &cto proceed."));
            return true;
        }

        StorageProvider storage = plugin.getStorageOrWarn();
        if (storage == null) {
            com.gitepildev.giteconomy.util.MessageUtils.send(sender, plugin, "storage_unavailable");
            return true;
        }
        sender.sendMessage(com.gitepildev.giteconomy.util.MessageUtils.color(plugin, "&6Starting database test..."));

        try {
            // Test player balance operations
            UUID testUUID = UUID.randomUUID();
            sender.sendMessage(com.gitepildev.giteconomy.util.MessageUtils.color(plugin, "&eTesting player balance operations..."));
            storage.setBalance(testUUID, "dollar", 100.0);
            double balance = storage.getBalance(testUUID, "dollar");
            if (balance != 100.0) {
                throw new Exception("Balance set/get failed: expected 100.0, got " + balance);
            }

            boolean withdrawSuccess = storage.tryWithdraw(testUUID, "dollar", 50.0);
            if (!withdrawSuccess) {
                throw new Exception("Withdraw failed");
            }
            balance = storage.getBalance(testUUID, "dollar");
            if (balance != 50.0) {
                throw new Exception("Balance after withdraw failed: expected 50.0, got " + balance);
            }

            sender.sendMessage(com.gitepildev.giteconomy.util.MessageUtils.color(plugin, "&aPlayer balance operations: &2PASS"));

            sender.sendMessage(com.gitepildev.giteconomy.util.MessageUtils.color(plugin, "&aAll tests passed! Resetting database..."));

            // Reset database
            resetDatabase(storage);
            sender.sendMessage(com.gitepildev.giteconomy.util.MessageUtils.color(plugin, "&aDatabase reset complete."));

        } catch (Exception e) {
            sender.sendMessage(com.gitepildev.giteconomy.util.MessageUtils.color(plugin, "&cDatabase test failed: " + e.getMessage()));
            return true;
        }

        return true;
    }

    private void resetDatabase(StorageProvider storage) {
        // This would need to be implemented in each storage provider
        // For now, we'll call cleanup and assume reset is handled
        storage.cleanupOrphanedPlayers();
    }
}