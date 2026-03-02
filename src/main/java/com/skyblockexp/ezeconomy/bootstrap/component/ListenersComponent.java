package com.skyblockexp.ezeconomy.bootstrap.component;

import com.skyblockexp.ezeconomy.bootstrap.BootstrapComponent;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.core.Registry;
import com.skyblockexp.ezeconomy.listener.DailyRewardListener;
import com.skyblockexp.ezeconomy.gui.GuiListener;
import org.bukkit.Bukkit;

public class ListenersComponent implements BootstrapComponent {
    private final EzEconomyPlugin plugin;

    public ListenersComponent(EzEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        com.skyblockexp.ezeconomy.manager.DailyRewardManager drm = Registry.get(com.skyblockexp.ezeconomy.manager.DailyRewardManager.class);
        DailyRewardListener drl = new DailyRewardListener(drm);
        GuiListener gl = new GuiListener(plugin);
        Bukkit.getPluginManager().registerEvents(drl, plugin);
        Bukkit.getPluginManager().registerEvents(gl, plugin);

        // register listener instances for potential access
        Registry.register(DailyRewardListener.class, drl);
        Registry.register(GuiListener.class, gl);
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
