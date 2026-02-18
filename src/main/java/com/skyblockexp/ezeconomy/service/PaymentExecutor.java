package com.skyblockexp.ezeconomy.service;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.util.MessageUtils;
import com.skyblockexp.ezeconomy.util.CurrencyUtil;
import com.skyblockexp.ezeconomy.storage.TransferLockManager;
import com.skyblockexp.ezeconomy.storage.TransferResult;
import com.skyblockexp.ezeconomy.api.events.PlayerPayPlayerEvent;
import com.skyblockexp.ezeconomy.api.storage.StorageProvider;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.UUID;

public class PaymentExecutor {
    /**
     * Execute a payment between players. Returns true if the operation completed (success or handled failure).
     */
    public static boolean execute(EzEconomyPlugin plugin, Player from, String toName, BigDecimal amountDecimal, String currency) {
        if (from == null || toName == null || amountDecimal == null) return false;
        double netAmount = amountDecimal.doubleValue();
        StorageProvider storage = plugin.getStorageOrWarn();
        if (storage == null) return true;

        // Try online fast path
        Player online = Bukkit.getPlayerExact(toName);
        OfflinePlayer toOffline = online != null ? online : Bukkit.getOfflinePlayer(toName);

        UUID fromUuid = from.getUniqueId();

        // If recipient is not online and has never played before, treat as not found
        if (online == null && (toOffline == null || !toOffline.hasPlayedBefore())) {
            MessageUtils.send(from, plugin, "player_not_found");
            return true;
        }

        if (toOffline.getUniqueId().equals(fromUuid)) {
            MessageUtils.send(from, plugin, "cannot_pay_self");
            return true;
        }

        // Fire event
        PlayerPayPlayerEvent payEvent = new PlayerPayPlayerEvent(fromUuid, toOffline.getUniqueId(), amountDecimal);
        Bukkit.getPluginManager().callEvent(payEvent);
        if (payEvent.isCancelled()) {
            String reason = payEvent.getCancelReason();
            if (reason != null && !reason.isEmpty()) {
                from.sendMessage(reason);
            } else {
                MessageUtils.send(from, plugin, "payment_cancelled");
            }
            return true;
        }

        // Recipient currency preference
        String recipientCurrency = plugin.getCurrencyPreferenceManager().getPreferredCurrency(toOffline.getUniqueId());
        if (recipientCurrency != null && !recipientCurrency.equalsIgnoreCase(currency)) {
            UUID toUuid = toOffline.getUniqueId();
            UUID first = fromUuid.compareTo(toUuid) <= 0 ? fromUuid : toUuid;
            UUID second = fromUuid.compareTo(toUuid) <= 0 ? toUuid : fromUuid;
            java.util.concurrent.locks.ReentrantLock firstLock = TransferLockManager.getLock(first);
            java.util.concurrent.locks.ReentrantLock secondLock = first.equals(second) ? firstLock : TransferLockManager.getLock(second);
            firstLock.lock();
            if (!first.equals(second)) secondLock.lock();
            try {
                double fromBalance = storage.getBalance(fromUuid, currency);
                if (fromBalance < netAmount) {
                    MessageUtils.send(from, plugin, "not_enough_money");
                    return true;
                }
                boolean withdrew = storage.tryWithdraw(fromUuid, currency, netAmount);
                if (!withdrew) {
                    MessageUtils.send(from, plugin, "not_enough_money");
                    return true;
                }
                double creditAmount = CurrencyUtil.convert(plugin, netAmount, currency, recipientCurrency);
                if (Double.isNaN(creditAmount)) {
                    storage.deposit(fromUuid, currency, netAmount);
                    MessageUtils.send(from, plugin, "unknown_currency", java.util.Map.of("currency", recipientCurrency));
                    return true;
                }
                storage.deposit(toOffline.getUniqueId(), recipientCurrency, creditAmount);

                String payerDisplay = plugin.format(netAmount, currency);
                String receiverDisplay = plugin.format(creditAmount, recipientCurrency);
                String defaultCur = plugin.getDefaultCurrency();
                if (!currency.equalsIgnoreCase(defaultCur)) {
                    double equiv = CurrencyUtil.convert(plugin, netAmount, currency, defaultCur);
                    if (!Double.isNaN(equiv)) {
                        String equivDisplay = plugin.format(equiv, defaultCur);
                        MessageUtils.send(from, plugin, "paid_other_currency", java.util.Map.of("player", toOffline.getName(), "amount", payerDisplay, "amount_default", equivDisplay));
                    } else {
                        MessageUtils.send(from, plugin, "paid", java.util.Map.of("player", toOffline.getName(), "amount", payerDisplay));
                    }
                } else {
                    MessageUtils.send(from, plugin, "paid", java.util.Map.of("player", toOffline.getName(), "amount", payerDisplay));
                }
                if (toOffline.isOnline() && toOffline.getPlayer() != null) {
                    if (!currency.equalsIgnoreCase(defaultCur)) {
                        double equiv = CurrencyUtil.convert(plugin, creditAmount, recipientCurrency, defaultCur);
                        if (!Double.isNaN(equiv)) {
                            String equivDisplay = plugin.format(equiv, defaultCur);
                            MessageUtils.send(toOffline.getPlayer(), plugin, "received_other_currency", java.util.Map.of("player", from.getName(), "amount", receiverDisplay, "amount_default", equivDisplay));
                        } else {
                            MessageUtils.send(toOffline.getPlayer(), plugin, "received", java.util.Map.of("player", from.getName(), "amount", receiverDisplay));
                        }
                    } else {
                        MessageUtils.send(toOffline.getPlayer(), plugin, "received", java.util.Map.of("player", from.getName(), "amount", receiverDisplay));
                    }
                }
                return true;
            } finally {
                if (!first.equals(second)) secondLock.unlock();
                firstLock.unlock();
            }
        }

        // Simple transfer
        TransferResult transfer = storage.transfer(fromUuid, toOffline.getUniqueId(), currency, netAmount);
        if (!transfer.isSuccess()) {
            MessageUtils.send(from, plugin, "not_enough_money");
            return true;
        }

        String amountWithSymbol = plugin.format(netAmount, currency);
        String defaultCur = plugin.getDefaultCurrency();
        if (!currency.equalsIgnoreCase(defaultCur)) {
            double equiv = CurrencyUtil.convert(plugin, netAmount, currency, defaultCur);
            if (!Double.isNaN(equiv)) {
                String equivDisplay = plugin.format(equiv, defaultCur);
                MessageUtils.send(from, plugin, "paid_other_currency", java.util.Map.of("player", toOffline.getName(), "amount", amountWithSymbol, "amount_default", equivDisplay));
            } else {
                MessageUtils.send(from, plugin, "paid", java.util.Map.of("player", toOffline.getName(), "amount", amountWithSymbol));
            }
        } else {
            MessageUtils.send(from, plugin, "paid", java.util.Map.of("player", toOffline.getName(), "amount", amountWithSymbol));
        }
        if (toOffline.isOnline() && toOffline.getPlayer() != null) {
            MessageUtils.send(toOffline.getPlayer(), plugin, "received", java.util.Map.of("player", from.getName(), "amount", amountWithSymbol));
        }
        return true;
    }
}
