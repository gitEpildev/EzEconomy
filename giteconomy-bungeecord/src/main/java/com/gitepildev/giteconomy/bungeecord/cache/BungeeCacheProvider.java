package com.gitepildev.giteconomy.bungeecord.cache;

import com.gitepildev.giteconomy.cache.CacheProvider;
import com.gitepildev.giteconomy.cache.ExpiringCache;
import com.gitepildev.giteconomy.lock.transport.PluginMessagingTransport;
import com.gitepildev.giteconomy.core.GitEconomyPlugin;

/**
 * Bungee cache provider that uses plugin messaging transport to forward cache
 * requests to the proxy-side GitBungeeProxy. Falls back to local cache when
 * messaging is unavailable.
 */
public class BungeeCacheProvider<K, V> implements CacheProvider<K, V> {
    private final PluginMessagingTransport transport;
    private final ExpiringCache<K, V> fallback = new ExpiringCache<>();

    public BungeeCacheProvider() {
        PluginMessagingTransport t = null;
        try {
            GitEconomyPlugin plugin = GitEconomyPlugin.getInstance();
            if (plugin != null) {
                String channel = plugin.getConfig().getString("bungee.channel", "ez:economy");
                long timeout = plugin.getConfig().getLong("bungee.response-timeout-ms", 5000L);
                t = new PluginMessagingTransport(plugin, channel, timeout);
            }
        } catch (Throwable ignored) {}
        this.transport = t;
    }

    @Override
    public ExpiringCache.Entry<V> getEntry(K key) {
        if (transport == null) return fallback.getEntry(key);
        try {
            String v = transport.getCache(String.valueOf(key), 2000L);
            if (v == null) return null;
            return new ExpiringCache.Entry<>((V) v, Long.MAX_VALUE);
        } catch (Throwable t) {
            return fallback.getEntry(key);
        }
    }

    @Override
    public void put(K key, V value, long ttlMs) {
        if (transport == null) { fallback.put(key, value, ttlMs); return; }
        try { transport.setCache(String.valueOf(key), String.valueOf(value), ttlMs); } catch (Throwable ignored) { fallback.put(key, value, ttlMs); }
    }

    @Override
    public void remove(K key) {
        if (transport == null) { fallback.remove(key); return; }
        try { transport.setCache(String.valueOf(key), "", 0); } catch (Throwable ignored) { fallback.remove(key); }
    }
}
