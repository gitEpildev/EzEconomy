package com.skyblockexp.ezeconomy.storage;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Assume;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Integration test for MongoDBStorageProvider.
 *
 * This test requires a running MongoDB instance. Provide the connection URI
 * via the environment variable `MONGODB_URI` (e.g. "mongodb://localhost:27017").
 * If the variable is not set the test will be skipped.
 */
public class MongoIntegrationTest {

    @Test
    public void integrationDepositWithdraw() {
        String uri = System.getenv("MONGODB_URI");
        Assume.assumeTrue(uri != null && !uri.isEmpty());

        MongoClient client = MongoClients.create(uri);
        MongoDatabase db = client.getDatabase("ezeconomy_integration_test");

        // Ensure clean state
        db.getCollection("balances").drop();
        db.getCollection("banks").drop();

        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("mongodb.database", "ezeconomy_integration_test");
        cfg.set("mongodb.collection", "balances");
        cfg.set("mongodb.banksCollection", "banks");

        MongoDBStorageProvider provider = new MongoDBStorageProvider(null, cfg);
        // package-private setter (same package) to inject the running DB
        provider.setMongoClient(client);

        UUID uuid = UUID.randomUUID();
        String currency = "coins";

        provider.deposit(uuid, currency, 50.0);
        assertEquals(50.0, provider.getBalance(uuid, currency), 0.0001);

        assertTrue(provider.tryWithdraw(uuid, currency, 20.0));
        assertEquals(30.0, provider.getBalance(uuid, currency), 0.0001);

        // cleanup
        db.getCollection("balances").drop();
        client.close();
    }
}
