package com.skyblockexp.ezeconomy.bungeecord.proxy;

import net.md_5.bungee.api.plugin.Plugin;
import java.io.File;

public class EzBungeeProxyPlugin extends Plugin {
    private EzBungeeProxy proxyLogic;

    @Override
    public void onEnable() {
        // Initialize bStats (plugin id 30431)
        try {
            org.bstats.bungeecord.Metrics metrics = new org.bstats.bungeecord.Metrics(this, 30431);

            // Add non-sensitive custom charts:
            //  - whether a shared-secret is present in the proxy config (yes/no)
            //  - whether the cleanup interval is the default or customized (default/custom)
            File cfg = new File(getDataFolder(), "bungeecord.yml");
            final boolean[] hasSecret = {false};
            final long[] cleanupInterval = {5000L};
            if (cfg != null && cfg.exists()) {
                try {
                    java.util.List<String> lines = java.nio.file.Files.readAllLines(cfg.toPath());
                    for (String raw : lines) {
                        String line = raw.trim();
                        if (line.startsWith("shared-secret:")) {
                            String val = line.substring("shared-secret:".length()).trim();
                            if (val.startsWith("\"") && val.endsWith("\"")) val = val.substring(1, val.length()-1);
                            if (val.length() > 0) hasSecret[0] = true;
                        } else if (line.startsWith("cleanup-interval-ms:")) {
                            String num = line.substring("cleanup-interval-ms:".length()).trim();
                            try { cleanupInterval[0] = Long.parseLong(num); } catch (Exception ignored) {}
                        }
                    }
                } catch (Exception ignored) {}
            }

            final String secretChart = hasSecret[0] ? "yes" : "no";
            final String cleanupChart = cleanupInterval[0] > 5000L ? "custom" : "default";
            try {
                metrics.addCustomChart(new org.bstats.bungeecord.SimplePie("has_shared_secret", () -> secretChart));
                metrics.addCustomChart(new org.bstats.bungeecord.SimplePie("cleanup_interval_type", () -> cleanupChart));
            } catch (Throwable ignored) {}
        } catch (Throwable t) {
            // best-effort; don't block startup
        }

        // Load proxy configuration from data folder
        File cfg = new File(getDataFolder(), "bungeecord.yml");
        this.proxyLogic = BungeeAdapterPlugin.loadProxyFromConfig(cfg);
    }

    @Override
    public void onDisable() {
        try { if (this.proxyLogic != null) this.proxyLogic.close(); } catch (Exception ignored) {}
    }

    public EzBungeeProxy getProxyLogic() { return proxyLogic; }
}
