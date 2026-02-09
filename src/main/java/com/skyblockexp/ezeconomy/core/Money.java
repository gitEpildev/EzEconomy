package com.skyblockexp.ezeconomy.core;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Lightweight Money value object encapsulating an amount and currency id.
 * Internally uses BigDecimal for safe monetary arithmetic.
 */
public final class Money {
    private final BigDecimal amount;
    private final String currencyId;

    private Money(BigDecimal amount, String currencyId) {
        this.amount = amount;
        this.currencyId = currencyId == null ? "" : currencyId;
    }

    public static Money of(BigDecimal amount, String currencyId) {
        return new Money(amount, currencyId);
    }

    public static Money of(double amount, String currencyId) {
        return new Money(BigDecimal.valueOf(amount), currencyId);
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrencyId() {
        return currencyId;
    }

    public Money withScale(int scale, RoundingMode mode) {
        return new Money(amount.setScale(scale, mode), currencyId);
    }

    public String toDisplayString(Locale locale, int decimals, String symbol, boolean symbolPrefix) {
        NumberFormat nf = NumberFormat.getNumberInstance(locale == null ? Locale.getDefault() : locale);
        nf.setGroupingUsed(true);
        nf.setMinimumFractionDigits(decimals);
        nf.setMaximumFractionDigits(decimals);
        String formatted = nf.format(amount.setScale(decimals, RoundingMode.HALF_UP));
        if (symbol == null || symbol.isEmpty()) return formatted;
        if (symbolPrefix) return symbol + " " + formatted;
        return formatted + " " + symbol;
    }
}
