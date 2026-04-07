package com.skyblockexp.ezeconomy.command.ezeconomy;

import com.skyblockexp.ezeconomy.command.Subcommand;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.core.MessageProvider;
import com.skyblockexp.ezeconomy.manager.DailyRewardManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.Map;

/**
 * Handles the /ezeconomy daily reset subcommand.
 */
public class DailyResetSubcommand implements Subcommand {
    private final EzEconomyPlugin plugin;
    private final DailyRewardManager dailyRewardManager;

    public DailyResetSubcommand(EzEconomyPlugin plugin, DailyRewardManager dailyRewardManager) {
        this.plugin = plugin;
        this.dailyRewardManager = dailyRewardManager;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ezeconomy.admin.daily")) {
            com.skyblockexp.ezeconomy.util.MessageUtils.send(sender, plugin, "no_permission");
            return true;
        }
        if (args.length < 1) {
            com.skyblockexp.ezeconomy.util.MessageUtils.send(sender, plugin, "usage_daily_reset");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        dailyRewardManager.resetReward(target.getUniqueId());
        com.skyblockexp.ezeconomy.util.MessageUtils.send(sender, plugin, "daily_reset", Map.of(
                "player", target.getName() == null ? args[0] : target.getName()
        ));
        return true;
    }
}