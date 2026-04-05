package com.skyblockexp.ezeconomy.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.util.MessageUtils;
import com.skyblockexp.ezeconomy.manager.CurrencyPreferenceManager;
import com.skyblockexp.ezeconomy.api.storage.StorageProvider;

public class BalanceCommand implements CommandExecutor {
    private final EzEconomyPlugin plugin;

    public BalanceCommand(EzEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Using MessageUtils for localized messages
        CurrencyPreferenceManager preferenceManager = plugin.getCurrencyPreferenceManager();
        StorageProvider storage = plugin.getStorageOrWarn();
        // Get available currencies from config
        java.util.Map<String, Object> currencies = plugin.getConfig().getConfigurationSection("multi-currency.currencies") != null
            ? plugin.getConfig().getConfigurationSection("multi-currency.currencies").getValues(false)
            : java.util.Collections.emptyMap();

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                MessageUtils.send(sender, plugin, "only_players");
                return true;
            }
            Player player = (Player) sender;
            String currency = preferenceManager.getPreferredCurrency(player.getUniqueId());
            double balance = storage != null ? storage.getBalance(player.getUniqueId(), currency) : plugin.getEconomy().getBalance(player);
            MessageUtils.send(sender, plugin, "your_balance", java.util.Map.of("balance", plugin.getCurrencyFormatter().formatPriceForMessage(balance, currency), "currency", currency));
            return true;
        } else if (args.length == 1) {
            // /balance <currency> OR /balance <player>
            String lower = args[0].toLowerCase();
            // Prefer treating the argument as a currency if it matches configured currencies.
            if (currencies.containsKey(lower) || currencies.containsKey(args[0])) {
                if (!(sender instanceof Player)) {
                    MessageUtils.send(sender, plugin, "only_players");
                    return true;
                }
                Player player = (Player) sender;
                String currency = lower;
                double balance = storage != null ? storage.getBalance(player.getUniqueId(), currency) : plugin.getEconomy().getBalance(player);
                MessageUtils.send(sender, plugin, "your_balance", java.util.Map.of("balance", plugin.getCurrencyFormatter().formatPriceForMessage(balance, currency), "currency", currency));
                return true;
            }

            // Otherwise treat as a player name. Check online players first to avoid Mojang lookups.
            Player online = Bukkit.getPlayerExact(args[0]);
            OfflinePlayer target = null;
            if (online != null) {
                target = online;
            } else {
                try {
                    target = Bukkit.getOfflinePlayer(args[0]);
                } catch (Exception ex) {
                    MessageUtils.send(sender, plugin, "player_not_found");
                    return true;
                }
            }

            if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
                MessageUtils.send(sender, plugin, "player_not_found");
                return true;
            }

            // /balance <player>
            if (!sender.hasPermission("ezeconomy.balance.others")) {
                MessageUtils.send(sender, plugin, "no_permission_others_balance");
                return true;
            }
            String currency = preferenceManager.getPreferredCurrency(target.getUniqueId());
            double balance = storage != null ? storage.getBalance(target.getUniqueId(), currency) : plugin.getEconomy().getBalance(target);
            MessageUtils.send(sender, plugin, "others_balance", java.util.Map.of("player", target.getName(), "balance", plugin.getCurrencyFormatter().formatPriceForMessage(balance, currency), "currency", currency));
            return true;
        } else if (args.length == 2) {
            // /balance <player> <currency>
            if (!sender.hasPermission("ezeconomy.balance.others")) {
                MessageUtils.send(sender, plugin, "no_permission_others_balance");
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            String currency = args[1].toLowerCase();
            if (!currencies.containsKey(currency)) {
                MessageUtils.send(sender, plugin, "unknown_currency", java.util.Map.of("currency", currency));
                return true;
            }
            double balance = storage != null ? storage.getBalance(target.getUniqueId(), currency) : plugin.getEconomy().getBalance(target);
            MessageUtils.send(sender, plugin, "others_balance", java.util.Map.of("player", target.getName(), "balance", plugin.getCurrencyFormatter().formatPriceForMessage(balance, currency), "currency", currency));
            return true;
        }
        MessageUtils.send(sender, plugin, "usage_balance");
        return true;
    }
}
