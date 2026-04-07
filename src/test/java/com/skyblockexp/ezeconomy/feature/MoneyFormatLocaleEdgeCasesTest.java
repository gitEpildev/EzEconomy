package com.skyblockexp.ezeconomy.feature;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.feature.support.TestSupport;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoneyFormatLocaleEdgeCasesTest {
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
    public void testFrenchLocaleGroupingAndDecimal() throws Exception {
        Object playerObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "frPlayer");
        org.bukkit.entity.Player player = (org.bukkit.entity.Player) playerObj;

        TestSupport.MockStorage storage = new TestSupport.MockStorage();
        TestSupport.injectField(plugin, "storage", storage);

        // Use French locale (comma decimal, space thousands in many formats)
        plugin.getConfig().set("money-format.locale", "fr_FR");
        plugin.getConfig().set("money-format.pattern", "#,##0.00 ¤");
        plugin.getConfig().set("money-format.currencySymbol", "€");
        plugin.getConfig().set("money-format.symbolPlacement", "suffix");

        storage.setBalance(player.getUniqueId(), plugin.getDefaultCurrency(), 1234.5);
        player.performCommand("balance");
        String msg = ((PlayerMock) player).nextMessage();

        // Expect either "1 234,50" or "1.234,50" depending on DecimalFormat behaviour; just ensure comma decimal present
        assertTrue(msg.contains(",50") || msg.contains(",5"), "French formatted decimal should contain a comma decimal separator");
        assertTrue(msg.contains("€"), "Should include configured currency symbol");
    }

    @Test
    public void testNegativeAmountsAndRoundingBehavior() throws Exception {
        Object playerObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "negPlayer");
        org.bukkit.entity.Player player = (org.bukkit.entity.Player) playerObj;

        TestSupport.MockStorage storage = new TestSupport.MockStorage();
        TestSupport.injectField(plugin, "storage", storage);

        // configure compact formatting with 2 decimals
        plugin.getConfig().set("money-format.useCompact", true);
        plugin.getConfig().set("money-format.compact.precision", 2);

        // set negative balance and run balance
        storage.setBalance(player.getUniqueId(), plugin.getDefaultCurrency(), -1532.456);
        player.performCommand("balance");
        String msg = ((PlayerMock) player).nextMessage();

        // Should display negative sign and rounded compacted value (e.g., -1.53K)
        assertTrue(msg.contains("-") && (msg.toLowerCase().contains("k") || msg.contains("1.53") || msg.contains("1,53")), "Negative amounts should show negative sign and rounded compact form");
    }

    @Test
    public void testSymbolPlacementPrefixAndSuffixWithCompactThresholds() throws Exception {
        Object senderObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "edgeSender");
        Object recipientObj = server.getClass().getMethod("addPlayer", String.class).invoke(server, "edgeRecipient");
        org.bukkit.entity.Player sender = (org.bukkit.entity.Player) senderObj;
        org.bukkit.entity.Player recipient = (org.bukkit.entity.Player) recipientObj;

        TestSupport.MockStorage storage = new TestSupport.MockStorage();
        TestSupport.injectField(plugin, "storage", storage);

        // set compact thresholds and precision
        plugin.getConfig().set("money-format.useCompact", true);
        plugin.getConfig().set("money-format.compact.precision", 1);
        // thresholds: make million use 'M' at 1_000_000
        plugin.getConfig().set("money-format.compact.thresholds.kilo", 1000);
        plugin.getConfig().set("money-format.compact.thresholds.million", 1000000);

        // test prefix placement
        plugin.getConfig().set("money-format.currencySymbol", "PFX");
        plugin.getConfig().set("money-format.symbolPlacement", "prefix");

        storage.setBalance(sender.getUniqueId(), plugin.getDefaultCurrency(), 1_500_000);
        sender.performCommand("balance");
        String msg = ((PlayerMock) sender).nextMessage();
        assertTrue(msg.contains("PFX") && (msg.toLowerCase().contains("1.5m") || msg.contains("1.5")), "Prefix symbol should appear with compact M suffix");

        // test suffix placement
        plugin.getConfig().set("money-format.currencySymbol", "SFX");
        plugin.getConfig().set("money-format.symbolPlacement", "suffix");

        storage.setBalance(recipient.getUniqueId(), plugin.getDefaultCurrency(), 2500);
        recipient.performCommand("balance");
        String rmsg = ((PlayerMock) recipient).nextMessage();
        assertTrue(rmsg.contains("SFX") && (rmsg.toLowerCase().contains("2.5k") || rmsg.contains("2.5")), "Suffix symbol should appear with compact K suffix");
    }
}
