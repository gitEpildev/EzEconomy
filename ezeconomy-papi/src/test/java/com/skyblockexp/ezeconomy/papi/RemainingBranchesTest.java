package com.skyblockexp.ezeconomy.papi;

import com.skyblockexp.ezeconomy.papi.testhelpers.TestEzEconomyStubs;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class RemainingBranchesTest {

    @AfterEach
    public void tearDown() {
        EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = null;
    }

    @Test
    public void balance_formatted_with_null_storage_returns_zero_formatted() {
        // TestEz with null storage should return formatted zero for balance_formatted
        EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new TestEzEconomyStubs.SimpleTestEz(null, "dollar");

        EzEconomyPAPIExpansion expansion = new EzEconomyPAPIExpansion(null);

        OfflinePlayer p = offlinePlayer(UUID.randomUUID());

        String v1 = expansion.onPlaceholderRequest(p, "balance_formatted");
        // Note: current implementation treats the bare "balance_formatted" token as currency "formatted"
        assertEquals("0.00 formatted", v1);

        String v2 = expansion.onPlaceholderRequest(p, "balance_formatted_dollar");
        assertEquals("0.00 formatted_dollar", v2);
    }

    @Test
    public void balance_short_with_null_storage_returns_short_format_of_zero() {
        EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new TestEzEconomyStubs.SimpleTestEz(null, "dollar");
        EzEconomyPAPIExpansion expansion = new EzEconomyPAPIExpansion(null);

        OfflinePlayer p = offlinePlayer(UUID.randomUUID());

        String s1 = expansion.onPlaceholderRequest(p, "balance_short");
        // bare "balance_short" maps to currency "short" in current implementation
        assertEquals("0.00 short", s1);

        String s2 = expansion.onPlaceholderRequest(p, "balance_short_dollar");
        assertEquals("0.00 short_dollar", s2);
    }

    @Test
    public void balance_uses_default_currency_when_preference_manager_is_null() {
        TestEzEconomyStubs.SimpleStorageProvider sp = new TestEzEconomyStubs.SimpleStorageProvider();
        UUID id = UUID.randomUUID();
        sp.setBalance(id, "dollar", 12.34);

        // SimpleTestEz has a null CurrencyPreferenceManager by default
        EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new TestEzEconomyStubs.SimpleTestEz(sp, "dollar");

        EzEconomyPAPIExpansion expansion = new EzEconomyPAPIExpansion(null);

        OfflinePlayer p = offlinePlayer(id);

        String out = expansion.onPlaceholderRequest(p, "balance");
        assertTrue(out.contains("12.34"));
    }

    private OfflinePlayer offlinePlayer(UUID id) {
        java.lang.reflect.InvocationHandler h = (proxy, method, args) -> {
            String name = method.getName();
            if ("getUniqueId".equals(name)) return id;
            Class<?> r = method.getReturnType();
            if (r.equals(boolean.class)) return false;
            if (r.equals(int.class)) return 0;
            if (r.equals(long.class)) return 0L;
            return null;
        };
        return (OfflinePlayer) java.lang.reflect.Proxy.newProxyInstance(OfflinePlayer.class.getClassLoader(), new Class[]{OfflinePlayer.class}, h);
    }
}
