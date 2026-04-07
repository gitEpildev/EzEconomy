package com.skyblockexp.ezeconomy.storage;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MySQLStorageProviderTest {

    @Mock
    EzEconomyPlugin plugin;

    @Mock
    org.bukkit.configuration.file.YamlConfiguration dbConfig;

    @InjectMocks
    MySQLStorageProvider provider;

    @Test
    @Disabled("Integration: requires MySQL/Testcontainers")
    void testConnection_andTransactions() {
        // TODO: use Testcontainers MySQL or H2 compatibility mode to test connection and transaction/rollback behavior.
    }
}
