package com.gitepildev.giteconomy.placeholder;

import com.gitepildev.giteconomy.core.GitEconomyPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class GitEconomyPlaceholderExpansion extends PlaceholderExpansion {

    private GitEconomyPlugin plugin;

    public GitEconomyPlaceholderExpansion(GitEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getAuthor() {
        return "GitEpildev";
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "giteconomy";
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String getRequiredPlugin() {
        return "GitEconomy";
    }

    @Override
    public boolean canRegister() {
        return (plugin = (GitEconomyPlugin) Bukkit.getPluginManager().getPlugin(getRequiredPlugin())) != null;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        // Usage:
        // %giteconomy_balance% (default currency)
        // %giteconomy_balance_<currency>%
        // %giteconomy_symbol_<currency>%
        // %giteconomy_top_#% (top # player and balance, e.g. %giteconomy_top_1%)

        var eco = plugin.getEconomy();
        var config = plugin.getConfig();
        boolean multiEnabled = config.getBoolean("multi-currency.enabled", false);
        String defaultCurrency = multiEnabled ? config.getString("multi-currency.default", "dollar") : "dollar";
        String preferredCurrency = defaultCurrency;
        if (player != null) {
            preferredCurrency = plugin.getCurrencyPreferenceManager().getPreferredCurrency(player.getUniqueId());
        }
        com.gitepildev.giteconomy.api.storage.StorageProvider storage = plugin.getStorageOrWarn();

        String[] split = params.toLowerCase().split("_");
        try {
            if (params.equalsIgnoreCase("balance")) {
                if (player == null || storage == null) {
                    return null;
                }
                double bal = storage.getBalance(player.getUniqueId(), preferredCurrency);
                return eco.format(bal);
            }
            // %giteconomy_balance_formatted% or %giteconomy_balance_short% (optional currency suffix)
            if (split.length >= 2 && split[0].equals("balance") && (split[1].equals("formatted") || split[1].equals("short"))) {
                if (player == null || storage == null) return null;
                String currency = (split.length >= 3) ? split[2] : preferredCurrency;
                double bal = storage.getBalance(player.getUniqueId(), currency);
                if (split[1].equals("formatted")) {
                    return plugin.getCurrencyFormatter().formatPriceForMessage(bal, currency);
                } else {
                    return plugin.getCurrencyFormatter().formatShort(bal, currency);
                }
            }

            if (split.length == 2 && split[0].equals("balance")) {
                if (player == null || storage == null) {
                    return null;
                }
                String currency = split[1];
                double bal = storage.getBalance(player.getUniqueId(), currency);
                return eco.format(bal);
            }
            if (split.length == 2 && split[0].equals("symbol")) {
                String currency = split[1];
                if (multiEnabled && config.contains("multi-currency.currencies." + currency + ".symbol")) {
                    return config.getString("multi-currency.currencies." + currency + ".symbol", "$");
                } else if (currency.equals("dollar")) {
                    return "$";
                }
                return "?";
            }

            // Handle %giteconomy_top_1%, %giteconomy_top_2%, etc.
            if (split.length == 2 && split[0].equals("top")) {
                if (storage == null) {
                    return null;
                }
                int rank;
                try {
                    rank = Integer.parseInt(split[1]);
                } catch (NumberFormatException e) {
                    return null;
                }
                if (rank <= 0) {
                    return null;
                }
                // Get all balances for the preferred currency
                java.util.Map<java.util.UUID, Double> balances = storage.getAllBalances(preferredCurrency);
                java.util.List<java.util.Map.Entry<java.util.UUID, Double>> sorted = balances.entrySet().stream()
                        .sorted(java.util.Map.Entry.<java.util.UUID, Double>comparingByValue().reversed())
                        .toList();
                if (rank > sorted.size()) {
                    return null;
                }
                java.util.Map.Entry<java.util.UUID, Double> entry = sorted.get(rank - 1);
                org.bukkit.OfflinePlayer topPlayer = org.bukkit.Bukkit.getOfflinePlayer(entry.getKey());
                // Return formatted string: PlayerName: Balance
                return topPlayer.getName() + ": " + eco.format(entry.getValue());
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
}
