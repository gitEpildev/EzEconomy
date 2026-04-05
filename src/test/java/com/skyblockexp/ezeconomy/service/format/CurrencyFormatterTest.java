package com.skyblockexp.ezeconomy.service.format;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyFormatterTest {

    @Mock
    EzEconomyPlugin plugin;

    @Test
    void format_withCurrencySymbol_suffixAndPrefix() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.createSection("multi-currency.currencies");
        cfg.set("multi-currency.currencies.dollar.symbol", "$");
        cfg.set("multi-currency.currencies.dollar.decimals", 2);
        cfg.set("multi-currency.currencies.dollar.symbol_placement", "suffix");
        when(plugin.getConfig()).thenReturn(cfg);
        when(plugin.getDefaultCurrency()).thenReturn("dollar");

        CurrencyFormatter f = new CurrencyFormatter(plugin);
        String out = f.format(1234.5, "dollar");
        assertTrue(out.contains("$") || out.endsWith(" $"));

        // prefix placement
        cfg.set("multi-currency.currencies.dollar.symbol_placement", "prefix");
        out = f.format(12.34, "dollar");
        assertTrue(out.startsWith("$") || out.startsWith("$ "));
    }

    @Test
    void formatShort_usesShortFormatting_whenEnabled() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("currency.format.short.enabled", true);
        cfg.set("currency.format.short.threshold", 1000.0);
        cfg.createSection("multi-currency.currencies");
        cfg.set("multi-currency.currencies.dollar.symbol", "$");
        cfg.set("multi-currency.currencies.dollar.decimals", 2);
        when(plugin.getConfig()).thenReturn(cfg);

        CurrencyFormatter f = new CurrencyFormatter(plugin);
        String shortFmt = f.formatShort(1500.0, "dollar");
        assertTrue(shortFmt.contains("k") || shortFmt.contains("m") || shortFmt.contains("b"));
    }
}
