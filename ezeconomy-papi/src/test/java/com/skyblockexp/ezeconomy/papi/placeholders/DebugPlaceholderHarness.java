package com.skyblockexp.ezeconomy.papi.placeholders;

import org.bukkit.OfflinePlayer;
import com.skyblockexp.ezeconomy.papi.testhelpers.TestPlayerFakes;

import java.util.UUID;

public class DebugPlaceholderHarness {
    public static void main(String[] args) throws Exception {
        com.skyblockexp.ezeconomy.papi.placeholders.IntegrationEzEconomyPAPIExpansionTest.StubEzEconomy stub = new com.skyblockexp.ezeconomy.papi.placeholders.IntegrationEzEconomyPAPIExpansionTest.StubEzEconomy();
        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = stub;
        UUID u = UUID.randomUUID();
        stub.getStorageOrWarn().setBalance(u, "dollar", 123.45);

        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion expansion = new com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion(null);

        OfflinePlayer fake = TestPlayerFakes.fakeOfflinePlayer(u);

        System.out.println("CALL balance => " + expansion.onPlaceholderRequest(fake, "balance"));
        System.out.println("CALL symbol => " + expansion.onPlaceholderRequest(null, "symbol_dollar"));
    }
}
