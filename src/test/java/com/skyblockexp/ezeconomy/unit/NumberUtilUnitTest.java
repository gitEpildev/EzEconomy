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
}
