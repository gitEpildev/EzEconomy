package com.gitepildev.giteconomy.bootstrap;

import com.gitepildev.giteconomy.core.GitEconomyPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration-style test that loads the real plugin via MockBukkit (if available)
 * and asserts the bootstrap completed.
 */
public class BootstrapComponentsTest {

    private Object server;

    @BeforeEach
    void setup() throws Exception {
        System.setProperty("giteconomy.test", "true");
        try {
            server = MockBukkit.mock();
        } catch (NoClassDefFoundError e) {
            Assumptions.assumeTrue(false, "MockBukkit not available; skipping MockBukkit-based tests");
        }
    }

    @AfterEach
    void teardown() throws Exception {
        try {
            MockBukkit.unmock();
        } catch (NoClassDefFoundError e) {
            // ignore
        }
        System.clearProperty("giteconomy.test");
    }

    @Test
    void testBootstrapLoadsPluginAndRunsComponents() throws Exception {
        GitEconomyPlugin plugin = MockBukkit.load(GitEconomyPlugin.class);
        assertNotNull(plugin, "Plugin should be loaded");

        assertNotNull(plugin.getVaultEconomy(), "Vault economy provider should be registered");
        assertNotNull(plugin.getMetrics(), "Metrics should be initialized");
        assertNotNull(plugin.getCurrencyManager(), "CurrencyManager should be initialized");
    }

}
