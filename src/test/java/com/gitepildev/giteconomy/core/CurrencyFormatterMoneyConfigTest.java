package com.gitepildev.giteconomy.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CurrencyFormatterMoneyConfigTest {

    @Test
    public void moneyFormat_useCompact_enabled_and_precision() {
        GitEconomyPlugin plugin = new GitEconomyPlugin();
        // configure legacy money-format keys
        plugin.getConfig().set("money-format.useCompact", true);
        plugin.getConfig().set("money-format.compact.precision", 2);

        // 1500 should be compacted with 2 decimals -> 1.50K
        assertEquals("1.50K", plugin.getCurrencyFormatter().formatShort(1500.0, null));
    }

    @Test
    public void moneyFormat_threshold_respected() {
        GitEconomyPlugin plugin = new GitEconomyPlugin();
        // enable compact but raise threshold to 2000
        plugin.getConfig().set("money-format.useCompact", true);
        plugin.getConfig().set("money-format.compact.thresholds.thousand", 2000);

        // 1500 is below threshold -> full format (defaults to 2 decimals)
        String full = plugin.getCurrencyFormatter().format(1500.0, null);
        assertEquals(full, plugin.getCurrencyFormatter().formatShort(1500.0, null));
    }

    @Test
    public void moneyFormat_disable_compact() {
        GitEconomyPlugin plugin = new GitEconomyPlugin();
        plugin.getConfig().set("money-format.useCompact", false);
        // even for large values, short format should fall back to full
        String full = plugin.getCurrencyFormatter().format(2500.0, null);
        assertEquals(full, plugin.getCurrencyFormatter().formatShort(2500.0, null));
    }
}
