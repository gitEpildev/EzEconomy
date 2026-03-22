package com.skyblockexp.ezeconomy.papi;

import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.Test;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.api.storage.StorageProvider;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class IntegrationEzEconomyPAPIExpansionTest {

    // No setup/teardown required for these tests

    static class StubStorage implements StorageProvider {
        private final Map<UUID, Double> balances = new HashMap<>();

        public void put(UUID u, double v) { balances.put(u, v); }

        @Override public void init() {}
        @Override public void load() {}
        @Override public void save() {}
        @Override public double getBalance(UUID uuid, String currency) { return balances.getOrDefault(uuid, 0.0); }
        @Override public void setBalance(UUID uuid, String currency, double amount) { balances.put(uuid, amount); }
        @Override public void logTransaction(com.skyblockexp.ezeconomy.api.storage.models.Transaction transaction) {}
        @Override public java.util.List<com.skyblockexp.ezeconomy.api.storage.models.Transaction> getTransactions(UUID uuid, String currency) { return Collections.emptyList(); }
        @Override public boolean tryWithdraw(UUID uuid, String currency, double amount) { Double v = balances.get(uuid); if (v == null || v < amount) return false; balances.put(uuid, v - amount); return true; }
        @Override public void deposit(UUID uuid, String currency, double amount) { balances.put(uuid, balances.getOrDefault(uuid,0.0)+amount); }
        @Override public Map<UUID, Double> getAllBalances(String currency) { return new HashMap<>(balances); }
        @Override public java.util.Set<String> cleanupOrphanedPlayers() { return Collections.emptySet(); }
        @Override public boolean isConnected() { return true; }
        @Override public com.skyblockexp.ezeconomy.storage.TransferResult transfer(UUID fromUuid, UUID toUuid, String currency, double amount) { return com.skyblockexp.ezeconomy.storage.TransferResult.failure(0,0); }
        @Override public void shutdown() {}
        @Override public boolean createBank(String name, UUID owner) { return false; }
        @Override public boolean deleteBank(String name) { return false; }
        @Override public boolean bankExists(String name) { return false; }
        @Override public double getBankBalance(String name, String currency) { return 0; }
        @Override public void setBankBalance(String name, String currency, double amount) {}
        @Override public boolean tryWithdrawBank(String name, String currency, double amount) { return false; }
        @Override public void depositBank(String name, String currency, double amount) {}
        @Override public Set<String> getBanks() { return Collections.emptySet(); }
        @Override public boolean isBankOwner(String name, UUID uuid) { return false; }
        @Override public boolean isBankMember(String name, UUID uuid) { return false; }
        @Override public boolean addBankMember(String name, UUID uuid) { return false; }
        @Override public boolean removeBankMember(String name, UUID uuid) { return false; }
        @Override public Set<UUID> getBankMembers(String name) { return Collections.emptySet(); }
    }

    static class StubEzEconomy implements EzEconomyPAPIExpansion.TestEzEconomy {
        private final StubStorage storage = new StubStorage();

        @Override
        public StorageProvider getStorageOrWarn() { return storage; }

        @Override
        public String getDefaultCurrency() { return "dollar"; }

        @Override
        public String format(double amount, String currency) { return String.format("%.2f %s", amount, currency); }

        @Override
        public String getCurrencySymbol(String currency) { return "$"; }

        @Override
        public com.skyblockexp.ezeconomy.manager.CurrencyPreferenceManager getCurrencyPreferenceManager() { return null; }
    }

    @Test
    public void integrationPlaceholderResponsesWithoutBukkit() throws Exception {
        // Use test hook to inject EzEconomy instance into expansion
        StubEzEconomy stub = new StubEzEconomy();
        EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = stub;

        // prepare storage data
        UUID u = UUID.randomUUID();
        stub.getStorageOrWarn().setBalance(u, "dollar", 123.45);
        stub.getStorageOrWarn().setBalance(UUID.randomUUID(), "dollar", 10.0);

        // create a simple OfflinePlayer proxy for the test (avoids implementing all methods)
        OfflinePlayer fakePlayer = (OfflinePlayer) java.lang.reflect.Proxy.newProxyInstance(
                OfflinePlayer.class.getClassLoader(),
                new Class[]{OfflinePlayer.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getUniqueId": return u;
                        case "getName": return "TestPlayer";
                        case "isOnline": return false;
                        case "hasPlayedBefore": return true;
                        default:
                            Class<?> ret = method.getReturnType();
                            if (ret.equals(boolean.class)) return false;
                            if (ret.equals(long.class)) return 0L;
                            return null;
                    }
                }
        );

        EzEconomyPAPIExpansion expansion = new EzEconomyPAPIExpansion(null);

        // balance for player via injected EzEconomy
        String balance = expansion.onPlaceholderRequest(fakePlayer, "balance");
        assertNotNull(balance);
        assertTrue(balance.contains("123.45"));

        // symbol
        String symbol = expansion.onPlaceholderRequest(null, "symbol_dollar");
        assertNotNull(symbol);

        // put a cached top value and verify it's returned without scheduling
        Field cacheField = EzEconomyPAPIExpansion.class.getDeclaredField("topCache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> topCache = (Map<String, Object>) cacheField.get(expansion);
        // create CacheEntry via reflection
        Class<?>[] inner = EzEconomyPAPIExpansion.class.getDeclaredClasses();
        Class<?> cacheEntryClass = null;
        for (Class<?> c : inner) if (c.getSimpleName().equals("CacheEntry")) cacheEntryClass = c;
        assertNotNull(cacheEntryClass);
        Constructor<?> ctor = cacheEntryClass.getDeclaredConstructor(String.class, long.class);
        ctor.setAccessible(true);
        Object entry = ctor.newInstance("player - 123.45 dollar", System.currentTimeMillis() + 10000L);
        topCache.put("top:dollar:1", entry);

        String top = expansion.onPlaceholderRequest(null, "top_1_dollar");
        assertEquals("player - 123.45 dollar", top);

        // bank (storage returns 0)
        String bank = expansion.onPlaceholderRequest(null, "bank_test_dollar");
        assertNotNull(bank);

        // clear test hook
        EzEconomyPAPIExpansion.TEST_ECONOMY_FOR_TESTS = null;
    }
}
