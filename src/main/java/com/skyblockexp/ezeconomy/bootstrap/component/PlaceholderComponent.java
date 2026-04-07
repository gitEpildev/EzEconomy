package com.skyblockexp.ezeconomy.bootstrap.component;

import com.skyblockexp.ezeconomy.bootstrap.BootstrapComponent;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import org.bukkit.Bukkit;
import com.skyblockexp.ezeconomy.placeholder.EzEconomyPlaceholderExpansion;

public class PlaceholderComponent implements BootstrapComponent {
    private final EzEconomyPlugin plugin;

    public PlaceholderComponent(EzEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return;
        }
        // If an external dedicated expansion plugin is present, skip internal registration
        if (Bukkit.getPluginManager().getPlugin("EzEconomy-PAPI") != null) {
            plugin.getLogger().info("Detected external EzEconomy-PAPI expansion; skipping built-in placeholders.");
            return;
        }
        new EzEconomyPlaceholderExpansion(plugin).register();
        plugin.getLogger().info("Registered EzEconomy placeholders with PlaceholderAPI.");
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
