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
        plugin.initializeManagers();
    }

    @Override
    public void stop() {
        if (plugin.getBankInterestManager() != null) {
            try { plugin.getBankInterestManager().stop(); } catch (Exception ignored) {}
        }
    }

    @Override
    public void reload() {
        // re-initialize managers if required
        plugin.initializeManagers();
    }
}
