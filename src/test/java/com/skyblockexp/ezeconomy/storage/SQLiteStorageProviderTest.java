package com.skyblockexp.ezeconomy.storage;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SQLiteStorageProviderTest {

    @Mock
    EzEconomyPlugin plugin;

    @Mock
    org.bukkit.configuration.file.YamlConfiguration dbConfig;

    @InjectMocks
    SQLiteStorageProvider provider;

    @Test
    void testSaveAndLoadPlayer_persistsBalances() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "SQLite JDBC driver not available; skipping integration test");
            return;
        }
        java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("sqlite-test");
        try {
            org.mockito.Mockito.when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
            org.bukkit.configuration.file.YamlConfiguration cfg = new org.bukkit.configuration.file.YamlConfiguration();
            cfg.set("sqlite.file", "test.db");
            SQLiteStorageProvider p = new SQLiteStorageProvider(plugin, cfg);
            java.util.UUID id = java.util.UUID.randomUUID();
            p.setBalance(id, "dollar", 7.25);
            double loaded = p.getBalance(id, "dollar");
            assertEquals(7.25, loaded, 0.0001);
        } finally {
            java.nio.file.Files.walk(tempDir).map(java.nio.file.Path::toFile).forEach(java.io.File::delete);
        }
    }

    @Test
    @Disabled("Integration: requires DB setup")
    void testAtomicTransfer_rollsBackOnFailure() {
        // TODO: Integration test that simulates a failure during transfer and asserts rollback.
    }
}
