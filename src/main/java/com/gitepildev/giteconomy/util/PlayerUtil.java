package com.gitepildev.giteconomy.util;

import com.gitepildev.giteconomy.core.GitEconomyPlugin;
import com.gitepildev.giteconomy.dto.EconomyPlayer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Utilities for resolving player identity information.
 */
public final class PlayerUtil {
    private PlayerUtil() {}

    /**
     * Resolve an EconomyPlayer for a UUID. Prefers online player displayName when available,
     * otherwise falls back to OfflinePlayer name or the UUID string.
     */
    public static EconomyPlayer getPlayer(UUID uuid) {
        if (uuid == null) return null;
        Player online = Bukkit.getPlayer(uuid);
        String name = null;
        String display = null;
        if (online != null) {
            name = online.getName();
            display = online.getDisplayName();
        } else {
            OfflinePlayer off = Bukkit.getOfflinePlayer(uuid);
            if (off != null) name = off.getName();
        }
        if (name == null || name.isEmpty()) {
            name = uuid.toString();
        }
        if (display == null || display.isEmpty()) {
            display = name;
        }
        return new EconomyPlayer(uuid, name, display);
    }
}
