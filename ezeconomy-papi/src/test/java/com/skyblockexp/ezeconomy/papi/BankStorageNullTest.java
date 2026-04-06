package com.skyblockexp.ezeconomy.papi;

import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.*;

public class BankStorageNullTest extends TestBase {

    @Test
    public void bank_with_null_storage_returnsEmpty() throws Exception {
        MockBukkit.mock();
        EzEconomyPapiPlugin papi = (EzEconomyPapiPlugin) MockBukkit.load(EzEconomyPapiPlugin.class);

        EzEconomyPAPIExpansion.TestEzEconomy stub = new EzEconomyPAPIExpansion.TestEzEconomy() {
            @Override public com.skyblockexp.ezeconomy.api.storage.StorageProvider getStorageOrWarn() { return null; }
            @Override public String getDefaultCurrency() { return "dollar"; }
            @Override public String format(double amount, String currency) { return ""; }
            @Override public String formatShort(double amount, String currency) { return ""; }
            @Override public String getCurrencySymbol(String currency) { return "$"; }
            @Override public com.skyblockexp.ezeconomy.manager.CurrencyPreferenceManager getCurrencyPreferenceManager() { return null; }
        };

        EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = stub;
        EzEconomyPAPIExpansion expansion = new EzEconomyPAPIExpansion(papi);

        String out = expansion.onPlaceholderRequest(null, "bank_mybank_dollar");
        assertEquals("", out);
    }
}
