package com.skyblockexp.ezeconomy.util;

import com.skyblockexp.ezeconomy.core.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class NumberUtilTest {

    @Test
    void parseMoney_withSuffixesAndSymbols() {
        Money m1 = NumberUtil.parseMoney("1k", "dollar");
        assertNotNull(m1);
        assertEquals(new BigDecimal("1000"), m1.getAmount().stripTrailingZeros());

        Money m2 = NumberUtil.parseMoney("$1,234.56", "dollar");
        assertNotNull(m2);
        assertEquals(0, new BigDecimal("1234.56").compareTo(m2.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP)));

        // German style
        Money m3 = NumberUtil.parseMoney("1.234,56", "dollar");
        assertNotNull(m3);
        assertEquals(0, new BigDecimal("1234.56").compareTo(m3.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP)));
    }

    @Test
    void formatShort_variousMagnitudes() {
        assertEquals("999", NumberUtil.formatShort(new BigDecimal("999")));
        assertEquals("1k", NumberUtil.formatShort(new BigDecimal("1000")));
        assertEquals("1.5k", NumberUtil.formatShort(new BigDecimal("1500")));
        assertEquals("2.5m", NumberUtil.formatShort(new BigDecimal("2500000")));
    }

    @Test
    void parseAmount_failureReturnsNaN() {
        double v = NumberUtil.parseAmount("not a number");
        assertTrue(Double.isNaN(v));
    }
}
package com.skyblockexp.ezeconomy.util;

import com.skyblockexp.ezeconomy.core.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class NumberUtilTest {

    @Test
    void parseSimpleSuffixes() {
        Money m1 = NumberUtil.parseMoney("1k", "dollar");
        assertNotNull(m1);
        assertEquals(0, m1.getAmount().compareTo(new BigDecimal("1000")));

        Money m2 = NumberUtil.parseMoney("2.5m", "dollar");
        assertNotNull(m2);
        assertEquals(0, m2.getAmount().compareTo(new BigDecimal("2500000")));

        Money m3 = NumberUtil.parseMoney("3b", "dollar");
        assertNotNull(m3);
        assertEquals(0, m3.getAmount().compareTo(new BigDecimal("3000000000")));
    }

    @Test
    void parseWithSymbolsAndCommas() {
        Money m = NumberUtil.parseMoney("$1,234.56", "dollar");
        assertNotNull(m);
        assertEquals(new BigDecimal("1234.56"), m.getAmount().setScale(2));
    }

    @Test
    void parseInvalidReturnsNull() {
        Money m = NumberUtil.parseMoney("notanumber", "dollar");
        assertNull(m);
    }
}
