package com.skyblockexp.ezeconomy.papi;

import com.skyblockexp.ezeconomy.api.storage.StorageProvider;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class EzEconomyPAPIExpansion extends PlaceholderExpansion {
    private final EzEconomyPapiPlugin plugin;
    private final ConcurrentMap<String, CacheEntry> topCache = new ConcurrentHashMap<>();
    private static final long TOP_CACHE_TTL_MS = 30_000L; // 30 seconds

    private static final class CacheEntry {
        final String value;
        final long expiresAt;

        CacheEntry(String v, long expiresAt) {
            this.value = v;
            this.expiresAt = expiresAt;
        }
    }

    public EzEconomyPAPIExpansion(EzEconomyPapiPlugin plugin) {
        this.plugin = plugin;
    }

    // Test hook: when non-null, tests will use this EzEconomy instance instead of
    // looking up the plugin via Bukkit.getPluginManager().getPlugin("EzEconomy").
    public interface TestEzEconomy {
        StorageProvider getStorageOrWarn();
        String getDefaultCurrency();
        String format(double amount, String currency);
        String getCurrencySymbol(String currency);
        com.skyblockexp.ezeconomy.manager.CurrencyPreferenceManager getCurrencyPreferenceManager();
    }

    public static TestEzEconomy TEST_ECONOMY_FOR_TESTS = null;

    private static int parseIntOrDefault(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception ignored) { return def; }
    }

    public boolean persist() {
        return true;
    }

    public boolean canRegister() {
        return true;
    }

    public String getAuthor() {
        return String.join(",", plugin.getDescription().getAuthors());
    }

    public String getIdentifier() {
        return "ezeconomy";
    }

    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    public String onPlaceholderRequest(OfflinePlayer offlinePlayer, String identifier) {
        EzEconomyPlugin ezPlugin = null;
        TestEzEconomy testEz = TEST_ECONOMY_FOR_TESTS;
        if (testEz == null) {
            if (Bukkit.getPluginManager().getPlugin("EzEconomy") instanceof EzEconomyPlugin) {
                ezPlugin = (EzEconomyPlugin) Bukkit.getPluginManager().getPlugin("EzEconomy");
            }
            if (ezPlugin == null) return "";
        }

        try {
            if (identifier.equalsIgnoreCase("balance")) {
                if (offlinePlayer == null) return "0";
                UUID uuid = offlinePlayer.getUniqueId();
                String pref;
                if (testEz != null) {
                    pref = testEz.getCurrencyPreferenceManager() == null ? testEz.getDefaultCurrency() : testEz.getCurrencyPreferenceManager().getPreferredCurrency(uuid);
                    String currency = pref == null ? testEz.getDefaultCurrency() : pref;
                    StorageProvider storage = testEz.getStorageOrWarn();
                    if (storage == null) return testEz.format(0d, currency);
                    double bal = storage.getBalance(uuid, currency);
                    return testEz.format(bal, currency);
                } else {
                    pref = ezPlugin.getCurrencyPreferenceManager() == null ? ezPlugin.getDefaultCurrency() : ezPlugin.getCurrencyPreferenceManager().getPreferredCurrency(uuid);
                    String currency = pref == null ? ezPlugin.getDefaultCurrency() : pref;
                    StorageProvider storage = ezPlugin.getStorageOrWarn();
                    if (storage == null) return ezPlugin.format(0d, currency);
                    double bal = storage.getBalance(uuid, currency);
                    return ezPlugin.format(bal, currency);
                }
            }

            if (identifier.startsWith("balance_")) {
                if (offlinePlayer == null) return "0";
                String currency = identifier.substring("balance_".length());
                UUID uuid = offlinePlayer.getUniqueId();
                if (testEz != null) {
                    StorageProvider storage = testEz.getStorageOrWarn();
                    if (storage == null) return testEz.format(0d, currency);
                    double bal = storage.getBalance(uuid, currency);
                    return testEz.format(bal, currency);
                } else {
                    StorageProvider storage = ezPlugin.getStorageOrWarn();
                    if (storage == null) return ezPlugin.format(0d, currency);
                    double bal = storage.getBalance(uuid, currency);
                    return ezPlugin.format(bal, currency);
                }
            }

            if (identifier.startsWith("symbol_")) {
                String currency = identifier.substring("symbol_".length());
                String out = testEz != null ? testEz.getCurrencySymbol(currency) : ezPlugin.getCurrencySymbol(currency);
                if (testEz != null) {
                    System.out.println("DEBUG expansion.symbol: testEz=" + (testEz != null) + " class=" + testEz.getClass().getName());
                    try {
                        System.out.println(java.util.Arrays.toString(testEz.getClass().getMethods()));
                    } catch (Throwable ignore) {}
                    try {
                        java.lang.reflect.Method mm = testEz.getClass().getMethod("getCurrencySymbol", String.class);
                        Object rr = mm.invoke(testEz, currency);
                        System.out.println("DEBUG expansion.symbol: reflect returned='" + rr + "' (class=" + (rr==null?"null":rr.getClass().getName()) + ")");
                    } catch (Throwable t) {
                        System.out.println("DEBUG expansion.symbol: reflection failed: " + t);
                    }
                }
                System.out.println("DEBUG expansion.symbol: testEz=" + (testEz != null) + " out='" + out + "'");
                return out;
            }

            if (identifier.startsWith("top_")) {
                // format: top_<n>_<currency>
                String[] parts = identifier.split("_");
                if (parts.length < 3) return "";
                final int nFinal = parseIntOrDefault(parts[1], 10);
                String currency = parts[2];
                String cacheKey = "top:" + currency + ":" + nFinal;

                CacheEntry entry = topCache.get(cacheKey);
                long now = System.currentTimeMillis();
                if (entry != null && entry.expiresAt > now) {
                    return entry.value;
                }

                // If we had a cached but expired value, return it immediately while refreshing async.
                String previous = entry == null ? "loading" : entry.value;

                // Refresh asynchronously. If a test hook is present, avoid scheduling
                // using the plugin instance (which may be null in tests) and instead
                // run inline within the test environment.
                if (testEz != null) {
                    try {
                        StorageProvider storage = testEz.getStorageOrWarn();
                        if (storage == null) {
                            topCache.put(cacheKey, new CacheEntry("", now + TOP_CACHE_TTL_MS));
                        } else {
                            Map<UUID, Double> all = storage.getAllBalances(currency);
                            if (all == null || all.isEmpty()) {
                                topCache.put(cacheKey, new CacheEntry("", now + TOP_CACHE_TTL_MS));
                            } else {
                                List<Map.Entry<UUID, Double>> top = all.entrySet().stream()
                                        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                                        .limit(nFinal)
                                        .collect(Collectors.toList());
                                String result = top.stream().map(e -> {
                                    OfflinePlayer p = Bukkit.getOfflinePlayer(e.getKey());
                                    String name = p == null ? e.getKey().toString() : (p.getName() == null ? e.getKey().toString() : p.getName());
                                    return name + " - " + testEz.format(e.getValue(), currency);
                                }).collect(Collectors.joining(", "));
                                topCache.put(cacheKey, new CacheEntry(result, System.currentTimeMillis() + TOP_CACHE_TTL_MS));
                            }
                        }
                    } catch (Throwable t) {
                        if (plugin != null) plugin.getLogger().warning("Failed to compute top placeholder for " + cacheKey + ": " + t.getMessage());
                    }
                } else {
                    EzEconomyPlugin finalEz = ezPlugin;
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        try {
                            StorageProvider storage = finalEz.getStorageOrWarn();
                            if (storage == null) {
                                topCache.put(cacheKey, new CacheEntry("", now + TOP_CACHE_TTL_MS));
                                return;
                            }
                            Map<UUID, Double> all = storage.getAllBalances(currency);
                            if (all == null || all.isEmpty()) {
                                topCache.put(cacheKey, new CacheEntry("", now + TOP_CACHE_TTL_MS));
                                return;
                            }
                            List<Map.Entry<UUID, Double>> top = all.entrySet().stream()
                                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                                    .limit(nFinal)
                                    .collect(Collectors.toList());
                            String result = top.stream().map(e -> {
                                OfflinePlayer p = Bukkit.getOfflinePlayer(e.getKey());
                                String name = p == null ? e.getKey().toString() : (p.getName() == null ? e.getKey().toString() : p.getName());
                                return name + " - " + finalEz.format(e.getValue(), currency);
                            }).collect(Collectors.joining(", "));
                            topCache.put(cacheKey, new CacheEntry(result, System.currentTimeMillis() + TOP_CACHE_TTL_MS));
                        } catch (Throwable t) {
                            plugin.getLogger().warning("Failed to compute top placeholder for " + cacheKey + ": " + t.getMessage());
                        }
                    });
                }

                return previous;
            }

            if (identifier.startsWith("bank_")) {
                // format: bank_<name>_<currency>
                String[] parts = identifier.split("_");
                if (parts.length < 3) return "";
                String bankName = parts[1];
                String currency = parts[2];
                StorageProvider storage = testEz != null ? testEz.getStorageOrWarn() : ezPlugin.getStorageOrWarn();
                if (storage == null) return "";
                double bal = storage.getBankBalance(bankName, currency);
                return testEz != null ? testEz.format(bal, currency) : ezPlugin.format(bal, currency);
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Error handling placeholder '" + identifier + "': " + t.getMessage());
        }

        return null;
    }
}
