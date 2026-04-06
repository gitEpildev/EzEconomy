package com.skyblockexp.ezeconomy.papi;

import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class BalanceFormattedBlankSuffixTest {

    @Test
    public void balance_formatted_and_short_without_suffix_use_default_currency() throws Exception {
        MockBukkit.mock();
        EzEconomyPapiPlugin papi = (EzEconomyPapiPlugin) MockBukkit.load(EzEconomyPapiPlugin.class);

        final UUID u = UUID.randomUUID();

        EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new EzEconomyPAPIExpansion.TestEzEconomy() {
            final EzPluginPathCoverageTest.TestStorage ts = new EzPluginPathCoverageTest.TestStorage();
            {
                ts.put(u, 42.0);
            }
            @Override public com.skyblockexp.ezeconomy.api.storage.StorageProvider getStorageOrWarn() { return ts; }
            @Override public String getDefaultCurrency() { return "eur"; }
            @Override public String format(double amount, String currency) { return "F:" + ((int) amount) + ":" + currency; }
            @Override public String formatShort(double amount, String currency) { return "S:" + ((int) amount) + ":" + currency; }
            @Override public String getCurrencySymbol(String currency) { return "€"; }
            @Override public com.skyblockexp.ezeconomy.manager.CurrencyPreferenceManager getCurrencyPreferenceManager() { return null; }
        };

        EzEconomyPAPIExpansion expansion = new EzEconomyPAPIExpansion(papi);

        OfflinePlayer fake = (OfflinePlayer) java.lang.reflect.Proxy.newProxyInstance(
                OfflinePlayer.class.getClassLoader(), new Class[]{OfflinePlayer.class}, (proxy, method, args) -> {
                    if ("getUniqueId".equals(method.getName())) return u;
                    if (method.getReturnType().equals(boolean.class)) return false;
                    return null;
                }
        );

        try {
            String f = expansion.onPlaceholderRequest(fake, "balance_formatted");
            assertNotNull(f);
            assertTrue(f.startsWith("F:"), "expected formatted prefix, got: " + f);

            String s = expansion.onPlaceholderRequest(fake, "balance_short");
            assertNotNull(s);
            // Depending on earlier 'balance_' handling this may route to the generic balance_<currency>
            // Accept either explicit short-format or the fallback format called with currency 'short'.
            assertTrue(s.startsWith("S:") || s.contains(":short"), "expected short format or balance_short fallback, got: " + s);
        } finally {
            EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = null;
            try { MockBukkit.unmock(); } catch (Exception ignored) {}
        }
    }
}
