package com.skyblockexp.ezeconomy.core;

import org.bstats.bukkit.Metrics;

public class EzEconomyMetrics {
    private final Metrics metrics;

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
                    m.addCustomChart(new Metrics.SimplePie("caching_strategy", () -> {
                        String v = plugin.getConfig().getString("caching-strategy");
                        if (v == null || v.isBlank()) {
                            v = plugin.getConfig().getString("locking-strategy", "unknown");
                        }
                        return v == null ? "unknown" : v;
                    }));

                    m.addCustomChart(new Metrics.SimplePie("locking_strategy", () -> {
                        String v = plugin.getConfig().getString("locking-strategy", "unknown");
                        return v == null ? "unknown" : v;
                    }));

                    // Numeric single-line charts: report whole-unit totals (cents/100)
                    try {
                        EzEconomyPlugin ez = (EzEconomyPlugin) plugin;
                        m.addCustomChart(new Metrics.SingleLineChart("amount_deposited", () -> (int) (ez.getTotalDepositedCents() / 100)));
                        m.addCustomChart(new Metrics.SingleLineChart("amount_withdrawn", () -> (int) (ez.getTotalWithdrawnCents() / 100)));
                        m.addCustomChart(new Metrics.SingleLineChart("amount_converted", () -> (int) (ez.getTotalConvertedCents() / 100)));
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
