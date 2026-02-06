package com.skyblockexp.ezeconomy.command.eco;

import com.skyblockexp.ezeconomy.command.Subcommand;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.util.MessageUtils;
import com.skyblockexp.ezeconomy.util.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.Map;

/**
 * Subcommand for /eco give <player> <amount>
 */
public class GiveSubcommand implements Subcommand {
    private final EzEconomyPlugin plugin;

    public GiveSubcommand(EzEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtils.send(sender, plugin, "usage_eco");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        double amount = NumberUtil.parseAmount(args[1]);
        if (Double.isNaN(amount)) {
            MessageUtils.send(sender, plugin, "invalid_amount");
            return true;
        }
        plugin.getEconomy().depositPlayer(target, amount);
        MessageUtils.send(sender, plugin, "paid", Map.of("player", target.getName(), "amount", plugin.getEconomy().format(amount)));
        return true;
    }
}