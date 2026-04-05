package com.skyblockexp.ezeconomy.storage;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class MySQLStorageProviderIntegrationTest {

    @Test
    void testSetAndGetBalance_withTestcontainers() throws Exception {
        try {
            Class.forName("org.testcontainers.containers.MySQLContainer");
        } catch (ClassNotFoundException e) {
            Assumptions.assumeTrue(false, "Testcontainers not on classpath; skipping integration test");
            return;
        }

        // Use reflection to avoid compile-time dependency if Testcontainers is absent
        org.testcontainers.containers.MySQLContainer<?> mysql = new org.testcontainers.containers.MySQLContainer<>("mysql:8.0")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test");

        try {
            mysql.start();
        } catch (Throwable t) {
            Assumptions.assumeTrue(false, "Docker/Testcontainers not available: " + t.getMessage());
            return;
        }

        try {
            EzEconomyPlugin plugin = mock(EzEconomyPlugin.class);

            YamlConfiguration cfg = new YamlConfiguration();
            cfg.set("mysql.host", mysql.getHost());
            cfg.set("mysql.port", mysql.getFirstMappedPort());
            cfg.set("mysql.database", mysql.getDatabaseName());
            cfg.set("mysql.username", mysql.getUsername());
            cfg.set("mysql.password", mysql.getPassword());

            MySQLStorageProvider provider = new MySQLStorageProvider(plugin, cfg);
            // create schema
            provider.init();
            // open connection
            provider.load();

            UUID id = UUID.randomUUID();
            provider.setBalance(id, "dollar", 42.5);
            double bal = provider.getBalance(id, "dollar");
            assertEquals(42.5, bal, 0.0001);

            provider.shutdown();
        } finally {
            try { mysql.stop(); } catch (Throwable ignored) {}
        }
    }
}
