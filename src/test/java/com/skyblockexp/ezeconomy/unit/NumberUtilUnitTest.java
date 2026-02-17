package com.skyblockexp.ezeconomy.unit;

import com.skyblockexp.ezeconomy.util.NumberUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NumberUtilUnitTest {
    @Test
    public void testParseValidInteger() {
        double v = NumberUtil.parseAmount("5");
        assertEquals(5.0, v, 0.0001);
    }

    @Test
    public void testParseInvalid() {
        double v = NumberUtil.parseAmount("notanumber");
        assertTrue(Double.isNaN(v));
    }

    @Test
    public void testParseDecimal() {
        double v = NumberUtil.parseAmount("12.34");
        assertEquals(12.34, v, 0.0001);
    }

    @org.junit.jupiter.api.Test
    public void testFormatShortValues() {
        assertEquals("500", NumberUtil.formatShort(new java.math.BigDecimal("500")));
        assertEquals("1k", NumberUtil.formatShort(new java.math.BigDecimal("1000")));
        assertEquals("1.5k", NumberUtil.formatShort(new java.math.BigDecimal("1500")));
        assertEquals("2.5m", NumberUtil.formatShort(new java.math.BigDecimal("2500000")));
        assertEquals("1b", NumberUtil.formatShort(new java.math.BigDecimal("1000000000")));
        assertEquals("1.2t", NumberUtil.formatShort(new java.math.BigDecimal("1200000000000")));
    }
}
