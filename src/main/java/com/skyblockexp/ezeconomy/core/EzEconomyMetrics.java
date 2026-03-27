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
