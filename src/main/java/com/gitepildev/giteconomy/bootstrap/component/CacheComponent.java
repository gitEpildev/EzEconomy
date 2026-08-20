package com.gitepildev.giteconomy.bootstrap.component;

import com.gitepildev.giteconomy.bootstrap.BootstrapComponent;
import com.gitepildev.giteconomy.core.GitEconomyPlugin;
import com.gitepildev.giteconomy.cache.CacheManager;
import com.gitepildev.giteconomy.cache.CachingStrategy;

/**
 * Bootstrap component that configures the global cache strategy early during startup.
 */
public class CacheComponent implements BootstrapComponent {
    private final GitEconomyPlugin plugin;

    public CacheComponent(GitEconomyPlugin plugin) { this.plugin = plugin; }

    @Override
    public void start() {
        String caching = plugin.getConfig().getString("caching-strategy", plugin.getConfig().getString("locking-strategy", "LOCAL")).toUpperCase();
        try {
            CacheManager.setStrategy(CachingStrategy.valueOf(caching));
            plugin.getLogger().info("Cache strategy set to " + caching + " (provider=" + CacheManager.getProviderClassName() + ")");
        } catch (IllegalArgumentException ia) {
            CacheManager.setStrategy(CachingStrategy.LOCAL);
            plugin.getLogger().warning("Unknown caching-strategy '" + caching + "', defaulting to LOCAL (provider=" + CacheManager.getProviderClassName() + ")");
        }
    }

    @Override
    public void stop() {
        // nothing to stop for cache manager
    }

    @Override
    public void reload() {
        stop(); start();
    }
}
