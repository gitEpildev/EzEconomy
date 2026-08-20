package com.gitepildev.giteconomy.listener;

import com.gitepildev.giteconomy.core.GitEconomyPlugin;
import com.gitepildev.giteconomy.api.storage.StorageProvider;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import java.util.UUID;

public class PlayerJoinListener implements Listener {
    private final GitEconomyPlugin plugin;

    public PlayerJoinListener(GitEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Optionally ensure player is stored in the configured storage backend
        if (!plugin.getConfig().getBoolean("store-on-join.enabled", false)) {
            return;
        }

        StorageProvider storage = plugin.getStorageOrWarn();
        if (storage == null) return;

        // Only persist mapping when store-on-join is enabled to avoid write spam.
        try {
            org.bukkit.entity.Player p = event.getPlayer();
            storage.persistPlayerInfo(p.getUniqueId(), p.getName(), p.getDisplayName());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to persist player info on join: " + e.getMessage());
        }

        String currency = plugin.getDefaultCurrency();
        try {
            UUID uuid = event.getPlayer().getUniqueId();
            if (!storage.playerExists(uuid)) {
                com.gitepildev.giteconomy.lock.LockManager lm = plugin.getLockManager();
                long ttlMs = plugin.getLockTtlMs();
                long retryMs = plugin.getLockRetryMs();
                int maxAttempts = plugin.getLockMaxAttempts();
                if (lm != null) {
                    String token = null;
                    try {
                        token = lm.acquire(uuid, ttlMs, retryMs, maxAttempts);
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
                        java.util.concurrent.locks.ReentrantLock l = com.gitepildev.giteconomy.storage.TransferLockManager.getLock(uuid);
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
