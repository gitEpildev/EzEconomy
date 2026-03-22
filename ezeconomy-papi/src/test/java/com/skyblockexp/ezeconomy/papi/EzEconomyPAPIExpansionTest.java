package com.skyblockexp.ezeconomy.papi;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

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
        Field cacheField = EzEconomyPAPIExpansion.class.getDeclaredField("topCache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> topCache = (Map<String, Object>) cacheField.get(expansion);
        assertNotNull(topCache);

        // create a CacheEntry via reflection (private static inner class)
        Class<?>[] inner = EzEconomyPAPIExpansion.class.getDeclaredClasses();
        Class<?> cacheEntryClass = null;
        for (Class<?> c : inner) {
            if (c.getSimpleName().equals("CacheEntry")) {
                cacheEntryClass = c;
                break;
            }
        }
        assertNotNull(cacheEntryClass, "CacheEntry class must exist");

        Constructor<?> ctor = cacheEntryClass.getDeclaredConstructor(String.class, long.class);
        ctor.setAccessible(true);
        Object entry = ctor.newInstance("value1", System.currentTimeMillis() + 1000);

        topCache.put("key1", entry);
        assertTrue(topCache.containsKey("key1"));

        // Put an expired entry and verify timestamp is in the past
        Object expired = ctor.newInstance("old", System.currentTimeMillis() - 1000);
        topCache.put("key-expired", expired);

        Field expiresAtField = cacheEntryClass.getDeclaredField("expiresAt");
        expiresAtField.setAccessible(true);
        long expires = (long) expiresAtField.get(expired);
        assertTrue(expires < System.currentTimeMillis());
    }
}
