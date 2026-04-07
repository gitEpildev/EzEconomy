package com.skyblockexp.ezeconomy.feature;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.feature.support.TestSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CurrencyConvertFeatureTest {
    private Object server;
    private EzEconomyPlugin plugin;
    private TestSupport.MockStorage storage;

    @BeforeEach
    public void setup() {
        server = MockBukkit.mock();
        plugin = (EzEconomyPlugin) MockBukkit.load(EzEconomyPlugin.class);

        storage = new TestSupport.MockStorage();
        TestSupport.injectField(plugin, "storage", storage);

        // Ensure messages and commands are initialized
        plugin.loadMessageProvider();
        plugin.registerCommands();
    }

    @AfterEach
    public void teardown() {
        TestSupport.tearDown();
    }

    @Test
    public void testCurrencyConvert_checksBalanceAndPerformsConversion() {
        try {
            Object pObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "convplayer");
            org.bukkit.entity.Player player = (org.bukkit.entity.Player) pObj;

            // Enable multi-currency and configure currencies + conversion
            plugin.getConfig().set("multi-currency.enabled", true);
            plugin.getConfig().set("multi-currency.currencies.usd.symbol", "$");
            plugin.getConfig().set("multi-currency.currencies.usd.decimals", 2);
            plugin.getConfig().set("multi-currency.currencies.eur.symbol", "€");
            plugin.getConfig().set("multi-currency.currencies.eur.decimals", 2);
            // conversion: 1 USD = 0.9 EUR
            plugin.getConfig().set("multi-currency.conversion.usd.eur", 0.9);

            // Give the player some USD
            storage.setBalance(player.getUniqueId(), "usd", 10.0);

            // Run the convert command: convert 5 USD -> EUR
            player.performCommand("currency convert usd eur 5");

            // USD should be debited by 5
            assertEquals(5.0, storage.getBalance(player.getUniqueId(), "usd"), 0.0001);
            // EUR should be credited with 5 * 0.9 = 4.5
            assertEquals(4.5, storage.getBalance(player.getUniqueId(), "eur"), 0.0001);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testCurrencyConvert_failsWhenInsufficientFunds() {
        try {
            Object pObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "convplayer2");
            org.bukkit.entity.Player player = (org.bukkit.entity.Player) pObj;

            // Enable multi-currency and configure currencies + conversion
            plugin.getConfig().set("multi-currency.enabled", true);
            plugin.getConfig().set("multi-currency.currencies.usd.symbol", "$");
            plugin.getConfig().set("multi-currency.currencies.usd.decimals", 2);
            plugin.getConfig().set("multi-currency.currencies.eur.symbol", "€");
            plugin.getConfig().set("multi-currency.currencies.eur.decimals", 2);
            // conversion: 1 USD = 0.9 EUR
            plugin.getConfig().set("multi-currency.conversion.usd.eur", 0.9);

            // Give the player insufficient USD (only 2)
            storage.setBalance(player.getUniqueId(), "usd", 2.0);

            // Attempt to convert 5 USD -> EUR (should fail)
            player.performCommand("currency convert usd eur 5");

            // USD balance should be unchanged
            assertEquals(2.0, storage.getBalance(player.getUniqueId(), "usd"), 0.0001);
            // EUR should remain zero
            assertEquals(0.0, storage.getBalance(player.getUniqueId(), "eur"), 0.0001);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testCurrencyConvert_failsWhenRoundedToZero() {
        try {
            Object pObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "convplayer3");
            org.bukkit.entity.Player player = (org.bukkit.entity.Player) pObj;

            // Enable multi-currency and configure currencies + conversion
            plugin.getConfig().set("multi-currency.enabled", true);
            plugin.getConfig().set("multi-currency.currencies.dollars.symbol", "$D");
            plugin.getConfig().set("multi-currency.currencies.dollars.decimals", 2);
            plugin.getConfig().set("multi-currency.currencies.gems.symbol", "G");
            // gems decimals = 0 (will cause small conversions to round to 0)
            plugin.getConfig().set("multi-currency.currencies.gems.decimals", 0);
            // conversion: 1 dollar = 0.01 gems
            plugin.getConfig().set("multi-currency.conversion.dollars.gems", 0.01);

            // Give the player some dollars
            storage.setBalance(player.getUniqueId(), "dollars", 3.0);

            // Attempt to convert 3 dollars -> gems (would be 0.03 -> rounds to 0)
            player.performCommand("currency convert dollars gems 3");

            // Dollars should be unchanged because conversion was rejected
            assertEquals(3.0, storage.getBalance(player.getUniqueId(), "dollars"), 0.0001);
            // Gems should remain zero
            assertEquals(0.0, storage.getBalance(player.getUniqueId(), "gems"), 0.0001);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testCurrencyConvert_succeedsWhenTargetHasDecimals() {
        try {
            Object pObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "convplayer4");
            org.bukkit.entity.Player player = (org.bukkit.entity.Player) pObj;

            // Enable multi-currency and configure currencies + conversion
            plugin.getConfig().set("multi-currency.enabled", true);
            plugin.getConfig().set("multi-currency.currencies.dollars.symbol", "$D");
            plugin.getConfig().set("multi-currency.currencies.dollars.decimals", 2);
            plugin.getConfig().set("multi-currency.currencies.gems.symbol", "G");
            // gems decimals = 2 (so 0.03 should be preserved)
            plugin.getConfig().set("multi-currency.currencies.gems.decimals", 2);
            // conversion: 1 dollar = 0.01 gems
            plugin.getConfig().set("multi-currency.conversion.dollars.gems", 0.01);

            // Give the player some dollars
            storage.setBalance(player.getUniqueId(), "dollars", 3.0);

            // Convert 3 dollars -> gems (should yield 0.03)
            player.performCommand("currency convert dollars gems 3");

            // Dollars should be debited by 3
            assertEquals(0.0, storage.getBalance(player.getUniqueId(), "dollars"), 0.0001);
            // Gems should be credited with 0.03
            assertEquals(0.03, storage.getBalance(player.getUniqueId(), "gems"), 0.0001);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
