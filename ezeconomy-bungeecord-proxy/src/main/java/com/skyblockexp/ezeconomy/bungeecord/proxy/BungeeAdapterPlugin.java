package com.skyblockexp.ezeconomy.bungeecord.proxy;

/**
 * Optional Bungee adapter stub. The real adapter that integrates with Bungee's
 * plugin API can be provided separately; this class is a reflection-friendly
 * stub that avoids hard compile-time dependency on the Bungee API so the proxy
 * module remains buildable in environments without the Bungee snapshot.
 */
public class BungeeAdapterPlugin {
    private final EzBungeeProxy proxyLogic;
    // temporary holder used when parsing config externally
    private String _tmpSecret = "";

    public BungeeAdapterPlugin() {
        this.proxyLogic = new EzBungeeProxy(_tmpSecret);
    }

    /**
     * Create an `EzBungeeProxy` configured from a `bungeecord.yml` file. The
     * file is expected to contain `shared-secret: "..."` and optionally
     * `cleanup-interval-ms: <number>` entries. This factory avoids introducing
     * a hard dependency on Bungee APIs so consumers can load configuration
     * from disk and pass settings to their runtime adapter.
     */
    public static EzBungeeProxy loadProxyFromConfig(java.io.File configFile) {
        final java.util.concurrent.atomic.AtomicReference<String> secretRef = new java.util.concurrent.atomic.AtomicReference<>("");
        final java.util.concurrent.atomic.AtomicLong cleanupRef = new java.util.concurrent.atomic.AtomicLong(5000L);
        if (configFile != null && configFile.exists()) {
            try {
                java.nio.file.Files.lines(configFile.toPath()).forEach(line -> {
                    String l = line.trim();
                    if (l.startsWith("shared-secret:")) {
                        String val = l.substring("shared-secret:".length()).trim();
                        if (val.startsWith("\"") && val.endsWith("\"")) val = val.substring(1, val.length()-1);
                        secretRef.set(val);
                    } else if (l.startsWith("cleanup-interval-ms:")) {
                        String num = l.substring("cleanup-interval-ms:".length()).trim();
                        try { cleanupRef.set(Long.parseLong(num)); } catch (Exception ignored) {}
                    }
                });
            } catch (Exception ignored) {}
        }
        return new EzBungeeProxy(secretRef.get(), cleanupRef.get());
    }

    /**
     * Attempt to enable an adapter via reflection if the Bungee API is present.
     * This method is intentionally best-effort and will no-op when Bungee is
     * unavailable (so the module can be used as a plain Java library).
     */
    public void tryEnable() {
        try {
            Class.forName("net.md_5.bungee.api.ProxyServer");
            // If present, an external build that includes Bungee can provide
            // a concrete adapter that wires events to `proxyLogic`.
        } catch (ClassNotFoundException ignored) {
            // Bungee API not available; nothing to do.
        }
    }

    public EzBungeeProxy getProxyLogic() { return proxyLogic; }
}
