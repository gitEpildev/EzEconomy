package com.skyblockexp.ezeconomy.service.format;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;

public class CurrencyFormatter {
    private final EzEconomyPlugin plugin;

    public CurrencyFormatter(EzEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    public String format(double amount) {
        return format(amount, plugin.getDefaultCurrency());
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

    public String formatAmountOnly(double amount, String currency) {
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
        return formatted;
    }

    public String formatShort(double amount, String currency) {
        var cfg = plugin.getConfig();
        // Determine whether short-formatting is enabled and the numeric threshold.
        boolean enabled = cfg.getBoolean("currency.format.short.enabled", true);
        double threshold = cfg.getDouble("currency.format.short.threshold", 1000.0);

        // Allow per-currency overrides for enabled/threshold
        if (currency != null && cfg.getConfigurationSection("multi-currency.currencies") != null) {
            String enabledKey = "multi-currency.currencies." + currency.toLowerCase() + ".short.enabled";
            String thresholdKey = "multi-currency.currencies." + currency.toLowerCase() + ".short.threshold";
            if (cfg.contains(enabledKey)) {
                enabled = cfg.getBoolean(enabledKey, enabled);
            }
            if (cfg.contains(thresholdKey)) {
                threshold = cfg.getDouble(thresholdKey, threshold);
            }
        }

        // If short-format is disabled or amount below threshold, fall back to full format
        if (!enabled || Math.abs(amount) < threshold) {
            return format(amount, currency);
        }

        // global default decimals (allow per-currency override below)
        int decimals = cfg.getInt("currency.format.short.decimals", 1);
        if (currency != null && cfg.getConfigurationSection("multi-currency.currencies") != null) {
            String perKey = "multi-currency.currencies." + currency.toLowerCase() + ".short.decimals";
            if (cfg.contains(perKey)) {
                decimals = cfg.getInt(perKey, decimals);
            }
        }

        String numeric = com.skyblockexp.ezeconomy.util.NumberUtil.formatShort(java.math.BigDecimal.valueOf(amount), decimals);
        if (currency == null) return numeric;
        if (cfg.getConfigurationSection("multi-currency.currencies") == null) {
            return numeric;
        }
        String key = currency.toLowerCase();
        String symbol = cfg.getString("multi-currency.currencies." + key + ".symbol", "");
        String placement = cfg.getString("multi-currency.currencies." + key + ".symbol_placement", "suffix").toLowerCase();
        boolean prefix = placement.equals("prefix") || placement.equals("before");
        if (symbol == null || symbol.isEmpty()) {
            return numeric;
        }
        return prefix ? (symbol + " " + numeric) : (numeric + " " + symbol);
    }

    public String getCurrencySymbol(String currency) {
        if (currency == null) return "";
        var cfg = plugin.getConfig();
        String key = currency.toLowerCase();
        return cfg.getString("multi-currency.currencies." + key + ".symbol", "");
    }

    public String formatPriceForMessage(double amount, String currency) {
        if (plugin.getMessageProvider() != null) {
            return plugin.getMessageProvider().formatPrice(plugin, amount, currency);
        }
        return format(amount, currency);
    }
}
