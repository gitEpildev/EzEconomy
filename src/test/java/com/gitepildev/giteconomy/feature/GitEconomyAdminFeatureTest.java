package com.gitepildev.giteconomy.feature;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import com.gitepildev.giteconomy.core.GitEconomyPlugin;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class GitEconomyAdminFeatureTest {
    private Object server;
    private GitEconomyPlugin plugin;

    @BeforeEach
    public void setup() throws Exception {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(GitEconomyPlugin.class);
        plugin.loadMessageProvider();
    }

    @AfterEach
    public void teardown() {
        MockBukkit.unmock();
    }

    @Test
    public void testReloadSubcommand_runs() throws Exception {
        Object senderObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "adminUser");
        org.bukkit.entity.Player sender = (org.bukkit.entity.Player) senderObj;
        sender.setOp(true);
        boolean result = sender.performCommand("giteconomy reload");
        assertTrue(result);
    }
}
