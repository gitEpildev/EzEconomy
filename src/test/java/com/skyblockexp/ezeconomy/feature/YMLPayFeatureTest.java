package com.skyblockexp.ezeconomy.feature;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.storage.YMLStorageProvider;
import com.skyblockexp.ezeconomy.feature.support.TestSupport;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class YMLPayFeatureTest {
    private Object server;
    private EzEconomyPlugin plugin;

    @BeforeEach
    public void setup() throws Exception {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(EzEconomyPlugin.class);

        // Create a YML storage provider using a test data folder under the plugin folder
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("yml.data-folder", "test-data");
        cfg.set("yml.per-player-file-naming", "uuid");
        YMLStorageProvider yml = new YMLStorageProvider(plugin, cfg);

        // Ensure test-data folder exists under plugin data
        TestSupport.createTestDataFolder(plugin, "test-data");

        // Inject into plugin
        TestSupport.injectField(plugin, "storage", yml);

        plugin.loadMessageProvider();
    }

    @AfterEach
    public void teardown() {
        TestSupport.cleanupTestDataFolder(plugin, "test-data");
        MockBukkit.unmock();
    }

    @Test
    public void testPayCommand_offlineRecipient_withYmlStorage() throws Exception {
        // create offline recipient
        Object offlineObj = null;
        try {
            java.lang.reflect.Method addOffline = server.getClass().getMethod("addOfflinePlayer", String.class);
            offlineObj = addOffline.invoke(server, "ymlOffline");
        } catch (NoSuchMethodException ignored) {
            offlineObj = org.bukkit.Bukkit.getOfflinePlayer("ymlOffline");
        }
        org.bukkit.OfflinePlayer offline = (org.bukkit.OfflinePlayer) offlineObj;

        // create sender
        Object senderObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "ymlSender");
        org.bukkit.entity.Player sender = (org.bukkit.entity.Player) senderObj;
        sender.setOp(true);

        // initialize balances via the plugin's storage
        Field storageField = EzEconomyPlugin.class.getDeclaredField("storage");
        storageField.setAccessible(true);
        YMLStorageProvider storage = (YMLStorageProvider) storageField.get(plugin);

        storage.setBalance(sender.getUniqueId(), "dollar", 10.0);
        storage.setBalance(offline.getUniqueId(), "dollar", 0.0);

        // perform command which will invoke YML transfer path
        sender.performCommand("pay ymlOffline 5");

        // give async path a moment
        Thread.sleep(300);

        assertEquals(5.0, storage.getBalance(sender.getUniqueId(), "dollar"), 0.0001);
        assertEquals(5.0, storage.getBalance(offline.getUniqueId(), "dollar"), 0.0001);
    }
}
