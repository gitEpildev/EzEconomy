package com.skyblockexp.ezeconomy.bootstrap.component;

import com.skyblockexp.ezeconomy.bootstrap.BootstrapComponent;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;

public class PlaceholderComponent implements BootstrapComponent {
    private final EzEconomyPlugin plugin;

    public PlaceholderComponent(EzEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        plugin.registerPlaceholderExpansion();
    }

    @Override
    public void stop() {
        // PlaceholderAPI handles unregistration when plugin disables
    }

    @Override
    public void reload() {
        plugin.registerPlaceholderExpansion();
    }
}
