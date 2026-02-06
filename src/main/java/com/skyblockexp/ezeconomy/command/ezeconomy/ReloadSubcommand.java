package com.skyblockexp.ezeconomy.command.ezeconomy;

import com.skyblockexp.ezeconomy.command.Subcommand;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.util.MessageUtils;
import org.bukkit.command.CommandSender;

/**
 * Handles the /ezeconomy reload subcommand to reload all configurations.
 */
public class ReloadSubcommand implements Subcommand {
    private final EzEconomyPlugin plugin;

    public ReloadSubcommand(EzEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ezeconomy.admin.reload")) {
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