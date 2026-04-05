package com.skyblockexp.ezeconomy.papi;

import org.junit.jupiter.api.Test;
import org.bukkit.OfflinePlayer;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

public class AdditionalPAPIExpansionTest {

    private OfflinePlayer offlinePlayer(java.util.UUID id) {
        InvocationHandler h = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String name = method.getName();
                if ("getUniqueId".equals(name)) return id;
                if (method.getReturnType().equals(boolean.class)) return false;
                if (method.getReturnType().equals(int.class)) return 0;
                if (method.getReturnType().equals(long.class)) return 0L;
                return null;
            }
        };
        return (OfflinePlayer) Proxy.newProxyInstance(OfflinePlayer.class.getClassLoader(), new Class[]{OfflinePlayer.class}, h);
    }

    @Test
    public void playerOverride_delegatesToOffline() throws Exception {
        EzEconomyPAPIExpansion expansion = new EzEconomyPAPIExpansion(null);

        // Use the existing test hook from other tests to avoid Bukkit
        EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new EzEconomyPAPIExpansion.TestEzEconomy() {
            @Override public com.skyblockexp.ezeconomy.api.storage.StorageProvider getStorageOrWarn() { return null; }
            @Override public String getDefaultCurrency() { return "dollar"; }
            @Override public String format(double amount, String currency) { return String.format("$%.2f", amount); }
            @Override public String formatShort(double amount, String currency) { return String.format("$%.0f", amount); }
            @Override public String getCurrencySymbol(String currency) { return "$"; }
            @Override public com.skyblockexp.ezeconomy.manager.CurrencyPreferenceManager getCurrencyPreferenceManager() { return null; }
        };

        java.util.UUID id = java.util.UUID.randomUUID();
        org.bukkit.entity.Player playerProxy = (org.bukkit.entity.Player) Proxy.newProxyInstance(
                org.bukkit.entity.Player.class.getClassLoader(),
                new Class[]{org.bukkit.entity.Player.class},
                (proxy, method, args) -> {
                    if ("getUniqueId".equals(method.getName())) return id;
                    Class<?> r = method.getReturnType();
                    if (r.equals(boolean.class)) return false;
                    if (r.equals(long.class)) return 0L;
                    return null;
                }
        );

        String out = expansion.onPlaceholderRequest(playerProxy, "balance");
        // Should return formatted zero from our test hook when storage is null
        assertEquals("$0.00", out);
    }

    @Test
    public void parseIntOrDefault_and_safe_behaviour_via_reflection() throws Exception {
        Class<?> cls = EzEconomyPAPIExpansion.class;

        Method parse = cls.getDeclaredMethod("parseIntOrDefault", String.class, int.class);
        parse.setAccessible(true);
        assertEquals(5, parse.invoke(null, "5", 10));
        assertEquals(10, parse.invoke(null, "notanumber", 10));
        assertEquals(7, parse.invoke(null, null, 7));

        Method safe = cls.getDeclaredMethod("safe", String.class);
        safe.setAccessible(true);
        assertEquals("", safe.invoke(null, (Object) null));
        assertEquals("abc", safe.invoke(null, "abc"));
    }
}
