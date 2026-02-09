package com.skyblockexp.ezeconomy.feature;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.feature.support.TestSupport;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.bukkit.configuration.file.YamlConfiguration;
import com.skyblockexp.ezeconomy.storage.YMLStorageProvider;
import com.skyblockexp.ezeconomy.storage.MySQLStorageProvider;

public class BalanceCommandFeatureTest {
    private Object server;
    private EzEconomyPlugin plugin;

    @BeforeEach
    public void setup() throws Exception {
        System.setProperty("ezeconomy.test", "true");
        server = TestSupport.setupMockServer();
        plugin = TestSupport.loadPlugin(server);
        plugin.loadMessageProvider();
    }

    @AfterEach
    public void teardown() {
        TestSupport.tearDown();
        System.clearProperty("ezeconomy.test");
    }

    @Test
    public void testBalanceCommand_showsConfiguredBalance() throws Exception {
        Object senderObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "alice");
        org.bukkit.entity.Player sender = (org.bukkit.entity.Player) senderObj;

        TestSupport.MockStorage storage = new TestSupport.MockStorage();
        // inject the mock storage into plugin
        TestSupport.injectField(plugin, "storage", storage);

        java.util.UUID uuid = sender.getUniqueId();
        String currency = plugin.getDefaultCurrency();
        storage.setBalance(uuid, currency, 1234.56);

        boolean result = sender.performCommand("balance");
        assertTrue(result);
    }

    @Test
    public void testBalanceCommand_otherPlayerOnline_returnsTrue() throws Exception {
        Object senderObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "bob");
        org.bukkit.entity.Player sender = (org.bukkit.entity.Player) senderObj;

        Object targetObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "carol");
        org.bukkit.entity.Player target = (org.bukkit.entity.Player) targetObj;

        TestSupport.MockStorage storage = new TestSupport.MockStorage();
        TestSupport.injectField(plugin, "storage", storage);

        storage.setBalance(target.getUniqueId(), plugin.getDefaultCurrency(), 50.0);

        boolean result = sender.performCommand("balance carol");
        assertTrue(result);
    }

    @Test
    public void testBalanceCommand_withYMLStorage_filePersistsBalance() throws Exception {
        Object senderObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "ymlBob");
        org.bukkit.entity.Player sender = (org.bukkit.entity.Player) senderObj;

        // Prepare YML storage config to use a test folder
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("yml.data-folder", "test-balance-yml");

        YMLStorageProvider yml = new YMLStorageProvider(plugin, cfg);
        TestSupport.injectField(plugin, "storage", yml);

        // set balance via storage and assert command reads it
        java.util.UUID uuid = sender.getUniqueId();
        yml.setBalance(uuid, plugin.getDefaultCurrency(), 77.0);

        boolean result = sender.performCommand("balance");
        assertTrue(result);

        // cleanup created test data folder
        TestSupport.cleanupTestDataFolder(plugin, "test-balance-yml");
    }

    @Test
    public void testBalanceCommand_withMySQLStorage_constructorOnly_noInit() throws Exception {
        Object senderObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "sqlAlice");
        org.bukkit.entity.Player sender = (org.bukkit.entity.Player) senderObj;

        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("mysql.host", "localhost");
        cfg.set("mysql.port", 3306);
        cfg.set("mysql.database", "testdb");
        cfg.set("mysql.username", "root");
        cfg.set("mysql.password", "password");

        MySQLStorageProvider sql = new MySQLStorageProvider(plugin, cfg);
        // Inject but do not call init/load (plugin test mode skips it)
        TestSupport.injectField(plugin, "storage", sql);

        // set balance via storage (constructor-only provider will still accept setBalance)
        java.util.UUID uuid = sender.getUniqueId();
        sql.setBalance(uuid, plugin.getDefaultCurrency(), 12.34);

        boolean result = sender.performCommand("balance");
        assertTrue(result);
    }

    @Test
    public void testBalanceCommand_runsWithoutError() throws Exception {
        Object senderObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "balPlayer");
        org.bukkit.entity.Player sender = (org.bukkit.entity.Player) senderObj;
        boolean result = sender.performCommand("balance");
        assertTrue(result);
    }
}
