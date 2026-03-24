package com.skyblockexp.ezeconomy.update;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

public class SpigotUpdateCheckerTest {

    private SpigotUpdateChecker createChecker() {
        return new SpigotUpdateChecker(null, 0);
    }

    private Object invoke(String name, Class<?>[] paramTypes, Object[] args) throws Exception {
        Method m = SpigotUpdateChecker.class.getDeclaredMethod(name, paramTypes);
        m.setAccessible(true);
        return m.invoke(createChecker(), args);
    }

    @Test
    void testIsStableVersion() throws Exception {
        assertTrue((Boolean) invoke("isStableVersion", new Class<?>[]{String.class}, new Object[]{"1"}));
        assertTrue((Boolean) invoke("isStableVersion", new Class<?>[]{String.class}, new Object[]{"1.2.3"}));
        assertFalse((Boolean) invoke("isStableVersion", new Class<?>[]{String.class}, new Object[]{"1.2.3-RC1"}));
        assertFalse((Boolean) invoke("isStableVersion", new Class<?>[]{String.class}, new Object[]{"2.0-SNAPSHOT"}));
        assertFalse((Boolean) invoke("isStableVersion", new Class<?>[]{String.class}, new Object[]{"beta"}));
    }

    @Test
    void testIsNewerSimple() throws Exception {
        assertTrue((Boolean) invoke("isNewer", new Class<?>[]{String.class, String.class}, new Object[]{"1.2.3", "1.2.2"}));
        assertTrue((Boolean) invoke("isNewer", new Class<?>[]{String.class, String.class}, new Object[]{"1.2.10", "1.2.9"}));
        assertFalse((Boolean) invoke("isNewer", new Class<?>[]{String.class, String.class}, new Object[]{"1.2.0", "1.2.0"}));
        assertFalse((Boolean) invoke("isNewer", new Class<?>[]{String.class, String.class}, new Object[]{"1.2.0", "1.2.1"}));
        assertTrue((Boolean) invoke("isNewer", new Class<?>[]{String.class, String.class}, new Object[]{"2.0", "1.9.9"}));
    }

    @Test
    void testIsNewerDifferentLengths() throws Exception {
        assertTrue((Boolean) invoke("isNewer", new Class<?>[]{String.class, String.class}, new Object[]{"1.3", "1.2.9"}));
        assertFalse((Boolean) invoke("isNewer", new Class<?>[]{String.class, String.class}, new Object[]{"1.3.0", "1.3"}));
        assertFalse((Boolean) invoke("isNewer", new Class<?>[]{String.class, String.class}, new Object[]{"1.2", "1.2.0"}));
    }

    @Test
    void testIsNewerWithNonNumericParts() throws Exception {
        // parsePart treats non-numeric parts as 0; ensure comparison still behaves predictably
        assertFalse((Boolean) invoke("isNewer", new Class<?>[]{String.class, String.class}, new Object[]{"1.a.0", "1.0.0"}));
        assertTrue((Boolean) invoke("isNewer", new Class<?>[]{String.class, String.class}, new Object[]{"1.0.1", "1.0.a"}));
    }
}
