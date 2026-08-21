package com.gitepildev.giteconomy.feature;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.gitepildev.giteconomy.core.GitEconomyPlugin;
import com.gitepildev.giteconomy.feature.support.TestSupport;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class GiteconomyAdminFeatureTest {
    private Object server;
    private GitEconomyPlugin plugin;

    @BeforeEach
    public void setup() throws Exception {
        System.setProperty("giteconomy.test", "true");
        server = TestSupport.setupMockServer();
        plugin = TestSupport.loadPlugin(server);
        plugin.loadMessageProvider();
    }

    @AfterEach
    public void teardown() {
        TestSupport.tearDown();
        System.clearProperty("giteconomy.test");
    }

    @Test
    public void testAdminReload_runs() throws Exception {
        Object senderObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "adminUser");
        org.bukkit.entity.Player sender = (org.bukkit.entity.Player) senderObj;

        // reload should execute without error
        boolean r = sender.performCommand("giteconomy reload");
        assertTrue(r);
    }
}

