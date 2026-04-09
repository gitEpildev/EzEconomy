package com.skyblockexp.ezeconomy.command;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.util.MessageUtils;
import com.skyblockexp.ezeconomy.manager.CurrencyPreferenceManager;
import com.skyblockexp.ezeconomy.api.storage.StorageProvider;
import java.util.UUID;

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

            // Otherwise treat as a player name.
            OfflinePlayer target = resolveOfflinePlayer(args[0], storage);
            if (target == null) {
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
            String displayName = target.getName() != null ? target.getName() : args[0];
            MessageUtils.send(sender, plugin, "others_balance", java.util.Map.of("player", displayName, "balance", plugin.getCurrencyFormatter().formatPriceForMessage(balance, currency), "currency", currency));
            return true;
        } else if (args.length == 2) {
            // /balance <player> <currency>
            if (!sender.hasPermission("ezeconomy.balance.others")) {
                MessageUtils.send(sender, plugin, "no_permission_others_balance");
                return true;
            }
            OfflinePlayer target = resolveOfflinePlayer(args[0], storage);
            if (target == null) {
                MessageUtils.send(sender, plugin, "player_not_found");
                return true;
            }
            String currency = args[1].toLowerCase();
            if (!currencies.containsKey(currency)) {
                MessageUtils.send(sender, plugin, "unknown_currency", java.util.Map.of("currency", currency));
                return true;
            }
            double balance = storage != null ? storage.getBalance(target.getUniqueId(), currency) : plugin.getEconomy().getBalance(target);
            String displayName2 = target.getName() != null ? target.getName() : args[0];
            MessageUtils.send(sender, plugin, "others_balance", java.util.Map.of("player", displayName2, "balance", plugin.getCurrencyFormatter().formatPriceForMessage(balance, currency), "currency", currency));
            return true;
        }
        MessageUtils.send(sender, plugin, "usage_balance");
        return true;
    }

    private OfflinePlayer resolveOfflinePlayer(String name, StorageProvider storage) {
        Player online = plugin.getServer().getPlayerExact(name);
        if (online != null) return online;

        UUID resolvedUuid = null;
        if (storage != null) {
            resolvedUuid = storage.resolvePlayerByName(name);
        }
        if (resolvedUuid == null) {
            var messenger = plugin.getCrossServerMessenger();
            if (messenger != null) {
                resolvedUuid = messenger.getNetworkPlayerUuid(name);
            }
        }
        if (resolvedUuid != null) {
            return plugin.getServer().getOfflinePlayer(resolvedUuid);
        }

        var maybe = com.skyblockexp.ezeconomy.util.PlayerLookup.findByName(name);
        if (maybe.isPresent()) return maybe.get();

        @SuppressWarnings("deprecation")
        OfflinePlayer stub = plugin.getServer().getOfflinePlayer(name);
        if (stub != null && stub.hasPlayedBefore()) {
            return stub;
        }
        if (stub != null && storage != null) {
            double bal = storage.getBalance(stub.getUniqueId(), plugin.getDefaultCurrency());
            if (bal > 0) return stub;
        }
        return null;
    }
}
