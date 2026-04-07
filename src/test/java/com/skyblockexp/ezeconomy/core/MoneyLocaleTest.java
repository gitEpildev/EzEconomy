package com.skyblockexp.ezeconomy.core;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

public class MoneyLocaleTest {

    @Test
    void germanLocaleUsesCommaDecimal() {
        Money m = Money.of(new BigDecimal("1234.56"), "euro");
        String out = m.toDisplayString(Locale.GERMANY, 2, "€", false);
        // Should use comma as decimal separator in German locale
        assertTrue(out.contains(","), "Expected decimal comma in German formatted output");
        assertTrue(out.contains("€"), "Expected currency symbol to be present");
    }

    @Test
    void usLocaleGroupingAndDecimals() {
        Money m = Money.of(new BigDecimal("1234567.89"), "dollar");
        String out = m.toDisplayString(Locale.US, 2, "$", true);
        assertTrue(out.startsWith("$"), "Expected prefix symbol for US with symbolPrefix=true");
        assertTrue(out.contains(","), "Expected grouping comma in US formatted output");
        assertTrue(out.contains(".89"));
    }
}
