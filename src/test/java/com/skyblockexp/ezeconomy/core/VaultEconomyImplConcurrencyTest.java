package com.skyblockexp.ezeconomy.core;

import com.skyblockexp.ezeconomy.feature.support.TestSupport;
import com.skyblockexp.ezeconomy.lock.LocalLockManager;
import net.milkbowl.vault.economy.EconomyResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.MockBukkit;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VaultEconomyImplConcurrencyTest {
    private Object server;
    private EzEconomyPlugin plugin;

    @BeforeEach
    void setup() {
        server = MockBukkit.mock();
        plugin = TestSupport.loadPlugin(server);
    }

    @AfterEach
    void teardown() {
        MockBukkit.unmock();
    }

    @Test
    void withdrawPlayer_serializesConcurrentDebitAttempts() throws Exception {
        UUID playerId = UUID.randomUUID();
        RaceyStorage storage = new RaceyStorage();
        storage.setBalance(playerId, "dollar", 100.0);
        TestSupport.injectField(plugin, "storage", storage);
        plugin.setLockManager(new LocalLockManager());

        VaultEconomyImpl economy = new VaultEconomyImpl(plugin);
        CountDownLatch startLatch = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            Future<EconomyResponse> first = pool.submit(() -> {
                startLatch.await(3, TimeUnit.SECONDS);
                return economy.withdrawPlayer(plugin.getServer().getOfflinePlayer(playerId), 100.0);
            });
            Future<EconomyResponse> second = pool.submit(() -> {
                startLatch.await(3, TimeUnit.SECONDS);
                return economy.withdrawPlayer(plugin.getServer().getOfflinePlayer(playerId), 100.0);
            });
            startLatch.countDown();

            EconomyResponse r1 = first.get(3, TimeUnit.SECONDS);
            EconomyResponse r2 = second.get(3, TimeUnit.SECONDS);
            int successCount = (r1.type == EconomyResponse.ResponseType.SUCCESS ? 1 : 0)
                + (r2.type == EconomyResponse.ResponseType.SUCCESS ? 1 : 0);

            assertEquals(1, successCount, "exactly one withdraw should succeed under contention");
            assertEquals(0.0, storage.getBalance(playerId, "dollar"), 0.0001, "balance must not go negative or be double-spent");
        } finally {
            pool.shutdownNow();
        }
    }

    private static final class RaceyStorage extends TestSupport.MockStorage {
        @Override
        public boolean tryWithdraw(UUID uuid, String currency, double amount) {
            double bal = getBalance(uuid, currency);
            if (bal < amount) {
                return false;
            }
            try {
                Thread.sleep(25L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
            setBalance(uuid, currency, bal - amount);
            return true;
        }
    }
}
