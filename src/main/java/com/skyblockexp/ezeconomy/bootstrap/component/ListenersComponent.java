package com.skyblockexp.ezeconomy.bootstrap.component;

import com.skyblockexp.ezeconomy.bootstrap.BootstrapComponent;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.core.Registry;
import com.skyblockexp.ezeconomy.listener.DailyRewardListener;
import com.skyblockexp.ezeconomy.gui.GuiInventoryClickListener;
import com.skyblockexp.ezeconomy.gui.GuiPlayerChatListener;
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
        GuiInventoryClickListener inv = new GuiInventoryClickListener(plugin);
        GuiPlayerChatListener chat = new GuiPlayerChatListener(plugin);
        Bukkit.getPluginManager().registerEvents(drl, plugin);
        Bukkit.getPluginManager().registerEvents(inv, plugin);
        Bukkit.getPluginManager().registerEvents(chat, plugin);

        // register listener instances for potential access
        Registry.register(DailyRewardListener.class, drl);
        Registry.register(GuiInventoryClickListener.class, inv);
        Registry.register(GuiPlayerChatListener.class, chat);
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
