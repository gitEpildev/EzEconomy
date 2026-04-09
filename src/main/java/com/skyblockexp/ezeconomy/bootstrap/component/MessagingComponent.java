package com.skyblockexp.ezeconomy.bootstrap.component;

import com.skyblockexp.ezeconomy.bootstrap.BootstrapComponent;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.messaging.CrossServerMessenger;
import com.skyblockexp.ezeconomy.storage.MySQLStorageProvider;
import com.skyblockexp.ezeconomy.api.storage.StorageProvider;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class MessagingComponent implements BootstrapComponent, Listener {
    private final EzEconomyPlugin plugin;
    private CrossServerMessenger messenger;
    private int cleanupTaskId = -1;

    public MessagingComponent(EzEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        if (!plugin.getConfig().getBoolean("cross-server.enabled", false)) {
            plugin.setCrossServerMessenger(null);
            plugin.getLogger().info("Cross-server messaging is disabled in config.");
            return;
        }

        messenger = new CrossServerMessenger(plugin);
        messenger.register();
        plugin.setCrossServerMessenger(messenger);
        Bukkit.getPluginManager().registerEvents(this, plugin);

        StorageProvider s = plugin.getStorage();
        if (s instanceof MySQLStorageProvider) {
            cleanupTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () ->
                ((MySQLStorageProvider) s).cleanupOldNotifications(86400000L),
                6000L, 6000L
            ).getTaskId();
        }

        plugin.getLogger().info("Cross-server messaging component started.");
    }

    @Override
    public void stop() {
        if (cleanupTaskId != -1) {
            Bukkit.getScheduler().cancelTask(cleanupTaskId);
            cleanupTaskId = -1;
        }
        if (messenger != null) {
            messenger.unregister();
            messenger = null;
        }
        plugin.setCrossServerMessenger(null);
        HandlerList.unregisterAll(this);
    }

    @Override
    public void reload() {
        stop();
        start();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (messenger != null) {
            messenger.deliverPendingNotifications(event.getPlayer());
        }
    }
}
