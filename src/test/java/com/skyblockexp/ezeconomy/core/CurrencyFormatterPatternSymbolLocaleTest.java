package com.skyblockexp.ezeconomy.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CurrencyFormatterPatternSymbolLocaleTest {

    @Test
    public void usesMoneyFormatPattern() {
        EzEconomyPlugin plugin = new EzEconomyPlugin();
        // Set pattern to always show three decimals
        plugin.getConfig().set("money-format.pattern", "#,##0.000");
        String out = plugin.getCurrencyFormatter().formatAmountOnly(1234.5, null);
        // Expect three decimals
        assertEquals("1,234.500", out);
    }

    @Test
    public void currencySymbolAndPlacement() {
        EzEconomyPlugin plugin = new EzEconomyPlugin();
        plugin.getConfig().set("money-format.currencySymbol", "¤");
        plugin.getConfig().set("money-format.symbolPlacement", "suffix");
        String out = plugin.getCurrencyFormatter().formatAmountOnly(99.99, null);
        assertEquals("99.99 ¤", out);

        plugin.getConfig().set("money-format.symbolPlacement", "prefix");
        out = plugin.getCurrencyFormatter().formatAmountOnly(99.99, null);
        assertEquals("¤ 99.99", out);
    }

    @Test
    public void localeSpecificFormatting() {
        EzEconomyPlugin plugin = new EzEconomyPlugin();
        // Use German locale where decimal separator is comma
        plugin.getConfig().set("money-format.locale", "de_DE");
        String out = plugin.getCurrencyFormatter().formatAmountOnly(1234.56, null);
        // In German locale: 1.234,56
        assertEquals("1.234,56", out);
    }
}
