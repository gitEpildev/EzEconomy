package com.skyblockexp.ezeconomy.core;

import org.bukkit.plugin.java.JavaPlugin;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple typed registry for plugin managers and services.
 */
public final class Registry {
    private static EzEconomyPlugin plugin;
    private static final Map<Class<?>, Object> services = new ConcurrentHashMap<>();

    private Registry() {}

    public static void initialize(EzEconomyPlugin plugin) {
        Registry.plugin = plugin;
        services.clear();
    }

    public static <T> void register(Class<T> key, T value) {
        if (key == null || value == null) return;
        services.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> key) {
        Object o = services.get(key);
        if (o == null) return null;
        return (T) o;
    }

    public static EzEconomyPlugin getPlugin() {
        return plugin;
    }

    public static Map<Class<?>, Object> getAll() {
        return services;
    }

    public static void initAll() {
        for (Object o : services.values()) {
            if (o == null) continue;
            try {
                if (o instanceof Manager) {
                    ((Manager) o).init();
                    continue;
                }
                tryInvokeNoArg(o, "init", "load");
            } catch (Exception ignored) {
            }
        }
    }

    public static void shutdownAll() {
        for (Object o : services.values()) {
            if (o == null) continue;
            try {
                if (o instanceof Manager) {
                    ((Manager) o).shutdown();
                    continue;
                }
                tryInvokeNoArg(o, "shutdown", "stop", "close");
            } catch (Exception ignored) {
            }
        }
    }

    private static void tryInvokeNoArg(Object o, String... names) {
        for (String n : names) {
            try {
                Method m = o.getClass().getMethod(n);
                if (m != null) {
                    m.setAccessible(true);
                    m.invoke(o);
                    return;
                }
            } catch (NoSuchMethodException e) {
                // try next
            } catch (Exception ignored) {
                return;
            }
        }
    }
}
