package com.skyblockexp.ezeconomy.feature;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.api.storage.StorageProvider;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;
import com.skyblockexp.ezeconomy.feature.support.TestSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PayCommandFeatureTest {
    // avoid declaring ServerMock as a field to prevent classloading issues during test discovery
    private Object server;
    private EzEconomyPlugin plugin;
    private TestSupport.MockStorage storage;

    private static final String CURRENCY = "dollar";

    @BeforeEach
    public void setup() throws Exception {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(EzEconomyPlugin.class);

        // Replace storage with a lightweight in-memory mock using reflection
        storage = new TestSupport.MockStorage();
        TestSupport.injectField(plugin, "storage", storage);

        // Ensure messages are loaded
        plugin.loadMessageProvider();
    }

    @AfterEach
    public void teardown() {
        MockBukkit.unmock();
    }

    @Test
    public void testPayCommand_transfersMoneyBetweenPlayers() {
        try {
            Object senderObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "payer");
            Object recipientObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "payee");
            org.bukkit.entity.Player sender = (org.bukkit.entity.Player) senderObj;
            org.bukkit.entity.Player recipient = (org.bukkit.entity.Player) recipientObj;
            // Give permission
            sender.setOp(true);

            // Initialize balances
            storage.setBalance(sender.getUniqueId(), CURRENCY, 10.0);
            storage.setBalance(recipient.getUniqueId(), CURRENCY, 0.0);

            // Run the command as the sender
            sender.performCommand("pay payee 5");

            // Assert balances updated
            assertEquals(5.0, storage.getBalance(sender.getUniqueId(), CURRENCY), 0.0001);
            assertEquals(5.0, storage.getBalance(recipient.getUniqueId(), CURRENCY), 0.0001);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    // Using centralized TestSupport.MockStorage
}
