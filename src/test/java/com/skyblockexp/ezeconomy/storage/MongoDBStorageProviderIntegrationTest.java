package com.skyblockexp.ezeconomy.storage;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class MongoDBStorageProviderIntegrationTest {

    @Test
    void testSetAndGetBalance_withTestcontainersMongo() throws Exception {
        try {
            Class.forName("org.testcontainers.containers.MongoDBContainer");
        } catch (ClassNotFoundException e) {
            Assumptions.assumeTrue(false, "Testcontainers not on classpath; skipping integration test");
            return;
        }

        org.testcontainers.containers.MongoDBContainer mongo = new org.testcontainers.containers.MongoDBContainer("mongo:6.0.6");
        try {
            mongo.start();
        } catch (Throwable t) {
            Assumptions.assumeTrue(false, "Docker/Testcontainers not available: " + t.getMessage());
            return;
        }

        try {
            EzEconomyPlugin plugin = mock(EzEconomyPlugin.class);
            YamlConfiguration cfg = new YamlConfiguration();
            cfg.set("mongodb.database", "testdb");

            MongoDBStorageProvider provider = new MongoDBStorageProvider(plugin, cfg);

            // Inject a MongoClient from the running container to avoid relying on network defaults
            com.mongodb.client.MongoClient client = com.mongodb.client.MongoClients.create(mongo.getReplicaSetUrl());
            provider.setMongoClient(client);

            UUID id = UUID.randomUUID();
            provider.setBalance(id, "dollar", 99.99);
            double bal = provider.getBalance(id, "dollar");
            assertEquals(99.99, bal, 0.0001);

            provider.shutdown();
            try { client.close(); } catch (Exception ignored) {}
        } finally {
            try { mongo.stop(); } catch (Throwable ignored) {}
        }
    }
}
