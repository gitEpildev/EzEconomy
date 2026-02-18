package com.skyblockexp.ezeconomy.bootstrap.component;

import com.skyblockexp.ezeconomy.bootstrap.BootstrapComponent;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;

public class ListenersComponent implements BootstrapComponent {
    private final EzEconomyPlugin plugin;

    public ListenersComponent(EzEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        plugin.registerListeners();
    }

    @Override
    public void stop() {
        // Bukkit unregisters listeners on plugin disable; nothing generic here.
    }

    @Override
    public void reload() {
        // No-op
    }
}
