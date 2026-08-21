package com.gitepildev.giteconomy.bootstrap.component;

import com.gitepildev.giteconomy.bootstrap.BootstrapComponent;
import com.gitepildev.giteconomy.core.GitEconomyPlugin;

public class ManagersComponent implements BootstrapComponent {
    private final GitEconomyPlugin plugin;

    public ManagersComponent(GitEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        initManagers();
    }

    @Override
    public void stop() {
        // Currency managers have no stop lifecycle.
    }

    @Override
    public void reload() {
        // Re-initialize managers in-place; keep lifecycle inside this component
        initManagers();
    }

    private void initManagers() {
        com.gitepildev.giteconomy.manager.CurrencyPreferenceManager pref = new com.gitepildev.giteconomy.manager.CurrencyPreferenceManager(plugin);
        com.gitepildev.giteconomy.manager.CurrencyManager cm = new com.gitepildev.giteconomy.manager.CurrencyManager(plugin);

        plugin.setCurrencyPreferenceManager(pref);
        plugin.setCurrencyManager(cm);
    }
}
