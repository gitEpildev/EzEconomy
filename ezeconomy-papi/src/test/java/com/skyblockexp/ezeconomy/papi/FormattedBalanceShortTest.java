package com.skyblockexp.ezeconomy.papi;

import com.skyblockexp.ezeconomy.papi.testhelpers.TestEzEconomyStubs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class FormattedBalanceShortTest {

    @AfterEach
    public void tearDown() {
        try { MockBukkit.unmock(); } catch (Exception ignored) {}
        EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = null;
    }

    @Test
    public void balance_formatted_and_short_with_null_storage_returns_zero_formats() throws Exception {
        MockBukkit.mock();
        EzEconomyPapiPlugin papi = (EzEconomyPapiPlugin) MockBukkit.load(EzEconomyPapiPlugin.class);

        // TestEz with null storage should trigger the storage==null branches
        TestEzEconomyStubs.SimpleTestEz stub = new TestEzEconomyStubs.SimpleTestEz(null, "dollar") {
            @Override public String format(double amount, String currency) { return "FMT:" + amount + ":" + currency; }
            @Override public String formatShort(double amount, String currency) { return "SRT:" + amount + ":" + currency; }
        };

        EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = stub;
        EzEconomyPAPIExpansion expansion = new EzEconomyPAPIExpansion(papi);

        // offline player proxy with an arbitrary UUID
        java.util.UUID u = UUID.randomUUID();
        org.bukkit.OfflinePlayer fake = (org.bukkit.OfflinePlayer) java.lang.reflect.Proxy.newProxyInstance(
                org.bukkit.OfflinePlayer.class.getClassLoader(), new Class[]{org.bukkit.OfflinePlayer.class}, (proxy, method, args) -> {
                    if ("getUniqueId".equals(method.getName())) return u;
                    if (method.getReturnType().equals(boolean.class)) return false;
                    return null;
                }
        );

        String f = expansion.onPlaceholderRequest(fake, "balance_formatted");
        assertNotNull(f);
        assertFalse(f.isEmpty());

        String s = expansion.onPlaceholderRequest(fake, "balance_short");
        assertNotNull(s);
        assertFalse(s.isEmpty());
    }
}
