package com.skyblockexp.ezeconomy.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.Bukkit;
import java.util.UUID;
import org.bukkit.ChatColor;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.core.Registry;
import com.skyblockexp.ezeconomy.manager.CurrencyManager;
import com.skyblockexp.ezeconomy.util.NumberUtil;

public class GuiPlayerChatListener implements Listener {
    private final EzEconomyPlugin plugin;

    public GuiPlayerChatListener(EzEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        if (!com.skyblockexp.ezeconomy.core.Registry.get(com.skyblockexp.ezeconomy.gui.PayFlowManager.class).isAwaiting(uuid)) return;
        e.setCancelled(true);
        String msg = e.getMessage().trim();
        var parsedMoney = NumberUtil.parseMoney(msg, com.skyblockexp.ezeconomy.core.Registry.get(com.skyblockexp.ezeconomy.manager.CurrencyManager.class).getDefaultCurrency());
        if (parsedMoney == null) {
            e.getPlayer().sendMessage(ChatColor.RED + "Invalid number. Please enter a valid amount.");
            return;
        }
        UUID target = com.skyblockexp.ezeconomy.core.Registry.get(com.skyblockexp.ezeconomy.gui.PayFlowManager.class).getTarget(uuid);
        if (target == null) {
            e.getPlayer().sendMessage(ChatColor.RED + "Target not found or expired.");
            com.skyblockexp.ezeconomy.core.Registry.get(com.skyblockexp.ezeconomy.gui.PayFlowManager.class).stopAwaiting(uuid);
            return;
        }
        String targetName = Registry.getPlugin().getServer().getPlayer(target) != null 
            ? Registry.getPlugin().getServer().getPlayer(target).getName() 
            : target.toString();
        int timeoutSeconds = Registry.getPlugin().getConfig().getInt("pay.confirmation.timeout_seconds", 30);
        long expiresAt = System.currentTimeMillis() + (timeoutSeconds * 1000L);
        Registry.get(PayFlowManager.class).createPendingTransfer(uuid, target, targetName, parsedMoney, Registry.get(CurrencyManager.class).getDefaultCurrency(), expiresAt);
        Bukkit.getScheduler().runTask(plugin, () -> PayConfirmGui.open(plugin, e.getPlayer(), targetName, parsedMoney.getAmount().toPlainString()));
        Bukkit.getScheduler().runTaskLater(plugin, () -> Registry.get(PayFlowManager.class).removeIfExpired(uuid), timeoutSeconds * 20L);
    }
}
