package com.skyblockexp.ezeconomy.bootstrap.component;

import com.skyblockexp.ezeconomy.bootstrap.BootstrapComponent;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import org.bukkit.Bukkit;
import net.milkbowl.vault.economy.Economy;

public class ShutdownComponent implements BootstrapComponent {
    private final EzEconomyPlugin plugin;

    public ShutdownComponent(EzEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        // not started during startup
    }

    @Override
    public void stop() {
        try {
            Bukkit.getServicesManager().unregister(Economy.class, plugin.getVaultEconomy());
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to unregister Vault service: " + ex.getMessage());
        }
    }

    @Override
    public void reload() {
        // no-op
    }
}
