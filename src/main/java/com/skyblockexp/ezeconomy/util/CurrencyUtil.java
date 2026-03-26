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
        // Delegate to BigDecimal-based implementation for better precision.
        if (from == null || to == null) return Double.NaN;
        if (from.equalsIgnoreCase(to)) return amount;
        ConversionResult res = convertBigDecimal(plugin, java.math.BigDecimal.valueOf(amount), from, to);
        if (res == null || res.converted == null) return Double.NaN;
        return res.converted.doubleValue();
    }

    public static double roundToCurrency(EzEconomyPlugin plugin, double value, String currency) {
        var cfg = plugin.getConfig();
        int decimals = cfg.getInt("multi-currency.currencies." + currency + ".decimals", 2);
        BigDecimal bd = BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static final class ConversionResult {
        public final BigDecimal converted; // amount to deposit in target currency
        public final BigDecimal usedSource; // amount of source currency to withdraw (may be null if full amount used)

        public ConversionResult(BigDecimal converted, BigDecimal usedSource) {
            this.converted = converted;
            this.usedSource = usedSource;
        }
    }

    /**
     * BigDecimal-based conversion that returns both the converted target amount and how much
     * of the source currency should be used (to allow leaving integer remainders when both
     * currencies have decimals == 0).
     * Returns null if no conversion path exists.
     */
    public static ConversionResult convertBigDecimal(EzEconomyPlugin plugin, BigDecimal amount, String from, String to) {
        if (from == null || to == null) return null;
        if (from.equalsIgnoreCase(to)) return new ConversionResult(amount, amount);
        String f = from.toLowerCase();
        String t = to.toLowerCase();
        var cfg = plugin.getConfig();
        if (!cfg.isConfigurationSection("multi-currency.conversion")) return null;

        // Build graph of rates (prefer explicit rates; add reciprocal if reverse missing)
        java.util.Map<String, java.util.Map<String, BigDecimal>> rates = new java.util.HashMap<>();
        var section = cfg.getConfigurationSection("multi-currency.conversion");
        for (String src : section.getKeys(false)) {
            var inner = section.getConfigurationSection(src);
            if (inner == null) continue;
            String srcKey = src.toLowerCase();
            rates.putIfAbsent(srcKey, new java.util.HashMap<>());
            for (String dst : inner.getKeys(false)) {
                String path = "multi-currency.conversion." + src + "." + dst;
                BigDecimal r;
                // try reading as string to preserve exact decimal literals
                String rs = cfg.getString(path, null);
                if (rs != null) {
                    try {
                        r = new BigDecimal(rs);
                    } catch (Exception ex) {
                        r = BigDecimal.valueOf(cfg.getDouble(path, Double.NaN));
                    }
                } else {
                    r = BigDecimal.valueOf(cfg.getDouble(path, Double.NaN));
                }
                if (r == null || r.compareTo(BigDecimal.ZERO) == 0) continue;
                rates.get(srcKey).put(dst.toLowerCase(), r);
            }
        }

        // add reciprocal edges when reverse not explicitly defined
        for (String src : new java.util.ArrayList<>(rates.keySet())) {
            for (var entry : rates.get(src).entrySet()) {
                String dst = entry.getKey();
                BigDecimal r = entry.getValue();
                rates.putIfAbsent(dst, new java.util.HashMap<>());
                if (!rates.get(dst).containsKey(src) && r.compareTo(BigDecimal.ZERO) != 0) {
                    rates.get(dst).put(src, BigDecimal.ONE.divide(r, 12, RoundingMode.HALF_UP));
                }
            }
        }

        // BFS to find conversion path and compute composite rate (BigDecimal)
        java.util.Queue<java.util.Map.Entry<String, BigDecimal>> q = new java.util.ArrayDeque<>();
        java.util.Set<String> visited = new java.util.HashSet<>();
        q.add(new java.util.AbstractMap.SimpleEntry<>(f, BigDecimal.ONE));
        visited.add(f);

        while (!q.isEmpty()) {
            var cur = q.remove();
            String curC = cur.getKey();
            BigDecimal acc = cur.getValue();
            if (curC.equals(t)) {
                BigDecimal result = acc.multiply(amount);
                // Determine decimals for currencies
                int sourceDecimals = cfg.getInt("multi-currency.currencies." + from + ".decimals", 2);
                int targetDecimals = cfg.getInt("multi-currency.currencies." + to + ".decimals", 2);

                if (sourceDecimals == 0 && targetDecimals == 0) {
                    // integer-to-integer: floor target units, compute required source used (ceiling)
                    BigDecimal targetUnits = result.setScale(0, RoundingMode.DOWN);
                    if (targetUnits.compareTo(BigDecimal.ZERO) == 0) {
                        return new ConversionResult(BigDecimal.ZERO, BigDecimal.ZERO);
                    }
                    // usedSource = ceil(targetUnits / acc)
                    BigDecimal usedSource = targetUnits.divide(acc, 0, RoundingMode.CEILING);
                    return new ConversionResult(targetUnits, usedSource);
                }

                // non-integer target: round to targetDecimals
                BigDecimal converted = result.setScale(targetDecimals, RoundingMode.HALF_UP);
                // assume full amount used from source for non-integer flows
                return new ConversionResult(converted, amount);
            }
            var neighbors = rates.getOrDefault(curC, java.util.Collections.emptyMap());
            for (var nb : neighbors.entrySet()) {
                String nxt = nb.getKey();
                if (visited.contains(nxt)) continue;
                BigDecimal edgeRate = nb.getValue();
                if (edgeRate.compareTo(BigDecimal.ZERO) == 0) continue;
                BigDecimal nextAcc = acc.multiply(edgeRate);
                visited.add(nxt);
                q.add(new java.util.AbstractMap.SimpleEntry<>(nxt, nextAcc));
            }
        }

        // no path found
        return null;
    }
}
