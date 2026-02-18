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
        } catch (Throwable ignored) {
            m = null;
        }
        this.metrics = m;
    }

    public Metrics getMetrics() {
        return metrics;
    }
}
