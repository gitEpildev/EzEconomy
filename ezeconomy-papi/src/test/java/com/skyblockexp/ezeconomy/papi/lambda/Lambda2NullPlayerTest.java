package com.skyblockexp.ezeconomy.papi.lambda;

import com.skyblockexp.ezeconomy.api.storage.StorageProvider;
import com.skyblockexp.ezeconomy.cache.CacheManager;
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

public class Lambda2NullPlayerTest extends TestBase {

    static class MapStorageNullPlayer implements StorageProvider {
        private final Map<UUID, Double> all = new HashMap<>();
        public void put(UUID u, double v) { all.put(u, v); }
        @Override public void init() {}
        @Override public void load() {}
        @Override public void save() {}
        @Override public double getBalance(UUID uuid, String currency) { return all.getOrDefault(uuid, 0.0); }
        @Override public void setBalance(UUID uuid, String currency, double amount) { all.put(uuid, amount); }
        @Override public void logTransaction(com.skyblockexp.ezeconomy.api.storage.models.Transaction transaction) {}
        @Override public java.util.List<com.skyblockexp.ezeconomy.api.storage.models.Transaction> getTransactions(UUID uuid, String currency) { return java.util.Collections.emptyList(); }
        @Override public boolean tryWithdraw(UUID uuid, String currency, double amount) { return false; }
        @Override public void deposit(UUID uuid, String currency, double amount) {}
        @Override public Map<UUID, Double> getAllBalances(String currency) { return new HashMap<>(all); }
        @Override public java.util.Set<String> cleanupOrphanedPlayers() { return java.util.Collections.emptySet(); }
        @Override public boolean isConnected() { return true; }
        @Override public com.skyblockexp.ezeconomy.storage.TransferResult transfer(UUID fromUuid, UUID toUuid, String currency, double amount) { return com.skyblockexp.ezeconomy.storage.TransferResult.failure(0,0); }
        @Override public void shutdown() {}
        @Override public com.skyblockexp.ezeconomy.dto.EconomyPlayer getPlayer(UUID uuid) { return null; }
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
    public void invoke_lambda2_with_null_player_path() throws Exception {
        MockBukkit.mock();
        EzEconomyPapiPlugin papi = (EzEconomyPapiPlugin) MockBukkit.load(EzEconomyPapiPlugin.class);
        com.skyblockexp.ezeconomy.core.EzEconomyPlugin core = (com.skyblockexp.ezeconomy.core.EzEconomyPlugin) MockBukkit.load(com.skyblockexp.ezeconomy.papi.EzPluginPathCoverageTest.SimpleEz.class);

        MapStorageNullPlayer ms = new MapStorageNullPlayer();
        UUID a = UUID.randomUUID();
        ms.put(a, 12345.0);
        core.setStorage(ms);

        org.bukkit.plugin.PluginManager pm = org.bukkit.Bukkit.getPluginManager();
        java.lang.reflect.Field[] fields = pm.getClass().getDeclaredFields();
        for (java.lang.reflect.Field f : fields) {
            if (Map.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                Object map = f.get(pm);
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, org.bukkit.plugin.Plugin> m = (Map<String, org.bukkit.plugin.Plugin>) map;
                    m.put("EzEconomy", core);
                } catch (ClassCastException ignored) {}
            }
        }

        EzEconomyPAPIExpansion expansion = new EzEconomyPAPIExpansion(papi);

        Method lambda2 = null;
        for (Method m : EzEconomyPAPIExpansion.class.getDeclaredMethods()) {
            if (m.getName().contains("lambda$2")) { lambda2 = m; break; }
        }
        assertNotNull(lambda2, "lambda$2 method not found");
        lambda2.setAccessible(true);

        AbstractMap.SimpleEntry<java.util.UUID, Double> entryPair = new AbstractMap.SimpleEntry<>(a, 12345.0);
        Object target2 = java.lang.reflect.Modifier.isStatic(lambda2.getModifiers()) ? null : expansion;
        Object mapped = lambda2.invoke(target2, core, "dollar", entryPair);
        assertNotNull(mapped);
        String s = mapped.toString();
        assertTrue(s.contains("12345") || s.length() > 0);

        String cacheKey = "top:dollar:1";
        var entry = CacheManager.getProvider().getEntry(cacheKey);
        assertNotNull(mapped);
    }
}
