package com.gitepildev.giteconomy.papi.formatting;

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
        com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin papi = (com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin) MockBukkit.load(com.gitepildev.giteconomy.papi.GitEconomyPapiPlugin.class);
        com.gitepildev.giteconomy.core.GitEconomyPlugin core = (com.gitepildev.giteconomy.core.GitEconomyPlugin) MockBukkit.load(com.gitepildev.giteconomy.papi.EzPluginPathCoverageTest.SimpleEz.class);

        com.gitepildev.giteconomy.api.storage.StorageProvider sp = new com.gitepildev.giteconomy.papi.EzPluginPathCoverageTest.TestStorage();
        UUID u = UUID.randomUUID();
        ((com.gitepildev.giteconomy.papi.EzPluginPathCoverageTest.TestStorage) sp).put(u, 321.0);
        core.setStorage(sp);

        com.gitepildev.giteconomy.service.format.CurrencyFormatter custom = new com.gitepildev.giteconomy.service.format.CurrencyFormatter(core) {
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

        java.lang.reflect.Field[] fields = org.bukkit.Bukkit.getPluginManager().getClass().getDeclaredFields();
        for (Field f : fields) {
            if (java.util.Map.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                Object map = f.get(org.bukkit.Bukkit.getPluginManager());
                try {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, org.bukkit.plugin.Plugin> m = (java.util.Map<String, org.bukkit.plugin.Plugin>) map;
                    m.put("GitEconomy", core);
                } catch (ClassCastException ignored) {}
            }
        }

        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TestGitEconomy() {
            @Override public com.gitepildev.giteconomy.api.storage.StorageProvider getStorageOrWarn() { return core.getStorageOrWarn(); }
            @Override public String getDefaultCurrency() { return core.getDefaultCurrency(); }
            @Override public String format(double amount, String currency) { return custom.format(amount, currency); }
            @Override public String formatShort(double amount, String currency) { return custom.formatShort(amount, currency); }
            @Override public String getCurrencySymbol(String currency) { try { return core.getCurrencyFormatter().getCurrencySymbol(currency); } catch (Throwable t) { return currency; } }
            @Override public com.gitepildev.giteconomy.manager.CurrencyPreferenceManager getCurrencyPreferenceManager() { return core.getCurrencyPreferenceManager(); }
        };

        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion expansion = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(papi);

        OfflinePlayer fake = com.gitepildev.giteconomy.papi.testhelpers.TestPlayerFakes.fakeOfflinePlayer(u);

        try {
            String formatted = expansion.onPlaceholderRequest(fake, "balance_formatted_eur");
            assertNotNull(formatted);
            assertTrue(formatted.startsWith("PRICE:") || formatted.startsWith("FMT:"));

            String shorted = expansion.onPlaceholderRequest(fake, "balance_short_eur");
            assertNotNull(shorted);
            assertTrue(shorted.startsWith("SHRT:") || shorted.startsWith("FMT:"));
        } finally {
            com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = null;
            try { MockBukkit.unmock(); } catch (Exception ignored) {}
        }
    }
}
