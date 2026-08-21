package com.gitepildev.giteconomy.bootstrap.component;

import com.gitepildev.giteconomy.bootstrap.BootstrapComponent;
import com.gitepildev.giteconomy.core.GitEconomyPlugin;
import org.bukkit.Bukkit;
import com.gitepildev.giteconomy.placeholder.GitEconomyPlaceholderExpansion;

public class PlaceholderComponent implements BootstrapComponent {
    private final GitEconomyPlugin plugin;

    public PlaceholderComponent(GitEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return;
        }
        // If an external dedicated expansion plugin is present, skip internal registration
        if (Bukkit.getPluginManager().getPlugin("GitEconomy-PAPI") != null) {
            plugin.getLogger().info("Detected external GitEconomy-PAPI expansion; skipping built-in placeholders.");
            return;
        }
        new GitEconomyPlaceholderExpansion(plugin).register();
        plugin.getLogger().info("Registered GitEconomy placeholders with PlaceholderAPI.");
    }

    @Override
    public void stop() {
        // PlaceholderAPI handles unregistration when plugin disables
    }

    @Override
    public void reload() {
        start();
    }
}
