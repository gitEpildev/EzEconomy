package com.skyblockexp.ezeconomy.feature;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.feature.support.TestSupport;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoneyFormatMessagingTest {
    private Object server;
    private EzEconomyPlugin plugin;

    @BeforeEach
    public void setup() {
        server = TestSupport.setupMockServer();
        plugin = TestSupport.loadPlugin(server);
        plugin.loadMessageProvider();
    }

    @AfterEach
    public void teardown() {
        TestSupport.tearDown();
    }

    @Test
    public void testPayCommand_compactFormattingInMessages() throws Exception {
        Object senderObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "payerFmt");
        Object recipientObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "payeeFmt");
        org.bukkit.entity.Player sender = (org.bukkit.entity.Player) senderObj;
        org.bukkit.entity.Player recipient = (org.bukkit.entity.Player) recipientObj;

        // inject lightweight storage
        TestSupport.MockStorage storage = new TestSupport.MockStorage();
        TestSupport.injectField(plugin, "storage", storage);

        // configure legacy compact formatting to ensure compact output is used
        plugin.getConfig().set("money-format.useCompact", true);
        plugin.getConfig().set("money-format.compact.precision", 1);

        // set balances
        storage.setBalance(sender.getUniqueId(), plugin.getDefaultCurrency(), 2000.0);

        // perform a pay that will produce a compact amount in messages (e.g., 1500 -> 1.5K)
        sender.performCommand("pay payeeFmt 1500");

        // sender message (confirmation)
        String sentMsg = ((PlayerMock) sender).nextMessage();
        // recipient message (received)
        String recMsg = ((PlayerMock) recipient).nextMessage();

        // Accept either K or k and allow possible currency symbol; verify compact-ish text present
        assertTrue(sentMsg.toLowerCase().contains("1.5k") || sentMsg.contains("1.5"), "Sender confirmation should show compact amount");
        assertTrue(recMsg.toLowerCase().contains("1.5k") || recMsg.contains("1.5"), "Recipient notification should show compact amount");
    }

    @Test
    public void testBalanceAndPay_respectPatternAndSymbolPlacement() throws Exception {
        Object senderObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "aliceFmt");
        org.bukkit.entity.Player sender = (org.bukkit.entity.Player) senderObj;

        TestSupport.MockStorage storage = new TestSupport.MockStorage();
        TestSupport.injectField(plugin, "storage", storage);

        // configure a simple currency symbol and a pattern via legacy keys
        plugin.getConfig().set("money-format.pattern", "¤#,##0.00");
        plugin.getConfig().set("money-format.currencySymbol", "EZ");
        plugin.getConfig().set("money-format.symbolPlacement", "prefix");

        // set a balance
        storage.setBalance(sender.getUniqueId(), plugin.getDefaultCurrency(), 1234.56);

        // run balance command and assert the returned message contains the configured symbol and amount
        sender.performCommand("balance");
        String msg = ((PlayerMock) sender).nextMessage();

        assertTrue(msg.contains("EZ") && (msg.contains("1234") || msg.contains("1,234") || msg.contains("1234.56")), "Balance message should include configured symbol and amount");

        // also verify that a pay confirmation uses the same symbol/pattern
        // create recipient
        Object recipientObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "bobFmt");
        org.bukkit.entity.Player recipient = (org.bukkit.entity.Player) recipientObj;
        storage.setBalance(sender.getUniqueId(), plugin.getDefaultCurrency(), 500.0);

        sender.performCommand("pay bobFmt 12.34");
        String sMsg = ((PlayerMock) sender).nextMessage();
        String rMsg = ((PlayerMock) recipient).nextMessage();

        assertTrue(sMsg.contains("EZ") && rMsg.contains("EZ"), "Both pay messages should include configured currency symbol");
    }
}
