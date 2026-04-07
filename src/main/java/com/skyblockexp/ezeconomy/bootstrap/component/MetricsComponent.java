package com.skyblockexp.ezeconomy.bootstrap.component;

import com.skyblockexp.ezeconomy.bootstrap.BootstrapComponent;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;

public class MetricsComponent implements BootstrapComponent {
    private final EzEconomyPlugin plugin;

    public MetricsComponent(EzEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        try {
            plugin.getLogger().info("Initializing metrics...");
            com.skyblockexp.ezeconomy.core.EzEconomyMetrics m = new com.skyblockexp.ezeconomy.core.EzEconomyMetrics(plugin);
            plugin.setMetrics(m);
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to initialize metrics: " + ex.getMessage());
        }
    }

    @Override
    public void stop() {
        // No explicit shutdown for metrics currently
    }

    @Override
    public void reload() {
        // Recreate metrics if desired
        start();
    }
}
