package com.skyblockexp.ezeconomy.storage;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.feature.support.TestSupport;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.MockBukkit;

import java.io.File;
import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class SQLiteProviderPlayerExistsTest {
    private EzEconomyPlugin plugin;

    @BeforeEach
    public void setUp() {
        MockBukkit.mock();
        plugin = (EzEconomyPlugin) MockBukkit.load(EzEconomyPlugin.class);
    }

    @AfterEach
    public void tearDown() {
        TestSupport.cleanupTestDataFolder(plugin, "test-sqlite");
        MockBukkit.unmock();
    }

    @Test
    public void testPlayerExists_beforeAndAfterSetBalance() throws Exception {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("sqlite.file", "test-sqlite.db");
        cfg.set("sqlite.table", "balances");

        SQLiteStorageProvider sqlite = new SQLiteStorageProvider(plugin, cfg);

        // ensure DB file is created under plugin data folder
        File db = new File(plugin.getDataFolder(), "test-sqlite.db");
        assertTrue(db.exists());

        UUID u = UUID.randomUUID();
        assertFalse(sqlite.playerExists(u));

        sqlite.setBalance(u, plugin.getDefaultCurrency(), 1.0);
        assertTrue(sqlite.playerExists(u));
    }
}
