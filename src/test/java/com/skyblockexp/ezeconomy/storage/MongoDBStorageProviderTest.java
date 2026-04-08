package com.skyblockexp.ezeconomy.storage;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MongoDBStorageProviderTest {

    @Mock
    EzEconomyPlugin plugin;

    @Mock
    org.bukkit.configuration.file.YamlConfiguration dbConfig;

    @InjectMocks
    MongoDBStorageProvider provider;

    @Test
    @Disabled("Integration: requires MongoDB/Testcontainers")
    void testCrudOperations() {
        // TODO: spin up Testcontainers MongoDB, perform CRUD and verify results.
    }
}
