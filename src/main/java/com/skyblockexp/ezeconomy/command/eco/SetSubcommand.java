package com.skyblockexp.ezeconomy.command.eco;

import com.skyblockexp.ezeconomy.api.storage.StorageProvider;
import com.skyblockexp.ezeconomy.command.Subcommand;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.util.MessageUtils;
import com.skyblockexp.ezeconomy.util.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.Map;

/**
 * Subcommand for /eco set <player> <amount>
 */
public class SetSubcommand implements Subcommand {
    private final EzEconomyPlugin plugin;

    public SetSubcommand(EzEconomyPlugin plugin) {
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
        StorageProvider storage = plugin.getStorageOrWarn();
        if (storage == null) {
            MessageUtils.send(sender, plugin, "storage_unavailable");
            return true;
        }
        storage.setBalance(target.getUniqueId(), plugin.getDefaultCurrency(), amount);
        MessageUtils.send(sender, plugin, "set", Map.of("player", target.getName(), "balance", plugin.getEconomy().format(amount)));
        return true;
    }
}