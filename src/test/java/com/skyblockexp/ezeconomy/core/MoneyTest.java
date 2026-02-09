package com.skyblockexp.ezeconomy.core;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

public class MoneyTest {

    @Test
    void displayStringUsesLocaleAndSymbolPrefix() {
        Money m = Money.of(new BigDecimal("1234.567"), "dollar");
        String out = m.toDisplayString(Locale.US, 2, "$", true);
        assertEquals("$ 1,234.57", out);
    }

    @Test
    void displayStringSuffixPlacement() {
        Money m = Money.of(new BigDecimal("1000000"), "euro");
        String out = m.toDisplayString(Locale.GERMANY, 0, "€", false);
        // Germany uses dot for grouping; with 0 decimals we expect grouping separator
        assertTrue(out.contains("1.000.000") || out.contains("1 000 000"));
        assertTrue(out.endsWith(" €") || out.endsWith("€"));
    }
}
