package com.skyblockexp.ezeconomy.bootstrap.component;

import com.skyblockexp.ezeconomy.bootstrap.BootstrapComponent;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;

public class ManagersComponent implements BootstrapComponent {
    private final EzEconomyPlugin plugin;

    public ManagersComponent(EzEconomyPlugin plugin) {
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
        com.skyblockexp.ezeconomy.manager.CurrencyPreferenceManager pref = new com.skyblockexp.ezeconomy.manager.CurrencyPreferenceManager(plugin);
        com.skyblockexp.ezeconomy.manager.CurrencyManager cm = new com.skyblockexp.ezeconomy.manager.CurrencyManager(plugin);

        plugin.setCurrencyPreferenceManager(pref);
        plugin.setCurrencyManager(cm);
    }
}
