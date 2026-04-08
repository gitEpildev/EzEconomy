package com.skyblockexp.ezeconomy.listener;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.storage.YMLStorageProvider;
import com.skyblockexp.ezeconomy.feature.support.TestSupport;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PlayerJoinListenerTest {
    private Object server;
    private EzEconomyPlugin plugin;

    @BeforeEach
    public void setup() throws Exception {
        server = MockBukkit.mock();
        plugin = TestSupport.loadPlugin(server);
        plugin.getConfig().set("store-on-join.enabled", true);
    }

    @AfterEach
    public void teardown() {
        TestSupport.cleanupTestDataFolder(plugin, "test-store-on-join");
        MockBukkit.unmock();
    }

    @Test
    public void testStoreOnJoin_createsPlayerRecord() throws Exception {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("yml.data-folder", "test-store-on-join");
        cfg.set("yml.per-player-file-naming", "uuid");

        YMLStorageProvider yml = new YMLStorageProvider(plugin, cfg);
        TestSupport.createTestDataFolder(plugin, "test-store-on-join");
        TestSupport.injectField(plugin, "storage", yml);

        // create player which triggers PlayerJoinEvent and our listener
        Object playerObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "joiner");
        org.bukkit.entity.Player player = (org.bukkit.entity.Player) playerObj;

        // allow listener to run
        Thread.sleep(100);

        assertTrue(yml.playerExists(player.getUniqueId()));
    }

    @Test
    public void testStoreOnJoinDisabled_doesNotCreatePlayerRecord() throws Exception {
        plugin.getConfig().set("store-on-join.enabled", false);
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("yml.data-folder", "test-store-on-join");
        cfg.set("yml.per-player-file-naming", "uuid");

        YMLStorageProvider yml = new YMLStorageProvider(plugin, cfg);
        TestSupport.createTestDataFolder(plugin, "test-store-on-join");
        TestSupport.injectField(plugin, "storage", yml);

        Object playerObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "joinerNoStore");
        org.bukkit.entity.Player player = (org.bukkit.entity.Player) playerObj;
        Thread.sleep(100);

        assertFalse(yml.playerExists(player.getUniqueId()));
    }
}
