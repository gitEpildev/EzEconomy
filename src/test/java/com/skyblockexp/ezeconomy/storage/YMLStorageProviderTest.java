package com.skyblockexp.ezeconomy.storage;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class YMLStorageProviderTest {

    @Mock
    EzEconomyPlugin plugin;

    @InjectMocks
    YMLStorageProvider provider;

    @Test
    void testSaveAndLoad_withTempFile() throws Exception {
        Path tempDir = Files.createTempDirectory("yml-test");
        try {
            org.mockito.Mockito.when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
            org.bukkit.configuration.file.YamlConfiguration cfg = new org.bukkit.configuration.file.YamlConfiguration();
            cfg.set("yml.data-folder", "data");
            YMLStorageProvider p = new YMLStorageProvider(plugin, cfg);
            java.util.UUID id = java.util.UUID.randomUUID();
            p.setBalance(id, "dollar", 12.5);
            double loaded = p.getBalance(id, "dollar");
            assertEquals(12.5, loaded, 0.0001);
            assertTrue(p.playerExists(id));
        } finally {
            Files.walk(tempDir).map(Path::toFile).forEach(java.io.File::delete);
        }
    }
}
