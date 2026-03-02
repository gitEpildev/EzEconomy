package com.skyblockexp.ezeconomy.util;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class CurrencyUtil {
    private CurrencyUtil() {}

    /**
     * Convert an amount from one currency to another using config-defined conversion rates.
     * Returns Double.NaN if no conversion path exists.
     */
    public static double convert(EzEconomyPlugin plugin, double amount, String from, String to) {
        if (from == null || to == null) return Double.NaN;
        if (from.equalsIgnoreCase(to)) return amount;
        String f = from.toLowerCase();
        String t = to.toLowerCase();
        var cfg = com.skyblockexp.ezeconomy.core.Registry.getPlugin().getConfig();
        if (!cfg.isConfigurationSection("multi-currency.conversion")) return Double.NaN;

        // Build graph of rates (prefer explicit rates; add reciprocal if reverse missing)
        java.util.Map<String, java.util.Map<String, Double>> rates = new java.util.HashMap<>();
        var section = cfg.getConfigurationSection("multi-currency.conversion");
        for (String src : section.getKeys(false)) {
            var inner = section.getConfigurationSection(src);
            if (inner == null) continue;
            String srcKey = src.toLowerCase();
            rates.putIfAbsent(srcKey, new java.util.HashMap<>());
            for (String dst : inner.getKeys(false)) {
                double r = cfg.getDouble("multi-currency.conversion." + src + "." + dst, Double.NaN);
                if (Double.isNaN(r) || r == 0) continue;
                rates.get(srcKey).put(dst.toLowerCase(), r);
            }
        }

        // add reciprocal edges when reverse not explicitly defined
        for (String src : new java.util.ArrayList<>(rates.keySet())) {
            for (var entry : rates.get(src).entrySet()) {
                String dst = entry.getKey();
                double r = entry.getValue();
                rates.putIfAbsent(dst, new java.util.HashMap<>());
                if (!rates.get(dst).containsKey(src) && r != 0) {
                    rates.get(dst).put(src, 1.0 / r);
                }
            }
        }

        // BFS to find conversion path and compute composite rate
        java.util.Queue<java.util.Map.Entry<String, java.math.BigDecimal>> q = new java.util.ArrayDeque<>();
        java.util.Set<String> visited = new java.util.HashSet<>();
        q.add(new java.util.AbstractMap.SimpleEntry<>(f, java.math.BigDecimal.ONE));
        visited.add(f);

        while (!q.isEmpty()) {
            var cur = q.remove();
            String curC = cur.getKey();
            java.math.BigDecimal acc = cur.getValue();
            if (curC.equals(t)) {
                java.math.BigDecimal result = acc.multiply(java.math.BigDecimal.valueOf(amount));
                double converted = result.doubleValue();
                return roundToCurrency(plugin, converted, t);
            }
            var neighbors = rates.getOrDefault(curC, java.util.Collections.emptyMap());
            for (var nb : neighbors.entrySet()) {
                String nxt = nb.getKey();
                if (visited.contains(nxt)) continue;
                double edgeRate = nb.getValue();
                if (edgeRate == 0) continue;
                java.math.BigDecimal nextAcc = acc.multiply(java.math.BigDecimal.valueOf(edgeRate));
                visited.add(nxt);
                q.add(new java.util.AbstractMap.SimpleEntry<>(nxt, nextAcc));
            }
        }

        // no path found
        return Double.NaN;
    }

    private static double roundToCurrency(EzEconomyPlugin plugin, double value, String currency) {
        var cfg = com.skyblockexp.ezeconomy.core.Registry.getPlugin().getConfig();
        int decimals = cfg.getInt("multi-currency.currencies." + currency + ".decimals", 2);
        BigDecimal bd = BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
