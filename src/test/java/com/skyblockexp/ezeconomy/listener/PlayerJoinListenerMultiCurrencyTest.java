package com.skyblockexp.ezeconomy.listener;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.storage.YMLStorageProvider;
import com.skyblockexp.ezeconomy.feature.support.TestSupport;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PlayerJoinListenerMultiCurrencyTest {
    private Object server;
    private EzEconomyPlugin plugin;

    @BeforeEach
    public void setup() throws Exception {
        server = MockBukkit.mock();
        plugin = TestSupport.loadPlugin(server);
        // Enable multi-currency and set default currency to 'euro'
        plugin.getConfig().set("multi-currency.enabled", true);
        plugin.getConfig().set("multi-currency.default", "euro");
        plugin.getConfig().set("store-on-join.enabled", true);
    }

    @AfterEach
    public void teardown() {
        TestSupport.cleanupTestDataFolder(plugin, "test-store-on-join-multi");
        MockBukkit.unmock();
    }

    @Test
    public void testStoreOnJoin_createsRecordForDefaultCurrency() throws Exception {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("yml.data-folder", "test-store-on-join-multi");
        cfg.set("yml.per-player-file-naming", "uuid");

        YMLStorageProvider yml = new YMLStorageProvider(plugin, cfg);
        TestSupport.createTestDataFolder(plugin, "test-store-on-join-multi");
        TestSupport.injectField(plugin, "storage", yml);

        // create player which triggers PlayerJoinEvent and our listener
        Object playerObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "multiJoiner");
        org.bukkit.entity.Player player = (org.bukkit.entity.Player) playerObj;

        // allow listener to run
        Thread.sleep(100);

        // default currency should be 'euro' and balance should be 0.0
        assertTrue(yml.playerExists(player.getUniqueId()));
        assertEquals(0.0, yml.getBalance(player.getUniqueId(), "euro"), 0.0001);
    }
}
