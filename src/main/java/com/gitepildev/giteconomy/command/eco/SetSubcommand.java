package com.gitepildev.giteconomy.command.eco;

import com.gitepildev.giteconomy.api.storage.StorageProvider;
import com.gitepildev.giteconomy.command.Subcommand;
import com.gitepildev.giteconomy.core.GitEconomyPlugin;
import com.gitepildev.giteconomy.util.MessageUtils;
import com.gitepildev.giteconomy.util.NumberUtil;
import com.gitepildev.giteconomy.core.Money;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.Map;
import java.util.UUID;

/**
 * Subcommand for /eco set <player> <amount>
 */
public class SetSubcommand implements Subcommand {
    private final GitEconomyPlugin plugin;

    public SetSubcommand(GitEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    private OfflinePlayer resolveTarget(String name) {
        org.bukkit.entity.Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        com.gitepildev.giteconomy.api.storage.StorageProvider storage = plugin.getStorageOrWarn();
        if (storage != null) {
            UUID dbUuid = storage.resolvePlayerByName(name);
            if (dbUuid != null) return Bukkit.getOfflinePlayer(dbUuid);
        }
        var maybe = com.gitepildev.giteconomy.util.PlayerLookup.findByName(name);
        if (maybe.isPresent()) return maybe.get();
        return Bukkit.getOfflinePlayer(name);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2 || args.length > 3) {
            MessageUtils.send(sender, plugin, "usage_eco");
            return true;
        }
        OfflinePlayer target = resolveTarget(args[0]);
        Money money = NumberUtil.parseMoney(args[1], null);
        if (money == null || money.getAmount().compareTo(java.math.BigDecimal.ZERO) < 0) {
            MessageUtils.send(sender, plugin, "invalid_amount", java.util.Map.of("input", args[1]));
            return true;
        }
        double amount = money.getAmount().doubleValue();
        StorageProvider storage = plugin.getStorageOrWarn();
        if (storage == null) {
            MessageUtils.send(sender, plugin, "storage_unavailable");
            return true;
        }

        String currency = plugin.getDefaultCurrency();
        if (args.length == 3) {
            currency = args[2].toLowerCase();
            java.util.Map<String, Object> currencies = plugin.getConfig().getConfigurationSection("multi-currency.currencies") != null
                ? plugin.getConfig().getConfigurationSection("multi-currency.currencies").getValues(false)
                : java.util.Collections.emptyMap();
            if (!currencies.containsKey(currency)) {
                MessageUtils.send(sender, plugin, "unknown_currency", java.util.Map.of("currency", currency));
                return true;
            }
        }

        storage.setBalance(target.getUniqueId(), currency, amount);
        String amountWithSymbol = plugin.getCurrencyFormatter().formatPriceForMessage(amount, currency);
        MessageUtils.send(sender, plugin, "set", Map.of("player", target.getName(), "balance", amountWithSymbol));
        return true;
    }
}