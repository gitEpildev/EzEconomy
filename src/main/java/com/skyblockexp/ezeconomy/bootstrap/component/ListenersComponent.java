package com.skyblockexp.ezeconomy.bootstrap.component;

import com.skyblockexp.ezeconomy.bootstrap.BootstrapComponent;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.listener.PlayerJoinListener;
import com.skyblockexp.ezeconomy.gui.GuiListener;
import com.skyblockexp.ezeconomy.listener.PlayerLookupListener;
import org.bukkit.Bukkit;

public class ListenersComponent implements BootstrapComponent {
    private final EzEconomyPlugin plugin;

    public ListenersComponent(EzEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(plugin, plugin.getDailyRewardManager()), plugin);
        // Keep PlayerLookup cache in sync with player activity
        Bukkit.getPluginManager().registerEvents(new PlayerLookupListener(), plugin);
        Bukkit.getPluginManager().registerEvents(new GuiListener(plugin), plugin);
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
