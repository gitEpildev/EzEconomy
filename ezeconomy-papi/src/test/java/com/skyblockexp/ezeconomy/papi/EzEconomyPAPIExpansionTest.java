package com.skyblockexp.ezeconomy.papi;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;
import com.skyblockexp.ezeconomy.cache.ExpiringCache;

import static org.junit.jupiter.api.Assertions.*;

public class EzEconomyPAPIExpansionTest {

    @Test
    public void testParseIntOrDefault() throws Exception {
        Method m = EzEconomyPAPIExpansion.class.getDeclaredMethod("parseIntOrDefault", String.class, int.class);
        m.setAccessible(true);
        int v1 = (int) m.invoke(null, "42", 10);
        assertEquals(42, v1);
        int v2 = (int) m.invoke(null, "notanumber", 7);
        assertEquals(7, v2);
        int v3 = (int) m.invoke(null, "", 5);
        assertEquals(5, v3);
    }

    @Test
    public void testTopCachePutAndExpire() throws Exception {
        // construct expansion (plugin param may be null for this test)
        EzEconomyPAPIExpansion expansion = new EzEconomyPAPIExpansion(null);

        // access private topCache field
        java.lang.reflect.Field cacheField = EzEconomyPAPIExpansion.class.getDeclaredField("topCache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        com.skyblockexp.ezeconomy.cache.CacheProvider<String, String> topCache = (com.skyblockexp.ezeconomy.cache.CacheProvider<String, String>) cacheField.get(expansion);
        assertNotNull(topCache);

        // Put a fresh entry and verify it's present
        topCache.put("key1", "value1", 1000);
        com.skyblockexp.ezeconomy.cache.ExpiringCache.Entry<String> e = topCache.getEntry("key1");
        assertNotNull(e);
        assertEquals("value1", e.value);

        // Put an entry with tiny TTL and ensure it expires
        topCache.put("key-expired", "old", 1);
        Thread.sleep(10);
        com.skyblockexp.ezeconomy.cache.ExpiringCache.Entry<String> expired = topCache.getEntry("key-expired");
        assertNotNull(expired);
        assertTrue(expired.expiresAt < System.currentTimeMillis());
    }
}
