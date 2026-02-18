package com.skyblockexp.ezeconomy.bootstrap.component;

import com.skyblockexp.ezeconomy.bootstrap.BootstrapComponent;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import org.bukkit.Bukkit;
import net.milkbowl.vault.economy.Economy;

public class EconomyComponent implements BootstrapComponent {
    private final EzEconomyPlugin plugin;

    public EconomyComponent(EzEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        plugin.registerEconomy();
    }

    @Override
    public void stop() {
        try {
            Economy e = plugin.getVaultEconomy();
            if (e != null) {
                Bukkit.getServicesManager().unregister(Economy.class, e);
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void reload() {
        // re-register economy if needed
        start();
    }
}
