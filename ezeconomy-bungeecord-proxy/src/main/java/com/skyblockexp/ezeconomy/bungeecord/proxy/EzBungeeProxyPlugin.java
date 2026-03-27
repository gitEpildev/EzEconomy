package com.skyblockexp.ezeconomy.bungeecord.proxy;

import net.md_5.bungee.api.plugin.Plugin;
import java.io.File;

public class EzBungeeProxyPlugin extends Plugin {
    private EzBungeeProxy proxyLogic;

    @Override
    public void onEnable() {
        // Initialize bStats (plugin id 30431)
        try {
            new org.bstats.bungeecord.Metrics(this, 30431);
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
