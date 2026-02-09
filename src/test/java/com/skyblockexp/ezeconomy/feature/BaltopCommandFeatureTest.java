package com.skyblockexp.ezeconomy.feature;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.feature.support.TestSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BaltopCommandFeatureTest {
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
    public void testBaltopCommand_showsTopPlayers() throws Exception {
        // create two players and give them balances
        Object p1Obj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "topOne");
        org.bukkit.entity.Player p1 = (org.bukkit.entity.Player) p1Obj;
        Object p2Obj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "topTwo");
        org.bukkit.entity.Player p2 = (org.bukkit.entity.Player) p2Obj;

        TestSupport.MockStorage storage = new TestSupport.MockStorage();
        TestSupport.injectField(plugin, "storage", storage);
        TestSupport.injectField(plugin, "vaultEconomy", new com.skyblockexp.ezeconomy.core.VaultEconomyImpl(plugin));

        storage.setBalance(p1.getUniqueId(), plugin.getDefaultCurrency(), 1000.0);
        storage.setBalance(p2.getUniqueId(), plugin.getDefaultCurrency(), 500.0);

        // perform baltop command as p1
        boolean ok = p1.performCommand("baltop");
        assertTrue(ok);

        // validate storage ordering: p1 should be top
        java.util.Map<java.util.UUID, Double> all = storage.getAllBalances(plugin.getDefaultCurrency());
        java.util.UUID topUuid = all.entrySet().stream().max(java.util.Comparator.comparingDouble(java.util.Map.Entry::getValue)).get().getKey();
        assertEquals(p1.getUniqueId(), topUuid);
    }
}
