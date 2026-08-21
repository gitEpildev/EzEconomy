package com.gitepildev.giteconomy.bootstrap.component;

import com.gitepildev.giteconomy.bootstrap.BootstrapComponent;
import com.gitepildev.giteconomy.core.GitEconomyPlugin;
import org.bukkit.Bukkit;
import net.milkbowl.vault.economy.Economy;

public class EconomyComponent implements BootstrapComponent {
    private final GitEconomyPlugin plugin;

    public EconomyComponent(GitEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        com.gitepildev.giteconomy.core.VaultEconomyImpl impl = new com.gitepildev.giteconomy.core.VaultEconomyImpl(plugin);
        org.bukkit.Bukkit.getServicesManager().register(net.milkbowl.vault.economy.Economy.class, impl, plugin, org.bukkit.plugin.ServicePriority.Highest);
        plugin.setVaultEconomy(impl);
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
