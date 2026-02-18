package com.skyblockexp.ezeconomy.bootstrap.component;

import com.skyblockexp.ezeconomy.bootstrap.BootstrapComponent;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;

public class CommandsComponent implements BootstrapComponent {
    private final EzEconomyPlugin plugin;

    public CommandsComponent(EzEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        plugin.registerCommands();
    }

    @Override
    public void stop() {
        // No generic command unregister API; Bukkit handles plugin disable.
    }

    @Override
    public void reload() {
        // Re-register commands if needed
        plugin.registerCommands();
    }
}
