package com.skyblockexp.ezeconomy.feature;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.api.storage.StorageProvider;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.skyblockexp.ezeconomy.feature.support.TestSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MongoPayFeatureTest {
    private Object server;
    private EzEconomyPlugin plugin;

    @BeforeEach
    public void setup() throws Exception {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(EzEconomyPlugin.class);

        // Force test mode to avoid real network init
        System.setProperty("ezeconomy.test", "true");

        // Inject a lightweight in-memory StorageProvider for tests
        TestSupport.MockStorage provider = new TestSupport.MockStorage();
        TestSupport.injectField(plugin, "storage", provider);

        plugin.loadMessageProvider();
    }

    @AfterEach
    public void teardown() {
        System.clearProperty("ezeconomy.test");
        MockBukkit.unmock();
    }

    @Test
    public void testPayCommand_offlineRecipient_withMongoStorage() throws Exception {
        // create offline recipient
        Object offlineObj = null;
        try {
            java.lang.reflect.Method addOffline = server.getClass().getMethod("addOfflinePlayer", String.class);
            offlineObj = addOffline.invoke(server, "mongoOffline");
        } catch (NoSuchMethodException ignored) {
            offlineObj = org.bukkit.Bukkit.getOfflinePlayer("mongoOffline");
        }
        org.bukkit.OfflinePlayer offline = (org.bukkit.OfflinePlayer) offlineObj;

        // create sender
        Object senderObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "mongoSender");
        org.bukkit.entity.Player sender = (org.bukkit.entity.Player) senderObj;
        sender.setOp(true);

        // get provider and initialize balances
        TestSupport.MockStorage storage = (TestSupport.MockStorage) TestSupport.getField(plugin, "storage");

        storage.setBalance(sender.getUniqueId(), "dollar", 10.0);
        storage.setBalance(offline.getUniqueId(), "dollar", 0.0);

        // perform command which will invoke storage transfer path
        sender.performCommand("pay mongoOffline 5");

        // wait briefly for any async actions
        Thread.sleep(300);

        assertEquals(5.0, storage.getBalance(sender.getUniqueId(), "dollar"), 0.0001);
        assertEquals(5.0, storage.getBalance(offline.getUniqueId(), "dollar"), 0.0001);
    }

    // Using TestSupport.MockStorage for in-memory test storage
}
