package com.skyblockexp.ezeconomy.util;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper utility to perform name -> {@link OfflinePlayer} lookups without
 * repeatedly looping over the full {@link Bukkit#getOfflinePlayers()} array
 * across code paths. It keeps a simple cached mapping of lower-cased player
 * names to {@link OfflinePlayer} instances and falls back to online exact
 * matches first to avoid any blocking or expensive lookups.
 */
public final class PlayerLookup {
    private static final Map<String, OfflinePlayer> CACHE = new ConcurrentHashMap<>();

    private PlayerLookup() {}

    /**
     * Find an {@link OfflinePlayer} by name.
     * - Checks online exact match first (cheap).
     * - Consults an internal cache built from {@link Bukkit#getOfflinePlayers()}.
     * - If not found returns empty to avoid triggering Mojang lookups.
     */
    public static Optional<OfflinePlayer> findByName(String name) {
        if (name == null) return Optional.empty();
        // Prefer direct online exact match
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return Optional.of(online);

        String key = name.toLowerCase(Locale.ROOT);
        // Try cache first
        OfflinePlayer cached = CACHE.get(key);
        if (cached != null) return Optional.of(cached);

        // Build cache lazily if empty; this avoids rebuilding on every call.
        if (CACHE.isEmpty()) {
            for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                if (op.getName() != null) {
                    CACHE.put(op.getName().toLowerCase(Locale.ROOT), op);
                }
            }
            cached = CACHE.get(key);
            if (cached != null) return Optional.of(cached);
        }

        // Not found in cache; do not call Bukkit#getOfflinePlayer(name) here to
        // avoid implementations that may do network lookups.
        return Optional.empty();
    }

    /**
     * Force-refresh the internal cache from {@link Bukkit#getOfflinePlayers()}.
     * Call this after migration/restore operations if callers need up-to-date
     * information. This runs on the calling thread and should be used sparingly.
     */
    public static void refreshCache() {
        CACHE.clear();
        for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
            if (op.getName() != null) {
                CACHE.put(op.getName().toLowerCase(Locale.ROOT), op);
            }
        }
    }

    /**
     * Add or update a single {@link OfflinePlayer} entry in the cache.
     * Safe to call from any thread; uses a concurrent map.
     */
    public static void addToCache(OfflinePlayer op) {
        if (op == null) return;
        String name = op.getName();
        if (name == null) return;
        CACHE.put(name.toLowerCase(Locale.ROOT), op);
    }

    /**
     * Remove a player from the cache by their OfflinePlayer reference.
     */
    public static void removeFromCache(OfflinePlayer op) {
        if (op == null) return;
        String name = op.getName();
        if (name == null) return;
        CACHE.remove(name.toLowerCase(Locale.ROOT));
    }

    /**
     * Return cached player names that start with the provided prefix (case-insensitive).
     * Builds the cache lazily if needed.
     */
    public static java.util.List<String> namesStartingWith(String prefix) {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (prefix == null) return out;
        String p = prefix.toLowerCase(Locale.ROOT);
        if (CACHE.isEmpty()) refreshCache();
        for (OfflinePlayer op : CACHE.values()) {
            String n = op.getName();
            if (n != null && n.toLowerCase(Locale.ROOT).startsWith(p)) out.add(n);
        }
        return out;
    }
}
