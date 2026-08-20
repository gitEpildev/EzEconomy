package com.gitepildev.giteconomy.bootstrap.component;

import com.gitepildev.giteconomy.bootstrap.BootstrapComponent;
import com.gitepildev.giteconomy.core.GitEconomyPlugin;
import com.gitepildev.giteconomy.listener.PlayerJoinListener;
import com.gitepildev.giteconomy.listener.PlayerLookupListener;
import org.bukkit.Bukkit;

public class ListenersComponent implements BootstrapComponent {
    private final GitEconomyPlugin plugin;

    public ListenersComponent(GitEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(plugin), plugin);
        // Keep PlayerLookup cache in sync with player activity
        Bukkit.getPluginManager().registerEvents(new PlayerLookupListener(), plugin);
    }

    @Override
    public void stop() {
        // Bukkit unregisters listeners on plugin disable; nothing generic here.
    }

    @Override
    public void reload() {
        // No-op
    }
}
