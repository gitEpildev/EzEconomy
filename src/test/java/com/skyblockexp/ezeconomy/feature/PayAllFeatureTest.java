package com.skyblockexp.ezeconomy.feature;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.feature.support.TestSupport;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PayAllFeatureTest {
    private Object server;
    private EzEconomyPlugin plugin;
    private TestSupport.MockStorage storage;

    private static final String CURRENCY = "dollar";

    @BeforeEach
    public void setup() throws Exception {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(EzEconomyPlugin.class);
        storage = new TestSupport.MockStorage();
        TestSupport.injectField(plugin, "storage", storage);
        plugin.loadMessageProvider();
    }

    @AfterEach
    public void teardown() {
        MockBukkit.unmock();
    }

    @Test
    public void testPayAll_withdrawsAndDeposits() {
        try {
            Object s1 = server.getClass().getMethod("addPlayer", String.class).invoke(server, "payer");
            Object r1 = server.getClass().getMethod("addPlayer", String.class).invoke(server, "alice");
            Object r2 = server.getClass().getMethod("addPlayer", String.class).invoke(server, "bob");
            org.bukkit.entity.Player payer = (org.bukkit.entity.Player) s1;
            org.bukkit.entity.Player alice = (org.bukkit.entity.Player) r1;
            org.bukkit.entity.Player bob = (org.bukkit.entity.Player) r2;

            // Give necessary permission to run /pay
            payer.setOp(true);

            // Initialize balances: payer enough for 2 recipients
            storage.setBalance(payer.getUniqueId(), CURRENCY, 20.0);
            storage.setBalance(alice.getUniqueId(), CURRENCY, 0.0);
            storage.setBalance(bob.getUniqueId(), CURRENCY, 0.0);

            // Run the command
            payer.performCommand("pay * 10");

            // payer should have 0, recipients 10 each
            assertEquals(0.0, storage.getBalance(payer.getUniqueId(), CURRENCY), 0.0001);
            assertEquals(10.0, storage.getBalance(alice.getUniqueId(), CURRENCY), 0.0001);
            assertEquals(10.0, storage.getBalance(bob.getUniqueId(), CURRENCY), 0.0001);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testPayAll_bypassWithdraw_keepsPayerBalance() {
        try {
            Object s1 = server.getClass().getMethod("addPlayer", String.class).invoke(server, "payer2");
            Object r1 = server.getClass().getMethod("addPlayer", String.class).invoke(server, "charlie");
            org.bukkit.entity.Player payer = (org.bukkit.entity.Player) s1;
            org.bukkit.entity.Player charlie = (org.bukkit.entity.Player) r1;

            // Do not give withdraw permission but give payall bypass
            payer.setOp(false);
            payer.addAttachment(plugin, "ezeconomy.pay", true);
            payer.addAttachment(plugin, "ezeconomy.payall.bypasswithdraw", true);

            // Payer balance zero
            storage.setBalance(payer.getUniqueId(), CURRENCY, 0.0);
            storage.setBalance(charlie.getUniqueId(), CURRENCY, 0.0);

            payer.performCommand("pay * 5");

            // payer unchanged, recipient received
            assertEquals(0.0, storage.getBalance(payer.getUniqueId(), CURRENCY), 0.0001);
            assertEquals(5.0, storage.getBalance(charlie.getUniqueId(), CURRENCY), 0.0001);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testPayAll_includeOffline_paysStoredOfflinePlayers() {
        try {
            // enable include_offline in config
            plugin.getConfig().set("pay.pay_all.include_offline", true);

            Object s1 = server.getClass().getMethod("addPlayer", String.class).invoke(server, "payer_off");
            org.bukkit.entity.Player payer = (org.bukkit.entity.Player) s1;
            payer.setOp(true);

            // create an offline UUID (not added to server)
            java.util.UUID offlineUuid = java.util.UUID.randomUUID();
            // ensure storage has an entry so getAllBalances will include it
            storage.setBalance(offlineUuid, CURRENCY, 0.0);

            // payer has enough for one recipient
            storage.setBalance(payer.getUniqueId(), CURRENCY, 5.0);

            payer.performCommand("pay * 5");

            // offline recipient should have received 5
            assertEquals(5.0, storage.getBalance(offlineUuid, CURRENCY), 0.0001);
            // payer should have 0
            assertEquals(0.0, storage.getBalance(payer.getUniqueId(), CURRENCY), 0.0001);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testPayAll_requiresPayallPermission() {
        try {
            Object s1 = server.getClass().getMethod("addPlayer", String.class).invoke(server, "payer3");
            Object r1 = server.getClass().getMethod("addPlayer", String.class).invoke(server, "dave");
            org.bukkit.entity.Player payer = (org.bukkit.entity.Player) s1;
            org.bukkit.entity.Player dave = (org.bukkit.entity.Player) r1;

            // Give normal pay permission but not payall
            payer.setOp(false);
            payer.addAttachment(plugin, "ezeconomy.pay", true);

            storage.setBalance(payer.getUniqueId(), CURRENCY, 10.0);
            storage.setBalance(dave.getUniqueId(), CURRENCY, 0.0);

            payer.performCommand("pay * 5");

            // No transfer should occur because payall permission missing
            assertEquals(10.0, storage.getBalance(payer.getUniqueId(), CURRENCY), 0.0001);
            assertEquals(0.0, storage.getBalance(dave.getUniqueId(), CURRENCY), 0.0001);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
