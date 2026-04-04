package com.skyblockexp.ezeconomy.core;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import java.util.concurrent.atomic.AtomicLong;

public class EzEconomyMetrics {
    private final Metrics metrics;
    private final AtomicLong lastSentDepositedCents = new AtomicLong(0L);
    private final AtomicLong lastSentWithdrawnCents = new AtomicLong(0L);
    private final AtomicLong lastSentConvertedCents = new AtomicLong(0L);

    public EzEconomyMetrics(org.bukkit.plugin.Plugin plugin) {
        // In test mode we avoid constructing bStats Metrics to prevent errors
        // when the bStats jar isn't relocated/available in the test classpath.
        if (Boolean.getBoolean("ezeconomy.test")) {
            this.metrics = null;
            return;
        }
        Metrics m = null;
        try {
            m = new Metrics(plugin, 28470);
            if (m != null) {
                try {
                    m.addCustomChart(new SimplePie("caching_strategy", () -> {
                        String v = plugin.getConfig().getString("caching-strategy");
                        if (v == null || v.isBlank()) {
                            v = plugin.getConfig().getString("locking-strategy", "unknown");
                        }
                        return v == null ? "unknown" : v;
                    }));

                    m.addCustomChart(new SimplePie("locking_strategy", () -> {
                        String v = plugin.getConfig().getString("locking-strategy", "unknown");
                        return v == null ? "unknown" : v;
                    }));

                    // Numeric single-line charts: report whole-unit totals (cents/100)
                    try {
                        EzEconomyPlugin ez = (EzEconomyPlugin) plugin;
                        // Report deltas since last send: compute current - lastSent and return whole-unit value
                        m.addCustomChart(new SingleLineChart("amount_deposited", () -> {
                            long current = ez.getTransactionMetricsService().getTotalDepositedCents();
                            long last = lastSentDepositedCents.getAndSet(current);
                            long delta = current - last;
                            if (delta < 0) delta = 0;
                            return (int) (delta / 100);
                        }));

                        m.addCustomChart(new SingleLineChart("amount_withdrawn", () -> {
                            long current = ez.getTransactionMetricsService().getTotalWithdrawnCents();
                            long last = lastSentWithdrawnCents.getAndSet(current);
                            long delta = current - last;
                            if (delta < 0) delta = 0;
                            return (int) (delta / 100);
                        }));

                        m.addCustomChart(new SingleLineChart("amount_converted", () -> {
                            long current = ez.getTransactionMetricsService().getTotalConvertedCents();
                            long last = lastSentConvertedCents.getAndSet(current);
                            long delta = current - last;
                            if (delta < 0) delta = 0;
                            return (int) (delta / 100);
                        }));
                    } catch (Throwable ignoredInner) {
                        // Ignore if casting or chart creation fails
                    }
                } catch (Throwable ignored) {
                    // Non-fatal: chart registration failed
                }
            }
        } catch (Throwable ignored) {
            m = null;
        }
        this.metrics = m;
    }

    public Metrics getMetrics() {
        return metrics;
    }
}
