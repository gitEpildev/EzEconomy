package com.gitepildev.giteconomy.service;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.gitepildev.giteconomy.core.GitEconomyPlugin;
import com.gitepildev.giteconomy.api.storage.StorageProvider;
import com.gitepildev.giteconomy.storage.TransferResult;
import com.gitepildev.giteconomy.service.format.CurrencyFormatter;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class PaymentExecutorIntegrationTest {

    @BeforeEach
    void setup() {
        MockBukkit.mock();
    }

    @AfterEach
    void teardown() {
        MockBukkit.unmock();
    }

    @Test
    void execute_simpleTransfer_callsStorageTransfer() {
        GitEconomyPlugin plugin = mock(GitEconomyPlugin.class);
        StorageProvider storage = mock(StorageProvider.class);
        when(plugin.getStorageOrWarn()).thenReturn(storage);

        CurrencyFormatter fmt = mock(CurrencyFormatter.class);
        when(fmt.formatPriceForMessage(anyDouble(), anyString())).thenReturn("10$");
        when(plugin.getCurrencyFormatter()).thenReturn(fmt);
        when(plugin.getDefaultCurrency()).thenReturn("dollar");

        // No recipient currency preference
        var pref = mock(com.gitepildev.giteconomy.manager.CurrencyPreferenceManager.class);
        when(pref.getPreferredCurrency(any())).thenReturn(null);
        when(plugin.getCurrencyPreferenceManager()).thenReturn(pref);

        PlayerMock from = MockBukkit.createPlayer("alice");
        PlayerMock to = MockBukkit.createPlayer("bob");

        when(storage.transfer(from.getUniqueId(), to.getUniqueId(), "dollar", 10.0)).thenReturn(TransferResult.success(0.0, 10.0));

        boolean res = PaymentExecutor.execute(plugin, from, "bob", BigDecimal.valueOf(10), "dollar");
        assertTrue(res);
        verify(storage).transfer(from.getUniqueId(), to.getUniqueId(), "dollar", 10.0);
    }
}
