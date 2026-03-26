package com.skyblockexp.ezeconomy.feature;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.feature.support.TestSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CurrencyIntegerConversionTest {
    private Object server;
    private EzEconomyPlugin plugin;
    private TestSupport.MockStorage storage;

    @BeforeEach
    public void setup() {
        server = MockBukkit.mock();
        plugin = (EzEconomyPlugin) MockBukkit.load(EzEconomyPlugin.class);

        storage = new TestSupport.MockStorage();
        TestSupport.injectField(plugin, "storage", storage);

        plugin.loadMessageProvider();
        plugin.registerCommands();
    }

    @AfterEach
    public void teardown() {
        TestSupport.tearDown();
    }

    @Test
    public void testIntegerRemainder105to100() {
        try {
            Object pObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "intplayer1");
            org.bukkit.entity.Player player = (org.bukkit.entity.Player) pObj;

            plugin.getConfig().set("multi-currency.enabled", true);
            plugin.getConfig().set("multi-currency.currencies.copper_money.decimals", 0);
            plugin.getConfig().set("multi-currency.currencies.silver_money.decimals", 0);
            // 1 silver = 100 copper
            plugin.getConfig().set("multi-currency.conversion.copper_money.silver_money", 0.01);

            storage.setBalance(player.getUniqueId(), "copper_money", 105.0);

            player.performCommand("currency convert copper_money silver_money 105");

            // 100 should convert to 1 silver, 5 should remain
            assertEquals(5.0, storage.getBalance(player.getUniqueId(), "copper_money"), 0.0001);
            assertEquals(1.0, storage.getBalance(player.getUniqueId(), "silver_money"), 0.0001);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testIntegerDivisible200to2() {
        try {
            Object pObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "intplayer2");
            org.bukkit.entity.Player player = (org.bukkit.entity.Player) pObj;

            plugin.getConfig().set("multi-currency.enabled", true);
            plugin.getConfig().set("multi-currency.currencies.copper_money.decimals", 0);
            plugin.getConfig().set("multi-currency.currencies.silver_money.decimals", 0);
            plugin.getConfig().set("multi-currency.conversion.copper_money.silver_money", 0.01);

            storage.setBalance(player.getUniqueId(), "copper_money", 200.0);

            player.performCommand("currency convert copper_money silver_money 200");

            // 200 -> 2 silver, 0 copper
            assertEquals(0.0, storage.getBalance(player.getUniqueId(), "copper_money"), 0.0001);
            assertEquals(2.0, storage.getBalance(player.getUniqueId(), "silver_money"), 0.0001);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void test1to1000Edge500ShouldFailWhenTargetInteger() {
        try {
            Object pObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "intplayer3");
            org.bukkit.entity.Player player = (org.bukkit.entity.Player) pObj;

            plugin.getConfig().set("multi-currency.enabled", true);
            plugin.getConfig().set("multi-currency.currencies.copper_money.decimals", 0);
            plugin.getConfig().set("multi-currency.currencies.imperial_money.decimals", 0);
            // 1 imperial = 1000 copper -> copper.imperial = 0.001
            plugin.getConfig().set("multi-currency.conversion.copper_money.imperial_money", 0.001);

            storage.setBalance(player.getUniqueId(), "copper_money", 500.0);

            // converting 500 copper -> imperial yields 0.5 imperial -> should round to 0 and be rejected
            player.performCommand("currency convert copper_money imperial_money 500");

            // balances unchanged
            assertEquals(500.0, storage.getBalance(player.getUniqueId(), "copper_money"), 0.0001);
            assertEquals(0.0, storage.getBalance(player.getUniqueId(), "imperial_money"), 0.0001);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
