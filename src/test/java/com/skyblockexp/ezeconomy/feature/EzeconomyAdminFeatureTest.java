package com.skyblockexp.ezeconomy.feature;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.feature.support.TestSupport;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class EzeconomyAdminFeatureTest {
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
    public void testAdminReload_runs() throws Exception {
        Object senderObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "adminUser");
        org.bukkit.entity.Player sender = (org.bukkit.entity.Player) senderObj;

        // reload should execute without error
        boolean r = sender.performCommand("ezeconomy reload");
        assertTrue(r);
    }
}

