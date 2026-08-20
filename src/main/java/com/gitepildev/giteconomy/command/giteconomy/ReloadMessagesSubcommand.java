package com.gitepildev.giteconomy.command.giteconomy;

import com.gitepildev.giteconomy.command.Subcommand;
import com.gitepildev.giteconomy.core.GitEconomyPlugin;
import com.gitepildev.giteconomy.core.MessageProvider;
import org.bukkit.command.CommandSender;

/**
 * Handles the /giteconomy reload messages subcommand.
 */
public class ReloadMessagesSubcommand implements Subcommand {
    private final GitEconomyPlugin plugin;

    public ReloadMessagesSubcommand(GitEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("giteconomy.admin.reload")) {
            com.gitepildev.giteconomy.util.MessageUtils.send(sender, plugin, "no_permission");
            return true;
        }
        plugin.reloadConfig();
        plugin.loadMessageProvider(); // Assuming there's a method to reload MessageProvider
        com.gitepildev.giteconomy.util.MessageUtils.send(sender, plugin, "reload_messages_success");
        return true;
    }
}