package com.skyblockexp.ezeconomy.bootstrap.component;

import com.skyblockexp.ezeconomy.bootstrap.BootstrapComponent;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.core.Registry;
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
        EzEconomyPlaceholderExpansion exp = new EzEconomyPlaceholderExpansion(plugin);
        exp.register();
        Registry.register(EzEconomyPlaceholderExpansion.class, exp);
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
