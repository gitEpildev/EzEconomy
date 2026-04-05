package com.skyblockexp.ezeconomy.core;

import com.skyblockexp.ezeconomy.storage.StorageProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class VaultEconomyImplTest {

    @Mock
    EzEconomyPlugin plugin;

    @Mock
    StorageProvider storage;

    @InjectMocks
    VaultEconomyImpl vault;

    @Test
    void testDeposit_increasesPlayerBalance() {
        java.util.UUID id = java.util.UUID.randomUUID();
        org.bukkit.OfflinePlayer offline = org.mockito.Mockito.mock(org.bukkit.OfflinePlayer.class);
        org.mockito.Mockito.when(offline.getUniqueId()).thenReturn(id);

        org.bukkit.Server server = org.mockito.Mockito.mock(org.bukkit.Server.class);
        org.mockito.Mockito.when(plugin.getServer()).thenReturn(server);
        org.mockito.Mockito.when(server.getOfflinePlayer(org.mockito.Mockito.anyString())).thenReturn(offline);
        org.mockito.Mockito.when(plugin.getDefaultCurrency()).thenReturn("dollar");

        // Ensure storage.deposit is invoked and getBalance returns expected value
        org.mockito.Mockito.doAnswer(invocation -> {
            // no-op
            return null;
        }).when(storage).deposit(org.mockito.Mockito.eq(id), org.mockito.Mockito.eq("dollar"), org.mockito.Mockito.eq(25.0));
        org.mockito.Mockito.when(storage.getBalance(org.mockito.Mockito.eq(id), org.mockito.Mockito.eq("dollar"))).thenReturn(25.0);

        var res = vault.depositPlayer(offline, 25.0);
        assertEquals(net.milkbowl.vault.economy.EconomyResponse.ResponseType.SUCCESS, res.type);
        org.mockito.Mockito.verify(storage).deposit(id, "dollar", 25.0);
    }

    @Test
    void testWithdraw_insufficientFunds_returnsFailure() {
        java.util.UUID id = java.util.UUID.randomUUID();
        org.bukkit.OfflinePlayer offline = org.mockito.Mockito.mock(org.bukkit.OfflinePlayer.class);
        org.mockito.Mockito.when(offline.getUniqueId()).thenReturn(id);

        org.bukkit.Server server = org.mockito.Mockito.mock(org.bukkit.Server.class);
        org.mockito.Mockito.when(plugin.getServer()).thenReturn(server);
        org.mockito.Mockito.when(server.getOfflinePlayer(org.mockito.Mockito.anyString())).thenReturn(offline);
        org.mockito.Mockito.when(plugin.getDefaultCurrency()).thenReturn("dollar");

        org.mockito.Mockito.when(storage.tryWithdraw(org.mockito.Mockito.eq(id), org.mockito.Mockito.eq("dollar"), org.mockito.Mockito.eq(100.0))).thenReturn(false);
        org.mockito.Mockito.when(storage.getBalance(org.mockito.Mockito.eq(id), org.mockito.Mockito.eq("dollar"))).thenReturn(10.0);

        var res = vault.withdrawPlayer(offline, 100.0);
        assertEquals(net.milkbowl.vault.economy.EconomyResponse.ResponseType.FAILURE, res.type);
        assertEquals("Insufficient funds", res.errorMessage);
    }
}
