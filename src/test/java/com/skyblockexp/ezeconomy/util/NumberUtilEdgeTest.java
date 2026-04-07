package com.skyblockexp.ezeconomy.util;

import com.skyblockexp.ezeconomy.core.Money;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

public class NumberUtilEdgeTest {

    @Test
    void parseUppercaseSuffixesAndSpaces() {
        Money m = NumberUtil.parseMoney("1K", "dollar");
        assertNotNull(m);
        assertEquals(0, m.getAmount().compareTo(new BigDecimal("1000")));

        Money m2 = NumberUtil.parseMoney("  1,000  ", "dollar");
        assertNotNull(m2);
        assertEquals(0, m2.getAmount().compareTo(new BigDecimal("1000")));
    }

    @Test
    void parseGermanFormattedStringBehavior() {
        // With localized parsing enabled, German-style "€1.234,56" should parse as 1234.56
        Money m = NumberUtil.parseMoney("€1.234,56", "euro");
        assertNotNull(m);
        assertEquals(0, m.getAmount().compareTo(new BigDecimal("1234.56")));
    }

    @Test
    void emptyOrWhitespaceReturnsNull() {
        assertNull(NumberUtil.parseMoney("", "dollar"));
        assertNull(NumberUtil.parseMoney("   ", "dollar"));
    }
}
