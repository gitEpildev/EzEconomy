
package com.skyblockexp.ezeconomy.command;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.util.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import java.util.*;
import java.util.stream.Collectors;

public class BaltopCommand implements CommandExecutor {
    private final EzEconomyPlugin plugin;
    private static final int DEFAULT_TOP = 10;
    private static final int PAGE_SIZE = 10;

    public BaltopCommand(EzEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Use MessageUtils for localized messages
        int top = DEFAULT_TOP;
        boolean usePaging = false;
        int page = 1;
        // Determine optional currency argument (last arg if it matches a currency id)
        String currency = plugin.getDefaultCurrency();
        java.util.Map<String, Object> currencies = plugin.getConfig().getConfigurationSection("multi-currency.currencies") != null
            ? plugin.getConfig().getConfigurationSection("multi-currency.currencies").getValues(false)
            : java.util.Collections.emptyMap();
        if (args.length == 2 && args[0].equalsIgnoreCase("page")) {
            usePaging = true;
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
            }
        } else if (args.length == 1) {
            // single argument could be top or currency
            try {
                top = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
                String maybeCurrency = args[0].toLowerCase();
                if (currencies.containsKey(maybeCurrency)) {
                    currency = maybeCurrency;
                }
            }
        } else if (args.length == 2) {
            // could be top + currency
            try {
                top = Integer.parseInt(args[0]);
                String maybeCurrency = args[1].toLowerCase();
                if (currencies.containsKey(maybeCurrency)) {
                    currency = maybeCurrency;
                }
            } catch (NumberFormatException ignored) {
                // ignore
            }
        }
        com.skyblockexp.ezeconomy.api.storage.StorageProvider storage = plugin.getStorageOrWarn();
        if (storage == null) {
            MessageUtils.send(sender, plugin, "storage_unavailable");
            return true;
        }
        Map<UUID, Double> balances = storage.getAllBalances(currency);
        List<Map.Entry<UUID, Double>> sorted = balances.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .collect(Collectors.toList());
        if (usePaging) {
            int totalEntries = sorted.size();
            int totalPages = (int) Math.ceil(totalEntries / (double) PAGE_SIZE);
            if (totalPages == 0) {
                totalPages = 1;
            }
            if (page <= 0 || page > totalPages) {
                MessageUtils.send(sender, plugin, "baltop_invalid_page", java.util.Map.of(
                    "page", String.valueOf(page),
                    "total_pages", String.valueOf(totalPages),
                    "page_size", String.valueOf(PAGE_SIZE)
                ));
                return true;
            }
                MessageUtils.send(sender, plugin, "top_balances_page", java.util.Map.of(
                    "page", String.valueOf(page),
                    "total_pages", String.valueOf(totalPages),
                    "page_size", String.valueOf(PAGE_SIZE)
                ));
            int startIndex = (page - 1) * PAGE_SIZE;
            int endIndex = Math.min(startIndex + PAGE_SIZE, totalEntries);
            sorted = sorted.subList(startIndex, endIndex);
            int rank = startIndex + 1;
            for (Map.Entry<UUID, Double> entry : sorted) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
                MessageUtils.send(sender, plugin, "rank_balance", java.util.Map.of(
                    "rank", String.valueOf(rank),
                    "player", player.getName(),
                    "balance", plugin.format(entry.getValue(), currency)
                ));
                rank++;
            }
            return true;
        }
        if (top > 0 && sorted.size() > top) {
            sorted = sorted.subList(0, top);
        }
        MessageUtils.send(sender, plugin, "top_balances", java.util.Map.of("top", String.valueOf(top)));
        int rank = 1;
        for (Map.Entry<UUID, Double> entry : sorted) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
            String playerName = player.getName();
            if (playerName == null) {
                playerName = MessageUtils.format(plugin, "unknown_player");
                plugin.getLogger().warning("[Baltop] Could not resolve player name for UUID: " + entry.getKey());
            }
            MessageUtils.send(sender, plugin, "rank_balance", java.util.Map.of(
                "rank", String.valueOf(rank),
                "player", playerName,
                "balance", plugin.format(entry.getValue(), currency)
            ));
            rank++;
        }
        return true;
    }
}
