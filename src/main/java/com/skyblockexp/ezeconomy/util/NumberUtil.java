package com.skyblockexp.ezeconomy.util;

import com.skyblockexp.ezeconomy.core.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public class NumberUtil {
    /**
     * Parse user-provided amount strings into a Money object for a given currency id.
     * Accepts optional commas, leading/trailing currency symbols, and suffixes (k,m,b,t).
     * Examples: "$1,234.56", "1k", "2.5m", "€3,000"
     * Returns null when parsing fails.
     */
    public static Money parseMoney(String input, String currencyId) {
        if (input == null || input.isEmpty()) return null;
        String raw = input.trim();

        // Remove common currency symbols and whitespace
        raw = raw.replace('\u00A0', ' ');
        raw = raw.replaceAll("[\\$€£¥]", "").trim();

        // Lowercase for suffix handling
        String lower = raw.toLowerCase(Locale.ROOT);
        try {
            BigDecimal multiplier = BigDecimal.ONE;
            if (lower.endsWith("k")) {
                multiplier = BigDecimal.valueOf(1_000L);
                lower = lower.substring(0, lower.length() - 1);
            } else if (lower.endsWith("m")) {
                multiplier = BigDecimal.valueOf(1_000_000L);
                lower = lower.substring(0, lower.length() - 1);
            } else if (lower.endsWith("b")) {
                multiplier = BigDecimal.valueOf(1_000_000_000L);
                lower = lower.substring(0, lower.length() - 1);
            } else if (lower.endsWith("t")) {
                multiplier = new BigDecimal("1000000000000");
                lower = lower.substring(0, lower.length() - 1);
            }

            // Normalize spacing and non-digit separators
            String candidate = lower.replaceAll("\\s+", "");

            // Heuristic to detect German-style numbers (e.g. "1.234,56") vs US ("1,234.56")
            boolean looksGerman = false;
            if (candidate.contains(",") && candidate.contains(".")) {
                // If the last comma appears after the last dot, treat as German decimal separator
                looksGerman = candidate.lastIndexOf(',') > candidate.lastIndexOf('.');
            } else if (candidate.contains(",") && !candidate.contains(".")) {
                // If there's a comma and no dot, it might be either thousands (US) or decimal (German).
                // If there are exactly three digits after the comma, it's likely grouping (treat as US);
                // otherwise treat comma as decimal (German).
                int idx = candidate.lastIndexOf(',');
                if (candidate.length() - idx - 1 != 3) {
                    looksGerman = true;
                }
            }

            Number parsed;
            if (looksGerman) {
                NumberFormat nf = NumberFormat.getNumberInstance(Locale.GERMANY);
                nf.setGroupingUsed(true);
                parsed = nf.parse(candidate);
            } else {
                // Default to US-style parsing (commas as grouping, dot as decimal)
                String cleaned = candidate.replaceAll(",", "");
                NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
                nf.setGroupingUsed(true);
                parsed = nf.parse(cleaned);
            }

            BigDecimal value = new BigDecimal(parsed.toString()).multiply(multiplier);
            return Money.of(value.setScale(10, RoundingMode.HALF_UP), currencyId);
        } catch (ParseException | NumberFormatException ex) {
            return null;
        }
    }

    /**
     * Backwards-compatible API: parse to a primitive double using suffix rules.
     * Returns Double.NaN on failure.
     */
    public static double parseAmount(String input) {
        Money m = parseMoney(input, null);
        if (m == null) return Double.NaN;
        try {
            return m.getAmount().doubleValue();
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
    }

    /**
     * Compatibility alias for parseAmount(String).
     */
    public static Double parseDouble(String input) {
        return parseAmount(input);
    }
}
