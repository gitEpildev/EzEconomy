package com.gitepildev.giteconomy.bootstrap.component;

import com.gitepildev.giteconomy.bootstrap.BootstrapComponent;
import com.gitepildev.giteconomy.core.GitEconomyPlugin;

public class MetricsComponent implements BootstrapComponent {
    private final GitEconomyPlugin plugin;

    public MetricsComponent(GitEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        try {
            plugin.getLogger().info("Initializing metrics...");
            com.gitepildev.giteconomy.core.GitEconomyMetrics m = new com.gitepildev.giteconomy.core.GitEconomyMetrics(plugin);
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
