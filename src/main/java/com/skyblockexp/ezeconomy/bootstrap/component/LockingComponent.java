package com.skyblockexp.ezeconomy.bootstrap.component;

import com.skyblockexp.ezeconomy.bootstrap.BootstrapComponent;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.lock.LockManager;
import com.skyblockexp.ezeconomy.lock.LocalLockManager;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ServiceLoader;
import java.util.Iterator;
import java.io.IOException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;

public class LockingComponent implements BootstrapComponent {
    private final EzEconomyPlugin plugin;
    private LockManager manager;

    public LockingComponent(EzEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        FileConfiguration cfg = plugin.getConfig();
        String strategy = cfg.getString("locking-strategy", "LOCAL").toUpperCase();
        // Load redis.yml from data folder (save default resource if missing)
        File redisFile = new File(plugin.getDataFolder(), "redis.yml");
        if (!redisFile.exists()) {
            try { plugin.saveResource("redis.yml", false); } catch (Exception ignored) {}
        }
        FileConfiguration redisCfg = YamlConfiguration.loadConfiguration(redisFile);

        if ("REDIS".equals(strategy) && redisCfg.getBoolean("enabled", false)) {
            String host = redisCfg.getString("host", cfg.getString("redis.host", "localhost"));
            int port = redisCfg.getInt("port", cfg.getInt("redis.port", 6379));
            String password = redisCfg.getString("password", cfg.getString("redis.password", ""));
            int database = redisCfg.getInt("database", cfg.getInt("redis.database", 0));
            boolean fallback = redisCfg.getBoolean("fallback-to-local", cfg.getBoolean("redis.fallback-to-local", true));
            try {
                // 1) Try ServiceLoader using current classloader (useful for development/classpath-loaded providers)
                ServiceLoader<LockManager> loader = ServiceLoader.load(LockManager.class);
                Iterator<LockManager> it = loader.iterator();
                if (it.hasNext()) {
                    this.manager = it.next();
                    plugin.getLogger().info("Loaded LockManager via ServiceLoader: " + this.manager.getClass().getName());
                }

                // 2) If not found, try loading jars from plugin data 'libs' directory
                if (this.manager == null) {
                    File libs = new File(plugin.getDataFolder(), "libs");
                    if (libs.exists() && libs.isDirectory()) {
                        File[] jars = libs.listFiles((d, name) -> name.endsWith(".jar"));
                        if (jars != null) {
                            for (File jar : jars) {
                                try (URLClassLoader cl = new URLClassLoader(new URL[]{jar.toURI().toURL()}, this.getClass().getClassLoader())) {
                                    ServiceLoader<LockManager> sl = ServiceLoader.load(LockManager.class, cl);
                                    Iterator<LockManager> sit = sl.iterator();
                                    if (sit.hasNext()) {
                                        this.manager = sit.next();
                                        plugin.getLogger().info("Loaded LockManager from " + jar.getName() + ": " + this.manager.getClass().getName());
                                        break;
                                    }
                                } catch (IOException e) {
                                    plugin.getLogger().warning("Failed to load extension jar " + jar.getName() + ": " + e.getMessage());
                                }
                            }
                        }
                    }
                }

                // 3) If still not found, attempt reflection instantiation (for cases where class is on classpath)
                if (this.manager == null) {
                    try {
                        Class<?> clazz = Class.forName("com.skyblockexp.ezeconomy.lock.RedisLockManager");
                        java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(String.class, int.class, String.class, int.class);
                        Object inst = ctor.newInstance(host, port, password, database);
                        this.manager = (LockManager) inst;
                        plugin.getLogger().info("Initialized RedisLockManager via reflection: " + this.manager.getClass().getName());
                    } catch (ClassNotFoundException cnf) {
                        // no-op
                    }
                }

                if (this.manager == null) throw new RuntimeException("No Redis LockManager provider found");
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to initialize RedisLockManager: " + ex.getMessage());
                if (fallback) {
                    this.manager = new LocalLockManager();
                    plugin.getLogger().info("Falling back to LocalLockManager.");
                } else {
                    throw new RuntimeException("Redis locking initialization failed and fallback disabled", ex);
                }
            }
        } else {
            this.manager = new LocalLockManager();
            plugin.getLogger().info("Using LocalLockManager for balance locking.");
        }
        plugin.setLockManager(this.manager);
    }

    @Override
    public void stop() {
        plugin.setLockManager(null);
        if (this.manager instanceof AutoCloseable) {
            try { ((AutoCloseable) this.manager).close(); } catch (Exception ignored) {}
        }
    }

    @Override
    public void reload() {
        stop();
        start();
    }
}
