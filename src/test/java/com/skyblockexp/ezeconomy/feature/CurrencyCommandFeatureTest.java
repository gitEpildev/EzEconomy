package com.skyblockexp.ezeconomy.feature;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.feature.support.TestSupport;
import com.skyblockexp.ezeconomy.feature.support.TestSupport.MockStorage;
import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CurrencyCommandFeatureTest {
    private Object server;
    private EzEconomyPlugin plugin;

    @BeforeEach
    public void setup() throws Exception {
        System.setProperty("ezeconomy.test", "true");
        server = TestSupport.setupMockServer();
        plugin = TestSupport.loadPlugin(server);
        plugin.loadMessageProvider();
    }

    @AfterEach
    public void teardown() {
        TestSupport.tearDown();
        System.clearProperty("ezeconomy.test");
    }

    @Test
    public void testCurrencyCommand_runs() throws Exception {
        Object senderObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "curPlayer");
        org.bukkit.entity.Player sender = (org.bukkit.entity.Player) senderObj;
        MockStorage storage = new MockStorage();
        TestSupport.injectField(plugin, "storage", storage);

        boolean result = sender.performCommand("currency");
        assertTrue(result);

        // basic sanity: default currency exists and can be formatted
        double curBal = storage.getBalance(sender.getUniqueId(), plugin.getDefaultCurrency());
        assertEquals(0.0, curBal, 0.001);
    }
}
