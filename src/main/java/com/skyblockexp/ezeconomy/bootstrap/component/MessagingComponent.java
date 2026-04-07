package com.skyblockexp.ezeconomy.bootstrap.component;

import com.skyblockexp.ezeconomy.bootstrap.BootstrapComponent;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.messaging.CrossServerMessenger;
import com.skyblockexp.ezeconomy.storage.MySQLStorageProvider;
import com.skyblockexp.ezeconomy.api.storage.StorageProvider;
import org.bukkit.Bukkit;
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
        messenger = new CrossServerMessenger(plugin);
        messenger.register();
        plugin.setCrossServerMessenger(messenger);
        Bukkit.getPluginManager().registerEvents(this, plugin);

        cleanupTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            StorageProvider s = plugin.getStorage();
            if (s instanceof MySQLStorageProvider) {
                ((MySQLStorageProvider) s).cleanupOldNotifications(86400000L);
            }
        }, 6000L, 6000L).getTaskId();

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
        }
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
