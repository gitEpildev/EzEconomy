package com.skyblockexp.ezeconomy.redis.cache;

import com.skyblockexp.ezeconomy.cache.CacheProvider;
import com.skyblockexp.ezeconomy.cache.ExpiringCache;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

/**
 * Redis-backed cache provider using Lettuce. Stores string values.
 */
public class RedisCacheProvider<K, V> implements CacheProvider<K, V>, AutoCloseable {
    private final RedisClient client;
    private final StatefulRedisConnection<String, String> connection;
    private final RedisCommands<String, String> commands;

    public RedisCacheProvider() {
        // Attempt to read redis.yml from plugin data folder; fall back to localhost
        String host = "127.0.0.1";
        int port = 6379;
        int database = 0;
        try {
            EzEconomyPlugin plugin = EzEconomyPlugin.getInstance();
            if (plugin != null) {
                File redisFile = new File(plugin.getDataFolder(), "redis.yml");
                if (!redisFile.exists()) {
                    try { plugin.saveResource("redis.yml", false); } catch (Exception ignored) {}
                }
                FileConfiguration redisCfg = YamlConfiguration.loadConfiguration(redisFile);
                host = redisCfg.getString("host", host);
                port = redisCfg.getInt("port", port);
                database = redisCfg.getInt("database", database);
            }
        } catch (Throwable ignored) {}
        RedisURI uri = RedisURI.builder().withHost(host).withPort(port).withDatabase(database).build();
        this.client = RedisClient.create(uri);
        this.connection = client.connect();
        this.commands = connection.sync();
    }

    private String keyToStr(Object k) { return "ezeconomy:cache:" + String.valueOf(k); }

    @Override
    public ExpiringCache.Entry<V> getEntry(K key) {
        String k = keyToStr(key);
        String v = commands.get(k);
        if (v == null) return null;
        // Lettuce does not return expiry in sync API easily; return non-expiring sentinel
        return new ExpiringCache.Entry<>((V) v, Long.MAX_VALUE);
    }

    @Override
    public void put(K key, V value, long ttlMs) {
        String k = keyToStr(key);
        if (ttlMs > 0) commands.setex(k, Math.max(1, ttlMs / 1000), String.valueOf(value));
        else commands.set(k, String.valueOf(value));
    }

    @Override
    public void remove(K key) {
        commands.del(keyToStr(key));
    }

    @Override
    public void close() throws Exception {
        try { connection.close(); } catch (Exception ignored) {}
        try { client.shutdown(); } catch (Exception ignored) {}
    }
}
