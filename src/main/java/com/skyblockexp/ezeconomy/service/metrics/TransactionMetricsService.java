package com.skyblockexp.ezeconomy.service.metrics;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.api.storage.StorageProvider;
import com.skyblockexp.ezeconomy.api.storage.models.Transaction;

public class TransactionMetricsService {
    private final java.util.concurrent.atomic.AtomicLong totalDepositedCents = new java.util.concurrent.atomic.AtomicLong(0L);
    private final java.util.concurrent.atomic.AtomicLong totalWithdrawnCents = new java.util.concurrent.atomic.AtomicLong(0L);
    private final java.util.concurrent.atomic.AtomicLong totalConvertedCents = new java.util.concurrent.atomic.AtomicLong(0L);

    private final EzEconomyPlugin plugin;

    public TransactionMetricsService(EzEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    public void logTransaction(Transaction transaction) {
        StorageProvider storage = plugin.getStorage();
        if (storage != null) {
            double amt = transaction.getAmount();
            try {
                long cents = Math.round(java.math.BigDecimal.valueOf(amt).movePointRight(2).doubleValue());
                if (amt > 0) {
                    totalDepositedCents.addAndGet(cents);
                } else if (amt < 0) {
                    totalWithdrawnCents.addAndGet(Math.abs(cents));
                }
            } catch (Exception ignored) {}
            storage.logTransaction(transaction);
        }
    }

    public void recordConversion(double amount) {
        try {
            long cents = Math.round(java.math.BigDecimal.valueOf(amount).movePointRight(2).doubleValue());
            totalConvertedCents.addAndGet(cents);
        } catch (Exception ignored) {}
    }

    public long getTotalDepositedCents() { return totalDepositedCents.get(); }
    public long getTotalWithdrawnCents() { return totalWithdrawnCents.get(); }
    public long getTotalConvertedCents() { return totalConvertedCents.get(); }
}
