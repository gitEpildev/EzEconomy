package com.skyblockexp.ezeconomy.storage.cache;

import com.skyblockexp.ezeconomy.cache.CacheProvider;
import com.skyblockexp.ezeconomy.cache.ExpiringCache;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Database-backed cache provider using MySQL (configured via plugin config).
 * Creates a simple `ezeconomy_cache` table if missing.
 */
public class DatabaseCacheProvider<K, V> implements CacheProvider<K, V> {
    private Connection conn;

    public DatabaseCacheProvider() {
        try {
            EzEconomyPlugin plugin = EzEconomyPlugin.getInstance();
            if (plugin == null) return;
            String host = plugin.getConfig().getString("mysql.host", "127.0.0.1");
            int port = plugin.getConfig().getInt("mysql.port", 3306);
            String database = plugin.getConfig().getString("mysql.database", "ezeconomy");
            String user = plugin.getConfig().getString("mysql.username", "root");
            String pass = plugin.getConfig().getString("mysql.password", "");
            this.conn = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database, user, pass);
            try (Statement s = conn.createStatement()) {
                s.executeUpdate("CREATE TABLE IF NOT EXISTS ezeconomy_cache (`k` VARCHAR(191) PRIMARY KEY, `v` TEXT, `expiresAt` BIGINT)");
            }
        } catch (Exception ex) {
            // leave conn null to indicate unavailable
            this.conn = null;
        }
    }

    @Override
    public ExpiringCache.Entry<V> getEntry(K key) {
        if (conn == null) return null;
        try (PreparedStatement ps = conn.prepareStatement("SELECT v, expiresAt FROM ezeconomy_cache WHERE k=?")) {
            ps.setString(1, String.valueOf(key));
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            String v = rs.getString(1);
            long expiresAt = rs.getLong(2);
            if (expiresAt > 0 && expiresAt <= System.currentTimeMillis()) return null;
            return new ExpiringCache.Entry<>((V) v, expiresAt);
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public void put(K key, V value, long ttlMs) {
        if (conn == null) return;
        long expiresAt = ttlMs <= 0 ? 0L : System.currentTimeMillis() + ttlMs;
        try (PreparedStatement ps = conn.prepareStatement("REPLACE INTO ezeconomy_cache (k, v, expiresAt) VALUES (?, ?, ?)")) {
            ps.setString(1, String.valueOf(key));
            ps.setString(2, String.valueOf(value));
            ps.setLong(3, expiresAt);
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }

    @Override
    public void remove(K key) {
        if (conn == null) return;
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM ezeconomy_cache WHERE k=?")) {
            ps.setString(1, String.valueOf(key));
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }
}
