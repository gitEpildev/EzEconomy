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
}
