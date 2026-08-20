package com.gitepildev.giteconomy.papi.placeholders;

import org.bukkit.OfflinePlayer;
import com.gitepildev.giteconomy.papi.testhelpers.TestPlayerFakes;

import java.util.UUID;

public class DebugPlaceholderHarness {
    public static void main(String[] args) throws Exception {
        com.gitepildev.giteconomy.papi.placeholders.IntegrationGitEconomyPAPIExpansionTest.StubGitEconomy stub = new com.gitepildev.giteconomy.papi.placeholders.IntegrationGitEconomyPAPIExpansionTest.StubGitEconomy();
        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = stub;
        UUID u = UUID.randomUUID();
        stub.getStorageOrWarn().setBalance(u, "dollar", 123.45);

        com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion expansion = new com.gitepildev.giteconomy.papi.GitEconomyPAPIExpansion(null);

        OfflinePlayer fake = TestPlayerFakes.fakeOfflinePlayer(u);

        System.out.println("CALL balance => " + expansion.onPlaceholderRequest(fake, "balance"));
        System.out.println("CALL symbol => " + expansion.onPlaceholderRequest(null, "symbol_dollar"));
    }
}
