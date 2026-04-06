package com.skyblockexp.ezeconomy.papi;

import com.skyblockexp.ezeconomy.papi.testhelpers.TestEzEconomyStubs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.bukkit.OfflinePlayer;

import static org.junit.jupiter.api.Assertions.*;

public class MockBukkitExpansionBehaviorTest {

    @AfterEach
    public void tearDown() {
        try { MockBukkit.unmock(); } catch (Exception ignored) {}
        EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = null;
    }

    @Test
    public void placeholder_calls_work_when_plugin_enabled_and_placeholder_present() {
        MockBukkit.mock();
        try { MockBukkit.load(PlaceholderStub.class); } catch (Exception ignored) {}

        EzEconomyPapiPlugin parent = (EzEconomyPapiPlugin) MockBukkit.load(EzEconomyPapiPlugin.class);
        assertNotNull(parent);

        // Inject a simple test economy implementation to avoid relying on EzEconomy bootstrap
        TestEzEconomyStubs.SimpleStorageProvider sp = new TestEzEconomyStubs.SimpleStorageProvider();
        java.util.UUID id = java.util.UUID.randomUUID();
        sp.setBalance(id, "dollar", 42.0);
        EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new TestEzEconomyStubs.SimpleTestEz(sp, "dollar");

        EzEconomyPAPIExpansion expansion = new EzEconomyPAPIExpansion(parent);

        OfflinePlayer fake = (OfflinePlayer) java.lang.reflect.Proxy.newProxyInstance(
                OfflinePlayer.class.getClassLoader(),
                new Class[]{OfflinePlayer.class},
                (proxy, method, args) -> {
                    if ("getUniqueId".equals(method.getName())) return id;
                    if (method.getReturnType().equals(boolean.class)) return false;
                    return null;
                }
        );

        String out = expansion.onPlaceholderRequest(fake, "balance");
        assertNotNull(out);
        assertTrue(out.contains("42") || out.contains("42.00"));
    }
}
