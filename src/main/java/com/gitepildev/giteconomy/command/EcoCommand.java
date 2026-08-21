package com.gitepildev.giteconomy.command;


import com.gitepildev.giteconomy.command.eco.GiveSubcommand;
import com.gitepildev.giteconomy.command.eco.SetSubcommand;
import com.gitepildev.giteconomy.command.eco.TakeSubcommand;
import com.gitepildev.giteconomy.command.Subcommand;
import com.gitepildev.giteconomy.core.GitEconomyPlugin;
import com.gitepildev.giteconomy.util.MessageUtils;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class EcoCommand implements CommandExecutor {
    private final GitEconomyPlugin plugin;
    private final Map<String, Subcommand> subcommands;

    public EcoCommand(GitEconomyPlugin plugin) {
        this.plugin = plugin;
        this.subcommands = new HashMap<>();
        this.subcommands.put("give", new GiveSubcommand(plugin));
        this.subcommands.put("take", new TakeSubcommand(plugin));
        this.subcommands.put("set", new SetSubcommand(plugin));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("giteconomy.eco")) {
            MessageUtils.send(sender, plugin, "no_permission");
            return true;
        }

        if (args.length == 0) {
            MessageUtils.send(sender, plugin, "usage_eco");
            return true;
        }

        String subcommandKey = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        Subcommand subcommand = subcommands.get(subcommandKey);
        if (subcommand != null) {
            return subcommand.execute(sender, subArgs);
        }

        // Unknown subcommand
        MessageUtils.send(sender, plugin, "unknown_action");
        return true;
    }
}
