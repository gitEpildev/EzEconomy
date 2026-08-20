package com.skyblockexp.ezeconomy.command;

import com.skyblockexp.ezeconomy.command.ezeconomy.CleanupSubcommand;
import com.skyblockexp.ezeconomy.command.ezeconomy.DatabaseSubcommand;
import com.skyblockexp.ezeconomy.command.ezeconomy.ReloadMessagesSubcommand;
import com.skyblockexp.ezeconomy.command.ezeconomy.ReloadSubcommand;
import com.skyblockexp.ezeconomy.command.Subcommand;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.util.MessageUtils;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles the /ezeconomy admin command and its subcommands.
 * Includes orphaned player data cleanup for all supported storage types.
 *
 * This class is part of the open-source EzEconomy project.
 */
public class EzEconomyCommand implements CommandExecutor {
    private final EzEconomyPlugin plugin;
    private final Map<String, Subcommand> subcommands;

    public EzEconomyCommand(EzEconomyPlugin plugin) {
        this.plugin = plugin;
        this.subcommands = new HashMap<>();
        this.subcommands.put("cleanup", new CleanupSubcommand(plugin));
        this.subcommands.put("reload", new ReloadSubcommand(plugin));
        this.subcommands.put("reloadmessages", new ReloadMessagesSubcommand(plugin));
        // Single database subcommand handles info, test, and reset
        this.subcommands.put("database", new DatabaseSubcommand(plugin));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            MessageUtils.send(sender, plugin, "usage_ezeconomy");
            return true;
        }

        String subcommandKey = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        // Handle multi-level subcommands
        if (subcommandKey.equals("reload") && subArgs.length > 0 && subArgs[0].equalsIgnoreCase("messages")) {
            subcommandKey = "reloadmessages";
            subArgs = Arrays.copyOfRange(subArgs, 1, subArgs.length);
        }

        // All database subcommands handled by DatabaseSubcommand
        if (subcommandKey.equals("database")) {
            Subcommand subcommand = subcommands.get("database");
            if (subcommand != null) {
                return subcommand.execute(sender, subArgs);
            }
        } else {
            Subcommand subcommand = subcommands.get(subcommandKey);
            if (subcommand != null) {
                return subcommand.execute(sender, subArgs);
            }
        }

        // Unknown subcommand
        MessageUtils.send(sender, plugin, "unknown_subcommand");
        return true;
    }
}
