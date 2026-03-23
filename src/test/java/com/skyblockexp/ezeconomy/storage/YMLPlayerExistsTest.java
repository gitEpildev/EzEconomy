package com.skyblockexp.ezeconomy.storage;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.feature.support.TestSupport;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.MockBukkit;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class YMLPlayerExistsTest {
    private EzEconomyPlugin plugin;

    @BeforeEach
    public void setUp() {
        MockBukkit.mock();
        plugin = (EzEconomyPlugin) MockBukkit.load(EzEconomyPlugin.class);
    }

    @AfterEach
    public void tearDown() {
        TestSupport.cleanupTestDataFolder(plugin, "test-player-exists");
        MockBukkit.unmock();
    }

    @Test
    public void testPlayerExists_beforeAndAfterSetBalance() throws Exception {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("yml.data-folder", "test-player-exists");
        cfg.set("yml.per-player-file-naming", "uuid");

        YMLStorageProvider yml = new YMLStorageProvider(plugin, cfg);
        TestSupport.createTestDataFolder(plugin, "test-player-exists");

        UUID u = UUID.randomUUID();
        // ensure not present initially
        assertFalse(yml.playerExists(u));

        // create via setBalance and ensure playerExists becomes true
        yml.setBalance(u, plugin.getDefaultCurrency(), 0.0);
        assertTrue(yml.playerExists(u));
    }
}
