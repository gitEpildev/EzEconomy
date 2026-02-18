package com.skyblockexp.ezeconomy.bootstrap.component;

import com.skyblockexp.ezeconomy.bootstrap.BootstrapComponent;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;

public class GuiComponent implements BootstrapComponent {
    private final EzEconomyPlugin plugin;

    public GuiComponent(EzEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        plugin.loadUserGuiConfig();
    }

    @Override
    public void stop() {
        // no-op
    }

    @Override
    public void reload() {
        plugin.loadUserGuiConfig();
    }
}
