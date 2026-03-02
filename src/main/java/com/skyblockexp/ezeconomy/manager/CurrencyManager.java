package com.skyblockexp.ezeconomy.manager;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;

public class CurrencyManager {
    private final EzEconomyPlugin plugin;
    public CurrencyManager(EzEconomyPlugin plugin) {
        this.plugin = plugin;
    }
    public String getDefaultCurrency() {
        var config = plugin.getConfig();
        boolean multiEnabled = config.getBoolean("multi-currency.enabled", false);
        return multiEnabled ? config.getString("multi-currency.default", "dollar") : "dollar";
    }

    public String format(double amount) {
        return format(amount, getDefaultCurrency());
    }

    public String format(double amount, String currency) {
        if (currency == null) {
            java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(java.util.Locale.getDefault());
            nf.setGroupingUsed(true);
            nf.setMinimumFractionDigits(2);
            nf.setMaximumFractionDigits(2);
            return nf.format(amount);
        }
        var cfg = plugin.getConfig();
        if (cfg.getConfigurationSection("multi-currency.currencies") == null) {
            java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(java.util.Locale.getDefault());
            nf.setGroupingUsed(true);
            nf.setMinimumFractionDigits(2);
            nf.setMaximumFractionDigits(2);
            return nf.format(amount);
        }
        String key = currency.toLowerCase();
        String symbol = cfg.getString("multi-currency.currencies." + key + ".symbol", "");
        int decimals = cfg.getInt("multi-currency.currencies." + key + ".decimals", 2);

        // Locale configuration: server-wide override optional
        String localeCfg = cfg.getString("currency.format.locale", "");
        java.util.Locale locale = java.util.Locale.getDefault();
        if (localeCfg != null && !localeCfg.isBlank()) {
            String[] parts = localeCfg.split("[_-]");
            if (parts.length == 1) locale = new java.util.Locale(parts[0]);
            else locale = new java.util.Locale(parts[0], parts[1]);
        }

        java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(locale);
        nf.setGroupingUsed(true);
        nf.setMinimumFractionDigits(decimals);
        nf.setMaximumFractionDigits(decimals);
        String formatted = nf.format(java.math.BigDecimal.valueOf(amount).setScale(decimals, java.math.RoundingMode.HALF_UP));

        // Symbol placement: optional per-currency setting (prefix/suffix)
        String placement = cfg.getString("multi-currency.currencies." + key + ".symbol_placement", "suffix").toLowerCase();
        boolean prefix = placement.equals("prefix") || placement.equals("before");
        if (symbol == null || symbol.isEmpty()) {
            return formatted;
        }
        return prefix ? (symbol + " " + formatted) : (formatted + " " + symbol);
    }
}