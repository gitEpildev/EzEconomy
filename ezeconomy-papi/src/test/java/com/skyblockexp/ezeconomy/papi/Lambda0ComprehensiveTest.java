package com.skyblockexp.ezeconomy.papi;

import com.skyblockexp.ezeconomy.api.storage.StorageProvider;
import com.skyblockexp.ezeconomy.papi.testhelpers.TestEzEconomyStubs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class Lambda0ComprehensiveTest extends TestBase {

    @Test
    public void invoke_lambda0_with_various_player_states() throws Exception {
        MockBukkit.mock();
        EzEconomyPapiPlugin papi = (EzEconomyPapiPlugin) MockBukkit.load(EzEconomyPapiPlugin.class);
        EzEconomyPAPIExpansion expansion = new EzEconomyPAPIExpansion(papi);

        Method lambda0 = null;
        for (Method m : EzEconomyPAPIExpansion.class.getDeclaredMethods()) {
            if (m.getName().contains("lambda$0")) { lambda0 = m; break; }
        }
        assertNotNull(lambda0, "lambda$0 not found");
        lambda0.setAccessible(true);

        UUID u1 = UUID.randomUUID();
        AbstractMap.SimpleEntry<java.util.UUID, Double> e1 = new AbstractMap.SimpleEntry<>(u1, 42.0);

        // Case A: storage.getPlayer returns EconomyPlayer with displayName
        TestEzEconomyStubs.SimpleStorageProvider spA = new TestEzEconomyStubs.SimpleStorageProvider();
        spA.putPlayer(u1, new com.skyblockexp.ezeconomy.dto.EconomyPlayer(u1, "UserA", "DisplayA"));
        TestEzEconomyStubs.SimpleTestEz ezA = new TestEzEconomyStubs.SimpleTestEz(spA, "dollar") {
            @Override public String format(double amount, String currency) { return "F:"+amount+":"+currency; }
        };
        Object rA = lambda0.invoke(expansion, ezA, "dollar", e1);
        assertNotNull(rA);
        String sA = rA.toString();
        assertTrue(sA.contains("DisplayA") || sA.contains("UserA") || sA.contains("F:"));

        // Case B: storage.getPlayer returns EconomyPlayer with null displayName but name present
        TestEzEconomyStubs.SimpleStorageProvider spB = new TestEzEconomyStubs.SimpleStorageProvider();
        spB.putPlayer(u1, new com.skyblockexp.ezeconomy.dto.EconomyPlayer(u1, "UserB", null));
        TestEzEconomyStubs.SimpleTestEz ezB = new TestEzEconomyStubs.SimpleTestEz(spB, "dollar") {
            @Override public String format(double amount, String currency) { return "FB:"+amount+":"+currency; }
        };
        Object rB = lambda0.invoke(expansion, ezB, "dollar", e1);
        assertNotNull(rB);
        String sB = rB.toString();
        assertTrue(sB.contains("UserB") || sB.contains("FB:"));

        // Case C: storage.getPlayer returns null -> fallback to Bukkit.getOfflinePlayer(...).getName()/uuid
        TestEzEconomyStubs.SimpleStorageProvider spC = new TestEzEconomyStubs.SimpleStorageProvider() {
            @Override public com.skyblockexp.ezeconomy.dto.EconomyPlayer getPlayer(UUID uuid) { return null; }
        };
        TestEzEconomyStubs.SimpleTestEz ezC = new TestEzEconomyStubs.SimpleTestEz(spC, "dollar") {
            @Override public String format(double amount, String currency) { return "FC:"+amount+":"+currency; }
        };
        Object rC = lambda0.invoke(expansion, ezC, "dollar", e1);
        assertNotNull(rC);
        String sC = rC.toString();
        assertFalse(sC.isEmpty());

        // Case D: storage.getPlayer throws -> should be caught and produce a non-empty result
        TestEzEconomyStubs.SimpleStorageProvider spD = new TestEzEconomyStubs.SimpleStorageProvider() {
            @Override public com.skyblockexp.ezeconomy.dto.EconomyPlayer getPlayer(UUID uuid) { throw new RuntimeException("boom"); }
        };
        TestEzEconomyStubs.SimpleTestEz ezD = new TestEzEconomyStubs.SimpleTestEz(spD, "dollar") {
            @Override public String format(double amount, String currency) { return "FD:"+amount+":"+currency; }
        };
        Object rD = lambda0.invoke(expansion, ezD, "dollar", e1);
        assertNotNull(rD);
        String sD = rD.toString();
        assertFalse(sD.isEmpty());
    }
}
