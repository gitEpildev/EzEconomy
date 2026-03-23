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

        // Optionally ensure player is stored in the configured storage backend
        if (!plugin.getConfig().getBoolean("store-on-join.enabled", false)) {
            return;
        }

        StorageProvider storage = plugin.getStorageOrWarn();
        if (storage == null) return;

        String currency = plugin.getDefaultCurrency();
        try {
            Map<UUID, Double> all = storage.getAllBalances(currency);
            if (!all.containsKey(event.getPlayer().getUniqueId())) {
                // Create a record for the player with zero balance
                storage.setBalance(event.getPlayer().getUniqueId(), currency, 0.0);
                plugin.getLogger().info("Stored player " + event.getPlayer().getName() + " on join");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to ensure player stored on join: " + e.getMessage());
        }
    }
}
