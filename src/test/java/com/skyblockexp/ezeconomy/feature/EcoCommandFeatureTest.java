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

public class EcoCommandFeatureTest {
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
    public void testEcoGiveSubcommand_runs() throws Exception {
        Object senderObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "ecoAdmin");
        org.bukkit.entity.Player sender = (org.bukkit.entity.Player) senderObj;
        sender.setOp(true);
        // ensure target exists
        Object targetObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "ecoUser");
        org.bukkit.entity.Player target = (org.bukkit.entity.Player) targetObj;

        // use MockStorage to validate a deposit occurred
        MockStorage storage = new MockStorage();
        TestSupport.injectField(plugin, "storage", storage);
        // update VaultEconomyImpl so it uses the injected storage
        TestSupport.injectField(plugin, "vaultEconomy", new com.skyblockexp.ezeconomy.core.VaultEconomyImpl(plugin));

        boolean result = sender.performCommand("eco give ecoUser 10");
        assertTrue(result);

        // Check that the target received funds
        double bal = storage.getBalance(target.getUniqueId(), plugin.getDefaultCurrency());
        assertEquals(10.0, bal, 0.001);
    }

    @Test
    public void testEco_withYMLStorage_depositPersists() throws Exception {
        Object senderObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "ecoYmlAdmin");
        org.bukkit.entity.Player sender = (org.bukkit.entity.Player) senderObj;
        sender.setOp(true);

        org.bukkit.configuration.file.YamlConfiguration cfg = new org.bukkit.configuration.file.YamlConfiguration();
        cfg.set("yml.data-folder", "test-eco-yml");
        com.skyblockexp.ezeconomy.storage.YMLStorageProvider yml = new com.skyblockexp.ezeconomy.storage.YMLStorageProvider(plugin, cfg);
        TestSupport.injectField(plugin, "storage", yml);
        TestSupport.injectField(plugin, "vaultEconomy", new com.skyblockexp.ezeconomy.core.VaultEconomyImpl(plugin));

        // ensure target exists (offline player is ok for YML provider)
        plugin.getServer().getOfflinePlayer("ecoYmlUser");

        boolean r = sender.performCommand("eco give ecoYmlUser 5");
        assertTrue(r);

        double bal = yml.getBalance(plugin.getServer().getOfflinePlayer("ecoYmlUser").getUniqueId(), plugin.getDefaultCurrency());
        org.junit.jupiter.api.Assertions.assertEquals(5.0, bal, 0.001);

        TestSupport.cleanupTestDataFolder(plugin, "test-eco-yml");
    }
}
