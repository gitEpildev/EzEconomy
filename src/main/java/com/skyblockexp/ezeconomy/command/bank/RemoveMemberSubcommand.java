package com.skyblockexp.ezeconomy.command.bank;

import com.skyblockexp.ezeconomy.api.storage.StorageProvider;
import com.skyblockexp.ezeconomy.command.Subcommand;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.core.MessageProvider;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.Map;

/**
 * Subcommand for /bank removemember <name> <player>
 */
public class RemoveMemberSubcommand implements Subcommand {
    private final EzEconomyPlugin plugin;

    public RemoveMemberSubcommand(EzEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ezeconomy.bank.removemember") && !sender.hasPermission("ezeconomy.bank.admin")) {
            com.skyblockexp.ezeconomy.util.MessageUtils.send(sender, plugin, "no_permission");
            return true;
        }
        if (args.length < 2) {
            com.skyblockexp.ezeconomy.util.MessageUtils.send(sender, plugin, "usage_bank");
            return true;
        }
        StorageProvider storage = plugin.getStorageOrWarn();
        if (storage == null) {
            com.skyblockexp.ezeconomy.util.MessageUtils.send(sender, plugin, "storage_unavailable");
            return true;
        }
        OfflinePlayer player = Bukkit.getOfflinePlayer(args[1]);
        storage.removeBankMember(args[0], player.getUniqueId());
        com.skyblockexp.ezeconomy.util.MessageUtils.send(sender, plugin, "removed_member", Map.of("player", player.getName(), "name", args[0]));
        return true;
    }
}