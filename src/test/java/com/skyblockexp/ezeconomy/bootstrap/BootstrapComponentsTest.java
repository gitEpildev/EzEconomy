package com.skyblockexp.ezeconomy.bootstrap;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
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
        System.setProperty("ezeconomy.test", "true");
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
        System.clearProperty("ezeconomy.test");
    }

    @Test
    void testBootstrapLoadsPluginAndRunsComponents() throws Exception {
        EzEconomyPlugin plugin = MockBukkit.load(EzEconomyPlugin.class);
        assertNotNull(plugin, "Plugin should be loaded");

        assertNotNull(plugin.getVaultEconomy(), "Vault economy provider should be registered");
        assertNotNull(plugin.getMetrics(), "Metrics should be initialized");
        assertNotNull(plugin.getPayFlowManager(), "PayFlowManager should be initialized");
    }

}
