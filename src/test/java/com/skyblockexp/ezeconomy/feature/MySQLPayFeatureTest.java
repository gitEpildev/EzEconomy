package com.skyblockexp.ezeconomy.feature;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.storage.MySQLStorageProvider;

import org.bukkit.configuration.file.YamlConfiguration;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MySQLPayFeatureTest {
    private Object server;
    private EzEconomyPlugin plugin;

    @BeforeEach
    public void setup() throws Exception {
        server = MockBukkit.mock();

        // Pre-create plugin data folder and config to force MySQL storage on plugin startup
        java.nio.file.Path pluginDir = java.nio.file.Paths.get("plugins", "EzEconomy");
        java.nio.file.Files.createDirectories(pluginDir);
        java.nio.file.Path cfgPath = pluginDir.resolve("config.yml");
        String cfgContent = "storage: mysql\n";
        java.nio.file.Files.writeString(cfgPath, cfgContent);

        plugin = MockBukkit.load(EzEconomyPlugin.class);

        // Force the plugin config to use MySQL and invoke initializeStorage()
        plugin.getConfig().set("storage", "mysql");
        try {
            java.lang.reflect.Method initStorage = EzEconomyPlugin.class.getDeclaredMethod("initializeStorage");
            initStorage.setAccessible(true);
            initStorage.invoke(plugin);
        } catch (Exception ignored) {
            // If initialization fails (no DB reachable), we'll override the storage below for testing
        }

        // Prepare a minimal YAML config; values won't be used because we'll inject a test Connection
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("mysql.host", "localhost");
        cfg.set("mysql.port", 3306);
        cfg.set("mysql.database", "testdb");
        cfg.set("mysql.username", "sa");
        cfg.set("mysql.password", "");

        MySQLStorageProvider mysql = new MySQLStorageProvider(plugin, cfg);

        // Create an H2 in-memory connection and create the expected tables
        Connection conn = DriverManager.getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=MySQL", "sa", "");
        try (Statement s = conn.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS `balances` (uuid VARCHAR(36), currency VARCHAR(32), balance DOUBLE, PRIMARY KEY (uuid, currency))");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS banks (name VARCHAR(64), currency VARCHAR(32), balance DOUBLE, PRIMARY KEY (name, currency))");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS bank_members (bank VARCHAR(64), uuid VARCHAR(36), owner BOOLEAN, PRIMARY KEY (bank, uuid))");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS transactions (uuid VARCHAR(36), currency VARCHAR(32), amount DOUBLE, timestamp BIGINT)");
        }

        // Inject connection into provider
        Field connField = MySQLStorageProvider.class.getDeclaredField("connection");
        connField.setAccessible(true);
        connField.set(mysql, conn);

        // Inject provider into plugin
        Field storageField = EzEconomyPlugin.class.getDeclaredField("storage");
        storageField.setAccessible(true);
        storageField.set(plugin, mysql);

        plugin.loadMessageProvider();
    }

    @AfterEach
    public void teardown() {
        MockBukkit.unmock();
    }

    @Test
    public void testPayCommand_offlineRecipient_withMySQLStorage() throws Exception {
        // create offline recipient
        Object offlineObj = null;
        try {
            java.lang.reflect.Method addOffline = server.getClass().getMethod("addOfflinePlayer", String.class);
            offlineObj = addOffline.invoke(server, "mysqlOffline");
        } catch (NoSuchMethodException ignored) {
            offlineObj = org.bukkit.Bukkit.getOfflinePlayer("mysqlOffline");
        }
        org.bukkit.OfflinePlayer offline = (org.bukkit.OfflinePlayer) offlineObj;

        // create sender
        Object senderObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "mysqlSender");
        org.bukkit.entity.Player sender = (org.bukkit.entity.Player) senderObj;
        sender.setOp(true);

        // get provider and initialize balances
        Field storageField = EzEconomyPlugin.class.getDeclaredField("storage");
        storageField.setAccessible(true);
        MySQLStorageProvider storage = (MySQLStorageProvider) storageField.get(plugin);

        storage.setBalance(sender.getUniqueId(), "dollar", 10.0);
        storage.setBalance(offline.getUniqueId(), "dollar", 0.0);

        // perform command which will invoke MySQL transfer path
        sender.performCommand("pay mysqlOffline 5");

        // wait briefly for any async actions
        Thread.sleep(300);

        assertEquals(5.0, storage.getBalance(sender.getUniqueId(), "dollar"), 0.0001);
        assertEquals(5.0, storage.getBalance(offline.getUniqueId(), "dollar"), 0.0001);
    }
}
