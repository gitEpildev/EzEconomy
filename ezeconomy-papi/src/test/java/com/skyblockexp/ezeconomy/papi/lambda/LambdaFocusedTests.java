package com.skyblockexp.ezeconomy.papi.lambda;

import com.skyblockexp.ezeconomy.api.storage.StorageProvider;
import com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion;
import com.skyblockexp.ezeconomy.papi.EzEconomyPapiPlugin;
import com.skyblockexp.ezeconomy.papi.TestBase;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class LambdaFocusedTests extends TestBase {

    static class SimpleStorage implements StorageProvider {
        private final Map<UUID, Double> balances = new HashMap<>();
        public void put(UUID u, double v) { balances.put(u, v); }
        @Override public void init() {}
        @Override public void load() {}
        @Override public void save() {}
        @Override public double getBalance(UUID uuid, String currency) { return balances.getOrDefault(uuid, 0.0); }
        @Override public void setBalance(UUID uuid, String currency, double amount) { balances.put(uuid, amount); }
        @Override public void logTransaction(com.skyblockexp.ezeconomy.api.storage.models.Transaction transaction) {}
        @Override public java.util.List<com.skyblockexp.ezeconomy.api.storage.models.Transaction> getTransactions(UUID uuid, String currency) { return java.util.Collections.emptyList(); }
        @Override public boolean tryWithdraw(UUID uuid, String currency, double amount) { return false; }
        @Override public void deposit(UUID uuid, String currency, double amount) {}
        @Override public Map<UUID, Double> getAllBalances(String currency) { return new HashMap<>(balances); }
        @Override public java.util.Set<String> cleanupOrphanedPlayers() { return java.util.Collections.emptySet(); }
        @Override public boolean isConnected() { return true; }
        @Override public com.skyblockexp.ezeconomy.storage.TransferResult transfer(UUID fromUuid, UUID toUuid, String currency, double amount) { return com.skyblockexp.ezeconomy.storage.TransferResult.failure(0,0); }
        @Override public void shutdown() {}
        @Override public com.skyblockexp.ezeconomy.dto.EconomyPlayer getPlayer(UUID uuid) { return new com.skyblockexp.ezeconomy.dto.EconomyPlayer(uuid, "User", null); }
        @Override public boolean createBank(String name, UUID owner) { return false; }
        @Override public boolean deleteBank(String name) { return false; }
        @Override public boolean bankExists(String name) { return false; }
        @Override public double getBankBalance(String name, String currency) { return 0; }
        @Override public void setBankBalance(String name, String currency, double amount) {}
        @Override public boolean tryWithdrawBank(String name, String currency, double amount) { return false; }
        @Override public void depositBank(String name, String currency, double amount) {}
        @Override public java.util.Set<String> getBanks() { return java.util.Collections.emptySet(); }
        @Override public java.util.Set<java.util.UUID> getBankMembers(String name) { return java.util.Collections.emptySet(); }
        @Override public boolean isBankOwner(String name, UUID uuid) { return false; }
        @Override public boolean isBankMember(String name, UUID uuid) { return false; }
        @Override public boolean addBankMember(String name, UUID uuid) { return false; }
        @Override public boolean removeBankMember(String name, UUID uuid) { return false; }
    }

    @Test
    public void invoke_lambda1_with_and_without_storage() throws Exception {
        MockBukkit.mock();
        com.skyblockexp.ezeconomy.core.EzEconomyPlugin core = (com.skyblockexp.ezeconomy.core.EzEconomyPlugin) MockBukkit.load(com.skyblockexp.ezeconomy.papi.EzPluginPathCoverageTest.SimpleEz.class);
        EzEconomyPapiPlugin papi = (EzEconomyPapiPlugin) MockBukkit.load(EzEconomyPapiPlugin.class);
        EzEconomyPAPIExpansion expansion = new EzEconomyPAPIExpansion(papi);

        java.util.UUID u1 = java.util.UUID.randomUUID();
        org.bukkit.OfflinePlayer off1 = org.bukkit.Bukkit.getOfflinePlayer(u1);

        core.setStorage(null);
        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion.computeTopSyncForProd(core, "usd", "top:usd:1", 1);

        SimpleStorage ss = new SimpleStorage();
        core.setStorage(ss);
        expansion.handlePlaceholderRequestForTests(off1, "top_1_usd");
    }

    @Test
    public void invoke_lambda2_name_fallbacks() throws Exception {
        MockBukkit.mock();
        com.skyblockexp.ezeconomy.core.EzEconomyPlugin core = (com.skyblockexp.ezeconomy.core.EzEconomyPlugin) MockBukkit.load(com.skyblockexp.ezeconomy.papi.EzPluginPathCoverageTest.SimpleEz.class);
        EzEconomyPapiPlugin papi = (EzEconomyPapiPlugin) MockBukkit.load(EzEconomyPapiPlugin.class);
        EzEconomyPAPIExpansion expansion = new EzEconomyPAPIExpansion(papi);

        AbstractMap.SimpleEntry<java.util.UUID, Double> entry = new AbstractMap.SimpleEntry<>(java.util.UUID.randomUUID(), 42.0);
        org.bukkit.OfflinePlayer off2 = org.bukkit.Bukkit.getOfflinePlayer(entry.getKey());

        core.setStorage(null);
        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion.computeTopSyncForProd(core, "usd", "top:usd:1", 1);
        com.skyblockexp.ezeconomy.cache.ExpiringCache.Entry<?> e1 = com.skyblockexp.ezeconomy.cache.CacheManager.getProvider().getEntry("top:usd:1");
        assertNotNull(e1);

        SimpleStorage ss2 = new SimpleStorage();
        core.setStorage(ss2);
        com.skyblockexp.ezeconomy.papi.EzEconomyPAPIExpansion.computeTopSyncForProd(core, "usd", "top:usd:1", 1);
        com.skyblockexp.ezeconomy.cache.ExpiringCache.Entry<?> e2 = com.skyblockexp.ezeconomy.cache.CacheManager.getProvider().getEntry("top:usd:1");
        assertNotNull(e2);
    }
}
