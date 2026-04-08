package com.skyblockexp.ezeconomy.papi.placeholders;

import org.junit.jupiter.api.Test;

 
import com.skyblockexp.ezeconomy.papi.testhelpers.TestPlayerFakes;

import static org.junit.jupiter.api.Assertions.*;

public class AdditionalPAPIExpansionTest {

    private org.bukkit.OfflinePlayer offlinePlayer(java.util.UUID id) {
        return TestPlayerFakes.fakeOfflinePlayer(id);
    }

    @Test
    public void playerOverride_delegatesToOffline() throws Exception {
        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion expansion = new com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion(null);

        // Use the existing test helper to avoid Bukkit
        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = com.skyblockexp.ezeconomy.papi.testhelpers.TestEzEconomyHelpers.formatting("dollar", "$");

        java.util.UUID id = java.util.UUID.randomUUID();
        org.bukkit.entity.Player playerProxy = TestPlayerFakes.fakePlayer(id);

        String out = expansion.onPlaceholderRequest(playerProxy, "balance");
        // Should return formatted zero from our test hook when storage is null
        assertEquals("$0.00", out);
    }

    @Test
    public void parseIntOrDefault_and_safe_behaviour_via_reflection() throws Exception {
        Class<?> cls = com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion.class;

        java.lang.reflect.Method parse = cls.getDeclaredMethod("parseIntOrDefault", String.class, int.class);
        parse.setAccessible(true);
        assertEquals(5, parse.invoke(null, "5", 10));
        assertEquals(10, parse.invoke(null, "notanumber", 10));
        assertEquals(7, parse.invoke(null, null, 7));

        java.lang.reflect.Method safe = cls.getDeclaredMethod("safe", String.class);
        safe.setAccessible(true);
        assertEquals("", safe.invoke(null, (Object) null));
        assertEquals("abc", safe.invoke(null, "abc"));
    }
}
