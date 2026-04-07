package com.skyblockexp.ezeconomy.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerKickEvent;

import com.skyblockexp.ezeconomy.util.PlayerLookup;

/**
 * Keeps the PlayerLookup cache in sync with join/quit activity.
 */
public class PlayerLookupListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        PlayerLookup.addToCache(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        PlayerLookup.removeFromCache(e.getPlayer());
    }

    @EventHandler
    public void onKick(PlayerKickEvent e) {
        PlayerLookup.removeFromCache(e.getPlayer());
    }
}
