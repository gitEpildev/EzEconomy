package com.gitepildev.giteconomy.storage;

import com.gitepildev.giteconomy.core.GitEconomyPlugin;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MySQLStorageProviderTest {

    @Mock
    GitEconomyPlugin plugin;

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
