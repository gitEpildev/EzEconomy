package com.skyblockexp.ezeconomy.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EzEconomyPluginFormatShortTest {

    @Test
    public void formatShort_withoutCurrency_smallValues() {
        EzEconomyPlugin plugin = new EzEconomyPlugin();
        // When currency is null, formatShort returns NumberUtil's short output
        assertEquals("1.5k", plugin.formatShort(1500.0, null));
        assertEquals("1k", plugin.formatShort(1000.0, null));
        assertEquals("999", plugin.formatShort(999.0, null));
        assertEquals("2.5m", plugin.formatShort(2_500_000.0, null));
    }

    @Test
    public void formatShort_respectsDecimalsConfig() {
        EzEconomyPlugin plugin = new EzEconomyPlugin();
        // override config to show 2 decimals
        plugin.getConfig().set("currency.format.short.decimals", 2);
        assertEquals("1.50k", plugin.formatShort(1500.0, null));
        // large value
        assertEquals("2.50m", plugin.formatShort(2_500_000.0, null));
    }

    @Test
    public void formatShort_perCurrencyDecimals() {
        EzEconomyPlugin plugin = new EzEconomyPlugin();
        // set global to 1 but per-currency to 2
        plugin.getConfig().set("currency.format.short.decimals", 1);
        plugin.getConfig().set("multi-currency.currencies.test.short.decimals", 2);
        // when specifying currency key, per-currency decimals should be used
        assertEquals("1.50k", plugin.formatShort(1500.0, "test"));
        // other currency falls back to global
        assertEquals("1.5k", plugin.formatShort(1500.0, "other"));
    }

    @Test
    public void formatShort_perCurrencyEnabledAndThreshold() {
        EzEconomyPlugin plugin = new EzEconomyPlugin();
        // global enabled, threshold 1000
        plugin.getConfig().set("currency.format.short.enabled", true);
        plugin.getConfig().set("currency.format.short.threshold", 1000);

        // per-currency: disabled
        plugin.getConfig().set("multi-currency.currencies.disabled.short.enabled", false);
        assertEquals(plugin.format(1500.0, "disabled"), plugin.formatShort(1500.0, "disabled"));

        // per-currency: higher threshold
        plugin.getConfig().set("multi-currency.currencies.highthresh.short.threshold", 2000);
        // 1500 below per-currency threshold -> fallback to full format
        assertEquals(plugin.format(1500.0, "highthresh"), plugin.formatShort(1500.0, "highthresh"));
        // 2500 above threshold -> uses short
        assertEquals("2.5k", plugin.formatShort(2500.0, "highthresh"));
    }
}
