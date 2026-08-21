package com.gitepildev.giteconomy.command.giteconomy;

import com.gitepildev.giteconomy.command.Subcommand;
import com.gitepildev.giteconomy.core.GitEconomyPlugin;
import com.gitepildev.giteconomy.util.MessageUtils;
import org.bukkit.command.CommandSender;

/**
 * Handles the /giteconomy reload subcommand to reload all configurations.
 */
public class ReloadSubcommand implements Subcommand {
    private final GitEconomyPlugin plugin;

    public ReloadSubcommand(GitEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("giteconomy.admin.reload")) {
            MessageUtils.send(sender, plugin, "no_permission");
            return true;
        }
        // Reload main config
        plugin.reloadConfig();
        // Reload messages
        plugin.loadMessageProvider();
        // TODO: Reload storage config if needed
        MessageUtils.send(sender, plugin, "reload_success");
        return true;
    }
}