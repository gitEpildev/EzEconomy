package com.skyblockexp.ezeconomy.util;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CurrencyUtilTest {

    private EzEconomyPlugin pluginWithConfig(FileConfiguration cfg) {
        // Ensure MockBukkit server is present so JavaPlugin ctor won't NPE
        org.mockbukkit.mockbukkit.MockBukkit.mock();
        // Load the plugin through MockBukkit so it's created by the mock classloader
        EzEconomyPlugin plugin = org.mockbukkit.mockbukkit.MockBukkit.load(EzEconomyPlugin.class);
        if (cfg != null) {
            // copy entries from the test-provided config into the plugin config
            for (String key : cfg.getKeys(true)) {
                plugin.getConfig().set(key, cfg.get(key));
            }
            plugin.saveConfig();
        }
        return plugin;
    }

    @Test
    public void inverseRateUsesReciprocal() {
        YamlConfiguration cfg = new YamlConfiguration();
        // Define conversion only one-way: usd -> eur = 0.9
        cfg.set("multi-currency.conversion.usd.eur", 0.9);
        // decimals for target currency (usd) when converting to usd
        cfg.set("multi-currency.currencies.usd.decimals", 2);
        cfg.set("multi-currency.currencies.eur.decimals", 2);

        EzEconomyPlugin plugin = pluginWithConfig(cfg);
        try {
            // Convert 90 EUR -> USD, reciprocal should produce 100.00 USD
            double converted = CurrencyUtil.convert(plugin, 90.0, "eur", "usd");
            assertEquals(100.00, converted, 0.001);
        } finally {
            org.mockbukkit.mockbukkit.MockBukkit.unmock();
        }
    }

    @Test
    public void chainedConversionUsesCompositeRate() {
        YamlConfiguration cfg = new YamlConfiguration();
        // a -> b = 2.0, b -> c = 3.0  => a -> c = 6.0
        cfg.set("multi-currency.conversion.a.b", 2.0);
        cfg.set("multi-currency.conversion.b.c", 3.0);
        cfg.set("multi-currency.currencies.c.decimals", 2);

        EzEconomyPlugin plugin = pluginWithConfig(cfg);
        try {
            // Convert 10 A -> C => 10 * 6 = 60.00
            double converted = CurrencyUtil.convert(plugin, 10.0, "a", "c");
            assertEquals(60.00, converted, 0.001);
        } finally {
            org.mockbukkit.mockbukkit.MockBukkit.unmock();
        }
    }
}
