package com.skyblockexp.ezeconomy.listener;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.manager.DailyRewardManager;
import com.skyblockexp.ezeconomy.api.storage.StorageProvider;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import java.util.Map;
import java.util.UUID;

public class PlayerJoinListener implements Listener {
    private final EzEconomyPlugin plugin;
    private final DailyRewardManager manager;

    public PlayerJoinListener(EzEconomyPlugin plugin, DailyRewardManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Keep existing daily reward behaviour
        manager.handleJoin(event.getPlayer());

        // Always persist correct UUID-to-name mapping from Velocity
        try {
            StorageProvider storage = plugin.getStorageOrWarn();
            if (storage != null) {
                org.bukkit.entity.Player p = event.getPlayer();
                storage.persistPlayerInfo(p.getUniqueId(), p.getName(), p.getDisplayName());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to persist player info on join: " + e.getMessage());
        }

        // Optionally ensure player is stored in the configured storage backend
        if (!plugin.getConfig().getBoolean("store-on-join.enabled", false)) {
            return;
        }

        StorageProvider storage = plugin.getStorageOrWarn();
        if (storage == null) return;

        String currency = plugin.getDefaultCurrency();
        try {
            UUID uuid = event.getPlayer().getUniqueId();
            if (!storage.playerExists(uuid)) {
                com.skyblockexp.ezeconomy.lock.LockManager lm = plugin.getLockManager();
                if (lm != null) {
                    String token = null;
                    try {
                        token = lm.acquire(uuid, 5000L, 50L, 100);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        token = null;
                    }
                    if (token != null) {
                        try {
                            storage.setBalance(uuid, currency, 0.0);
                        } finally {
                            lm.release(uuid, token);
                        }
                    } else {
                        java.util.concurrent.locks.ReentrantLock l = com.skyblockexp.ezeconomy.storage.TransferLockManager.getLock(uuid);
                        l.lock();
                        try {
                            storage.setBalance(uuid, currency, 0.0);
                        } finally {
                            l.unlock();
                        }
                    }
                } else {
                    storage.setBalance(uuid, currency, 0.0);
                }
                plugin.getLogger().info("Stored player " + event.getPlayer().getName() + " on join");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to ensure player stored on join: " + e.getMessage());
        }
    }
}
