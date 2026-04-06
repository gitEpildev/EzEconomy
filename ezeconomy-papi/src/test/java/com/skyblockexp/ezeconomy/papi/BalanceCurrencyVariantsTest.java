package com.skyblockexp.ezeconomy.papi;

import com.skyblockexp.ezeconomy.papi.testhelpers.TestEzEconomyStubs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class BalanceCurrencyVariantsTest {

    @AfterEach
    public void tearDown() {
        try { MockBukkit.unmock(); } catch (Exception ignored) {}
        EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = null;
    }

    @Test
    public void balance_formatted_and_short_with_currency_suffix_are_handled() throws Exception {
        MockBukkit.mock();
        EzEconomyPapiPlugin papi = (EzEconomyPapiPlugin) MockBukkit.load(EzEconomyPapiPlugin.class);

        TestEzEconomyStubs.SimpleStorageProvider sp = new TestEzEconomyStubs.SimpleStorageProvider();
        UUID u = UUID.randomUUID();
        sp.setBalance(u, "gold", 2500.5);

        TestEzEconomyStubs.SimpleTestEz stub = new TestEzEconomyStubs.SimpleTestEz(sp, "dollar") {
            @Override public String format(double amount, String currency) { return String.format("FMT:%.1f:%s", amount, currency); }
            @Override public String formatShort(double amount, String currency) { return String.format("SRT:%.1f:%s", amount, currency); }
        };

        EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = stub;
        EzEconomyPAPIExpansion expansion = new EzEconomyPAPIExpansion(papi);

        org.bukkit.OfflinePlayer fake = (org.bukkit.OfflinePlayer) java.lang.reflect.Proxy.newProxyInstance(
                org.bukkit.OfflinePlayer.class.getClassLoader(), new Class[]{org.bukkit.OfflinePlayer.class}, (proxy, method, args) -> {
                    if ("getUniqueId".equals(method.getName())) return u;
                    if (method.getReturnType().equals(boolean.class)) return false;
                    return null;
                }
        );

        String formatted = expansion.onPlaceholderRequest(fake, "balance_formatted_gold");
        assertNotNull(formatted);
        assertTrue(formatted.contains("gold") || formatted.contains("FMT:"));

        String shorted = expansion.onPlaceholderRequest(fake, "balance_short_gold");
        assertNotNull(shorted);
        assertTrue(shorted.contains("gold") || shorted.contains("SRT:"));
    }
}
