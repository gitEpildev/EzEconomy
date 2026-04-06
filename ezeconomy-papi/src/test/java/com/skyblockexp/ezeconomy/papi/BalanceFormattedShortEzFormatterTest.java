package com.skyblockexp.ezeconomy.papi;

import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class BalanceFormattedShortEzFormatterTest {

    @Test
    public void ezPlugin_customFormatter_format_paths_are_called() throws Exception {
        MockBukkit.mock();
        EzEconomyPapiPlugin papi = (EzEconomyPapiPlugin) MockBukkit.load(EzEconomyPapiPlugin.class);
        com.skyblockexp.ezeconomy.core.EzEconomyPlugin core = (com.skyblockexp.ezeconomy.core.EzEconomyPlugin) MockBukkit.load(EzPluginPathCoverageTest.SimpleEz.class);

        // install storage with a known uuid
        com.skyblockexp.ezeconomy.api.storage.StorageProvider sp = new EzPluginPathCoverageTest.TestStorage();
        UUID u = UUID.randomUUID();
        ((EzPluginPathCoverageTest.TestStorage) sp).put(u, 321.0);
        core.setStorage(sp);

        // inject a custom CurrencyFormatter that returns easily-assertable strings
        com.skyblockexp.ezeconomy.service.format.CurrencyFormatter custom = new com.skyblockexp.ezeconomy.service.format.CurrencyFormatter(core) {
            @Override public String format(double amount, String currency) { return "FMT:" + amount + ":" + currency; }
            @Override public String formatPriceForMessage(double amount, String currency) { return "PRICE:" + amount + ":" + currency; }
            @Override public String formatShort(double amount, String currency) { return "SHRT:" + amount + ":" + currency; }
        };
        Field cf = null;
        Class<?> ccls = core.getClass();
        while (ccls != null) {
            try { cf = ccls.getDeclaredField("currencyFormatter"); break; } catch (NoSuchFieldException e) { ccls = ccls.getSuperclass(); }
        }
        if (cf == null) throw new RuntimeException("currencyFormatter field not found");
        cf.setAccessible(true);
        cf.set(core, custom);

        // map plugin manager name
        java.lang.reflect.Field[] fields = org.bukkit.Bukkit.getPluginManager().getClass().getDeclaredFields();
        for (Field f : fields) {
            if (java.util.Map.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                Object map = f.get(org.bukkit.Bukkit.getPluginManager());
                try {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, org.bukkit.plugin.Plugin> m = (java.util.Map<String, org.bukkit.plugin.Plugin>) map;
                    m.put("EzEconomy", core);
                } catch (ClassCastException ignored) {}
            }
        }

        // Prefer the test hook to avoid depending on plugin manager mapping in MockBukkit
        EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new EzEconomyPAPIExpansion.TestEzEconomy() {
            @Override public com.skyblockexp.ezeconomy.api.storage.StorageProvider getStorageOrWarn() { return core.getStorageOrWarn(); }
            @Override public String getDefaultCurrency() { return core.getDefaultCurrency(); }
            @Override public String format(double amount, String currency) { return custom.format(amount, currency); }
            @Override public String formatShort(double amount, String currency) { return custom.formatShort(amount, currency); }
            @Override public String getCurrencySymbol(String currency) { try { return core.getCurrencyFormatter().getCurrencySymbol(currency); } catch (Throwable t) { return currency; } }
            @Override public com.skyblockexp.ezeconomy.manager.CurrencyPreferenceManager getCurrencyPreferenceManager() { return core.getCurrencyPreferenceManager(); }
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
            String formatted = expansion.onPlaceholderRequest(fake, "balance_formatted_eur");
            assertNotNull(formatted);
            assertTrue(formatted.startsWith("PRICE:") || formatted.startsWith("FMT:"));

            String shorted = expansion.onPlaceholderRequest(fake, "balance_short_eur");
            assertNotNull(shorted);
            assertTrue(shorted.startsWith("SHRT:") || shorted.startsWith("FMT:"));
        } finally {
            EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = null;
            try { MockBukkit.unmock(); } catch (Exception ignored) {}
        }
    }
}
