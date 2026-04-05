package com.skyblockexp.ezeconomy.service;

import com.skyblockexp.ezeconomy.api.storage.StorageProvider;
import com.skyblockexp.ezeconomy.api.storage.models.Transaction;
import com.skyblockexp.ezeconomy.storage.TransferResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerEconomyServiceTest {

    @Mock
    StorageProvider storage;

    @InjectMocks
    PlayerEconomyService service;

    @Test
    void getBalance_delegatesToStorage() {
        UUID id = UUID.randomUUID();
        when(storage.getBalance(id, "dollar")).thenReturn(123.45);
        double bal = service.getBalance(id, "dollar");
        assertEquals(123.45, bal, 1e-9);
    }

    @Test
    void deposit_alwaysTrue_andCallsStorage() {
        UUID id = UUID.randomUUID();
        boolean ok = service.deposit(id, "dollar", 10.0);
        assertTrue(ok);
        verify(storage).deposit(id, "dollar", 10.0);
    }

    @Test
    void withdraw_delegates_tryWithdrawReturn() {
        UUID id = UUID.randomUUID();
        when(storage.tryWithdraw(id, "dollar", 5.0)).thenReturn(true);
        assertTrue(service.withdraw(id, "dollar", 5.0));
        when(storage.tryWithdraw(id, "dollar", 6.0)).thenReturn(false);
        assertFalse(service.withdraw(id, "dollar", 6.0));
    }

    @Test
    void getTransactions_andGetAllBalances_delegates() {
        UUID id = UUID.randomUUID();
        List<Transaction> txs = List.of();
        when(storage.getTransactions(id, "dollar")).thenReturn(txs);
        assertSame(txs, service.getTransactions(id, "dollar"));

        Map<UUID, Double> all = Map.of(id, 50.0);
        when(storage.getAllBalances("dollar")).thenReturn(all);
        assertSame(all, service.getAllBalances("dollar"));
    }

    @Test
    void transfer_variants_delegateToStorage() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        TransferResult r1 = new TransferResult(true, 0.0, 100.0);
        when(storage.transfer(a, b, "dollar", 10.0)).thenReturn(r1);
        assertSame(r1, service.transfer(a, b, "dollar", 10.0));

        TransferResult r2 = new TransferResult(true, 0.0, 100.0);
        when(storage.transfer(a, b, "dollar", 10.0, 9.0)).thenReturn(r2);
        assertSame(r2, service.transfer(a, b, "dollar", 10.0, 9.0));
    }
}
