package com.skyblockexp.ezeconomy.storage;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.feature.support.TestSupport;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.MockBukkit;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class MySQLProviderPlayerExistsTest {
    private EzEconomyPlugin plugin;

    @BeforeEach
    public void setUp() {
        MockBukkit.mock();
        plugin = (EzEconomyPlugin) MockBukkit.load(EzEconomyPlugin.class);
    }

    @AfterEach
    public void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    public void testPlayerExists_withH2Memory() throws Exception {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("mysql.table", "balances");

        MySQLStorageProvider mysql = new MySQLStorageProvider(plugin, cfg);

        // create an H2 in-memory connection compatible with MySQL mode
        Connection conn = DriverManager.getConnection("jdbc:h2:mem:testmysql;MODE=MySQL;DB_CLOSE_DELAY=-1");
        Statement st = conn.createStatement();
        st.executeUpdate("CREATE TABLE IF NOT EXISTS `balances` (uuid VARCHAR(36), currency VARCHAR(32), balance DOUBLE, PRIMARY KEY (uuid, currency))");

        // inject connection into provider
        Field connField = MySQLStorageProvider.class.getDeclaredField("connection");
        connField.setAccessible(true);
        connField.set(mysql, conn);

        UUID u = UUID.randomUUID();
        assertFalse(mysql.playerExists(u));

        mysql.setBalance(u, plugin.getDefaultCurrency(), 7.0);
        assertTrue(mysql.playerExists(u));
    }
}
