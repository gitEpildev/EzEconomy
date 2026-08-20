package com.gitepildev.giteconomy.manager;

import com.gitepildev.giteconomy.core.GitEconomyPlugin;

public class CurrencyManager {
    private final GitEconomyPlugin plugin;
    public CurrencyManager(GitEconomyPlugin plugin) {
        this.plugin = plugin;
    }
    public String getDefaultCurrency() {
        var config = plugin.getConfig();
        boolean multiEnabled = config.getBoolean("multi-currency.enabled", false);
        return multiEnabled ? config.getString("multi-currency.default", "dollar") : "dollar";
    }
}