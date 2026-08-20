package com.gitepildev.giteconomy.papi;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class GitEconomyPapiPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().warning("PlaceholderAPI not found; disabling GitEconomy-PAPI expansion.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register the expansion
        new GitEconomyPAPIExpansion(this).register();
        getLogger().info("GitEconomy-PAPI expansion enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("GitEconomy-PAPI expansion disabled.");
    }
}
