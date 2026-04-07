package com.skyblockexp.ezeconomy.papi.testhelpers;

import org.bukkit.OfflinePlayer;

import java.lang.reflect.Proxy;
import java.util.UUID;

public final class TestPlayerFakes {

    private TestPlayerFakes() {}

    public static OfflinePlayer fakeOfflinePlayer() {
        return fakeOfflinePlayer(UUID.randomUUID());
    }

    public static OfflinePlayer fakeOfflinePlayer(UUID id) {
        return (OfflinePlayer) Proxy.newProxyInstance(
                OfflinePlayer.class.getClassLoader(),
                new Class[]{OfflinePlayer.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("getUniqueId".equals(name)) return id;
                    if ("getName".equals(name)) return "FakePlayer-" + id.toString().substring(0, 8);
                    if (method.getReturnType().equals(boolean.class)) return false;
                    return null;
                }
        );
    }

    public static org.bukkit.entity.Player fakePlayer(UUID id) {
        return (org.bukkit.entity.Player) Proxy.newProxyInstance(
                org.bukkit.entity.Player.class.getClassLoader(),
                new Class[]{org.bukkit.entity.Player.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("getUniqueId".equals(name)) return id;
                    if ("getName".equals(name)) return "FakePlayer-" + id.toString().substring(0, 8);
                    Class<?> r = method.getReturnType();
                    if (r.equals(boolean.class)) return false;
                    if (r.equals(int.class)) return 0;
                    if (r.equals(long.class)) return 0L;
                    return null;
                }
        );
    }
}
