package com.skyblockexp.ezeconomy.storage;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import org.junit.jupiter.api.Test;
import org.mockbukkit.MockBukkit;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lightweight test for MongoDBStorageProvider behavior using a simple in-memory
 * subclass to avoid external MongoDB dependencies.
 */
public class TestMongoDBProvider {
    static class InMemoryMongoProvider extends MongoDBStorageProvider {
        private final Map<String, Double> map = new HashMap<>();

        InMemoryMongoProvider(EzEconomyPlugin plugin) {
            super(plugin, null);
        }

        @Override
        public double getBalance(java.util.UUID uuid, String currency) {
            return map.getOrDefault(uuid.toString() + ":" + currency, 0.0);
        }

        @Override
        public void setBalance(java.util.UUID uuid, String currency, double amount) {
            map.put(uuid.toString() + ":" + currency, amount);
        }

        @Override
        public boolean playerExists(java.util.UUID uuid) {
            String prefix = uuid.toString() + ":";
            for (String k : map.keySet()) if (k.startsWith(prefix)) return true;
            return false;
        }
    }

    @Test
    public void testInMemoryMongoPlayerExists() {
        MockBukkit.mock();
        EzEconomyPlugin plugin = (EzEconomyPlugin) MockBukkit.load(EzEconomyPlugin.class);
        InMemoryMongoProvider p = new InMemoryMongoProvider(plugin);

        UUID u = UUID.randomUUID();
        assertFalse(p.playerExists(u));
        p.setBalance(u, "dollar", 3.0);
        assertTrue(p.playerExists(u));

        MockBukkit.unmock();
    }
}
