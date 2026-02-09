
package com.skyblockexp.ezeconomy.command;

import com.skyblockexp.ezeconomy.util.MessageUtils;
import com.skyblockexp.ezeconomy.util.NumberUtil;
import com.skyblockexp.ezeconomy.core.Money;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.api.storage.StorageProvider;
import com.skyblockexp.ezeconomy.storage.TransferResult;
import com.skyblockexp.ezeconomy.storage.TransferLockManager;
import com.skyblockexp.ezeconomy.util.CurrencyUtil;
import com.skyblockexp.ezeconomy.api.events.PlayerPayPlayerEvent;
import java.math.BigDecimal;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.concurrent.CompletableFuture;
import java.util.UUID;

public class PayCommand implements CommandExecutor {
    private final EzEconomyPlugin plugin;
    // Pending confirmation transfers: one per sender
    private static final java.util.concurrent.ConcurrentMap<java.util.UUID, PendingTransfer> PENDING = new java.util.concurrent.ConcurrentHashMap<>();

    private static final class PendingTransfer {
        final java.util.UUID toUuid; // may be null if unknown
        final String toName;
        final Money amount;
        final String currency;
        final long expiresAtMillis;

        PendingTransfer(java.util.UUID toUuid, String toName, Money amount, String currency, long expiresAtMillis) {
            this.toUuid = toUuid;
            this.toName = toName;
            this.amount = amount;
            this.currency = currency;
            this.expiresAtMillis = expiresAtMillis;
        }
    }

    public PayCommand(EzEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtils.send(sender, plugin, "only_players");
            return true;
        }
        if (!sender.hasPermission("ezeconomy.pay")) {
            MessageUtils.send(sender, plugin, "no_permission");
            return true;
        }
        // Support: /pay confirm
        if (args.length == 1 && args[0].equalsIgnoreCase("confirm")) {
            if (!(sender instanceof Player)) {
                MessageUtils.send(sender, plugin, "only_players");
                return true;
            }
            Player p = (Player) sender;
            PendingTransfer pt = PENDING.remove(p.getUniqueId());
            if (pt == null || System.currentTimeMillis() > pt.expiresAtMillis) {
                MessageUtils.send(sender, plugin, "no_pending_payment");
                return true;
            }
            // Execute pending transfer
            OfflinePlayer toOffline = pt.toUuid == null ? Bukkit.getOfflinePlayer(pt.toName) : Bukkit.getOfflinePlayer(pt.toUuid);
            executeTransfer(p, toOffline, pt.amount, pt.currency, true);
            return true;
        }

        if (args.length < 2) {
            MessageUtils.send(sender, plugin, "usage_pay");
            return true;
        }
        // Accept either: /pay <player> <amount> OR /pay <player> <amount> <currency>
        if (args.length > 3) {
            MessageUtils.send(sender, plugin, "usage_pay");
            return true;
        }
        Player from = (Player) sender;
        // Prefer online exact match to avoid blocking lookups.
        Player online = Bukkit.getPlayerExact(args[0]);
        OfflinePlayer to = (online != null) ? online : null;
        // Determine currency (optional 3rd arg) — use before parsing so parseMoney can know currency context
        String currency = plugin.getDefaultCurrency();
        if (args.length == 3) {
            currency = args[2].toLowerCase();
            java.util.Map<String, Object> currencies = plugin.getConfig().getConfigurationSection("multi-currency.currencies") != null
                ? plugin.getConfig().getConfigurationSection("multi-currency.currencies").getValues(false)
                : java.util.Collections.emptyMap();
            if (!currencies.containsKey(currency)) {
                MessageUtils.send(sender, plugin, "unknown_currency", java.util.Map.of("currency", currency));
                return true;
            }
        }

        Money money = NumberUtil.parseMoney(args[1], currency);

        // Validate amount and parsing
        if (money == null) {
            MessageUtils.send(sender, plugin, "invalid_amount", java.util.Map.of("input", args[1]));
            return true;
        }
        java.math.BigDecimal amountDecimal = money.getAmount();
        if (amountDecimal.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            MessageUtils.send(sender, plugin, "must_be_positive");
            return true;
        }

        // Legacy validations removed; using Money/BigDecimal checks above


        // Confirmation threshold check: if configured and amount >= threshold, create pending transfer
        double threshold = plugin.getConfig().getDouble("pay.confirmation.threshold", -1.0);
        int timeoutSeconds = plugin.getConfig().getInt("pay.confirmation.timeout_seconds", 30);
        boolean requireConfirm = threshold >= 0 && amountDecimal.compareTo(java.math.BigDecimal.valueOf(threshold)) >= 0;
        if (requireConfirm && !args[1].equalsIgnoreCase("confirm")) {
            // Store pending transfer and instruct user to confirm
            long expiresAt = System.currentTimeMillis() + (timeoutSeconds * 1000L);
            PendingTransfer pt = new PendingTransfer(to == null ? null : to.getUniqueId(), args[0], money, currency, expiresAt);
            PENDING.put(from.getUniqueId(), pt);
            MessageUtils.send(sender, plugin, "payment_confirm_required", java.util.Map.of("amount", plugin.format(amountDecimal.doubleValue(), currency), "timeout", String.valueOf(timeoutSeconds)));
            // Schedule cleanup
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                PendingTransfer cur = PENDING.get(from.getUniqueId());
                if (cur != null && cur.expiresAtMillis <= System.currentTimeMillis()) {
                    PENDING.remove(from.getUniqueId());
                }
            }, timeoutSeconds * 20L);
            return true;
        }

        // If recipient is online we can perform the transfer synchronously (fast path)
        double netAmount = amountDecimal.doubleValue();
        StorageProvider storage = plugin.getStorageOrWarn();
        if (storage == null) {
            return true;
        }

        // If recipient resolved as online, handle synchronously (fast path)
        if (to != null) {
            // Prevent paying self
            if (from.getUniqueId().equals(to.getUniqueId())) {
                MessageUtils.send(sender, plugin, "cannot_pay_self");
                return true;
            }

            // Fire cancellable event on main thread
            PlayerPayPlayerEvent payEvent = new PlayerPayPlayerEvent(from.getUniqueId(), to.getUniqueId(), amountDecimal);
            Bukkit.getPluginManager().callEvent(payEvent);
            if (payEvent.isCancelled()) {
                String reason = payEvent.getCancelReason();
                if (reason != null && !reason.isEmpty()) {
                    sender.sendMessage(reason);
                } else {
                    MessageUtils.send(sender, plugin, "payment_cancelled");
                }
                return true;
            }

            // If recipient prefers a different currency, perform cross-currency transfer
            String recipientCurrency = plugin.getCurrencyPreferenceManager().getPreferredCurrency(to.getUniqueId());
            if (recipientCurrency != null && !recipientCurrency.equalsIgnoreCase(currency)) {
                // lock both accounts to make operation atomic
                java.util.UUID fromUuid = from.getUniqueId();
                java.util.UUID toUuid = to.getUniqueId();
                java.util.UUID first = fromUuid.compareTo(toUuid) <= 0 ? fromUuid : toUuid;
                java.util.UUID second = fromUuid.compareTo(toUuid) <= 0 ? toUuid : fromUuid;
                java.util.concurrent.locks.ReentrantLock firstLock = TransferLockManager.getLock(first);
                java.util.concurrent.locks.ReentrantLock secondLock = first.equals(second) ? firstLock : TransferLockManager.getLock(second);
                firstLock.lock();
                if (!first.equals(second)) secondLock.lock();
                try {
                    double fromBalance = storage.getBalance(fromUuid, currency);
                    if (fromBalance < netAmount) {
                        MessageUtils.send(sender, plugin, "not_enough_money");
                        return true;
                    }
                    boolean withdrew = storage.tryWithdraw(fromUuid, currency, netAmount);
                    if (!withdrew) {
                        MessageUtils.send(sender, plugin, "not_enough_money");
                        return true;
                    }
                    double creditAmount = CurrencyUtil.convert(plugin, netAmount, currency, recipientCurrency);
                    if (Double.isNaN(creditAmount)) {
                        // rollback withdraw
                        storage.deposit(fromUuid, currency, netAmount);
                        MessageUtils.send(sender, plugin, "unknown_currency", java.util.Map.of("currency", recipientCurrency));
                        return true;
                    }
                    storage.deposit(to.getUniqueId(), recipientCurrency, creditAmount);

                    // success messages: show payer amount in payer currency, receiver amount in receiver currency
                    String payerDisplay = plugin.format(netAmount, currency);
                    String receiverDisplay = plugin.format(creditAmount, recipientCurrency);
                    // if payer used a non-default currency, include equivalent in default currency
                    String defaultCur = plugin.getDefaultCurrency();
                    if (!currency.equalsIgnoreCase(defaultCur)) {
                        double equiv = CurrencyUtil.convert(plugin, netAmount, currency, defaultCur);
                        if (!Double.isNaN(equiv)) {
                            String equivDisplay = plugin.format(equiv, defaultCur);
                            MessageUtils.send(sender, plugin, "paid_other_currency", java.util.Map.of("player", to.getName(), "amount", payerDisplay, "amount_default", equivDisplay));
                        } else {
                            MessageUtils.send(sender, plugin, "paid", java.util.Map.of("player", to.getName(), "amount", payerDisplay));
                        }
                    } else {
                        MessageUtils.send(sender, plugin, "paid", java.util.Map.of("player", to.getName(), "amount", payerDisplay));
                    }
                    if (to.isOnline() && to.getPlayer() != null) {
                        if (!currency.equalsIgnoreCase(defaultCur)) {
                            double equiv = CurrencyUtil.convert(plugin, creditAmount, recipientCurrency, defaultCur);
                            if (!Double.isNaN(equiv)) {
                                String equivDisplay = plugin.format(equiv, defaultCur);
                                MessageUtils.send(to.getPlayer(), plugin, "received_other_currency", java.util.Map.of("player", from.getName(), "amount", receiverDisplay, "amount_default", equivDisplay));
                            } else {
                                MessageUtils.send(to.getPlayer(), plugin, "received", java.util.Map.of("player", from.getName(), "amount", receiverDisplay));
                            }
                        } else {
                            MessageUtils.send(to.getPlayer(), plugin, "received", java.util.Map.of("player", from.getName(), "amount", receiverDisplay));
                        }
                    }
                    return true;
                } finally {
                    if (!first.equals(second)) secondLock.unlock();
                    firstLock.unlock();
                }
            }

            TransferResult transfer = storage.transfer(from.getUniqueId(), to.getUniqueId(), currency, netAmount);
            if (!transfer.isSuccess()) {
                MessageUtils.send(sender, plugin, "not_enough_money");
                return true;
            }

            // Success messages
            String amountWithSymbol = plugin.format(netAmount, currency);
            String defaultCur = plugin.getDefaultCurrency();
            if (!currency.equalsIgnoreCase(defaultCur)) {
                double equiv = CurrencyUtil.convert(plugin, netAmount, currency, defaultCur);
                if (!Double.isNaN(equiv)) {
                    String equivDisplay = plugin.format(equiv, defaultCur);
                    MessageUtils.send(sender, plugin, "paid_other_currency", java.util.Map.of("player", to.getName(), "amount", amountWithSymbol, "amount_default", equivDisplay));
                } else {
                    MessageUtils.send(sender, plugin, "paid", java.util.Map.of("player", to.getName(), "amount", amountWithSymbol));
                }
            } else {
                MessageUtils.send(sender, plugin, "paid", java.util.Map.of("player", to.getName(), "amount", amountWithSymbol));
            }
            if (to.isOnline() && to.getPlayer() != null) {
                if (!currency.equalsIgnoreCase(defaultCur)) {
                    double equiv = CurrencyUtil.convert(plugin, netAmount, currency, defaultCur);
                    if (!Double.isNaN(equiv)) {
                        String equivDisplay = plugin.format(equiv, defaultCur);
                        MessageUtils.send(to.getPlayer(), plugin, "received_other_currency", java.util.Map.of("player", from.getName(), "amount", amountWithSymbol, "amount_default", equivDisplay));
                    } else {
                        MessageUtils.send(to.getPlayer(), plugin, "received", java.util.Map.of("player", from.getName(), "amount", amountWithSymbol));
                    }
                } else {
                    MessageUtils.send(to.getPlayer(), plugin, "received", java.util.Map.of("player", from.getName(), "amount", amountWithSymbol));
                }
            }

            return true;
        }

        // Recipient not online — perform a safe async lookup and transfer to avoid blocking the main thread
        UUID fromUuid = from.getUniqueId();
        String nameArg = args[0];
        String fromName = from.getName();
        // Resolve offline player directly to avoid iterating all offline players
        OfflinePlayer offline = Bukkit.getOfflinePlayer(nameArg);
        if (offline == null) {
            MessageUtils.send(sender, plugin, "player_not_found");
            return true;
        }


        if (offline.getUniqueId().equals(fromUuid)) {
            MessageUtils.send(sender, plugin, "cannot_pay_self");
            return true;
        }
        // If offline player hasn't played before, ensure storage contains a record for them
        java.util.Map<UUID, Double> all = storage.getAllBalances(currency);
        boolean exists = offline.hasPlayedBefore() || all.containsKey(offline.getUniqueId());
        if (!exists) {
            MessageUtils.send(sender, plugin, "player_not_found");
            return true;
        }

        PlayerPayPlayerEvent payEvent = new PlayerPayPlayerEvent(fromUuid, offline.getUniqueId(), amountDecimal);
        Bukkit.getPluginManager().callEvent(payEvent);
        if (payEvent.isCancelled()) {
            String reason = payEvent.getCancelReason();
            if (reason != null && !reason.isEmpty()) {
                sender.sendMessage(reason);
            } else {
                MessageUtils.send(sender, plugin, "payment_cancelled");
            }
            return true;
        }

        TransferResult tr = storage.transfer(fromUuid, offline.getUniqueId(), currency, netAmount);
        if (!tr.isSuccess()) {
            MessageUtils.send(sender, plugin, "not_enough_money");
            return true;
        }

        String amountWithSymbol = plugin.format(netAmount, currency);
        MessageUtils.send(sender, plugin, "paid", java.util.Map.of(
            "player", offline.getName(),
            "amount", amountWithSymbol
        ));
        if (offline.isOnline() && offline.getPlayer() != null) {
            MessageUtils.send(offline.getPlayer(), plugin, "received", java.util.Map.of(
                "player", fromName,
                "amount", amountWithSymbol
            ));
        }

        return true;
    }

    private boolean executeTransfer(Player from, OfflinePlayer to, Money money, String currency, boolean confirmed) {
        StorageProvider storage = plugin.getStorageOrWarn();
        if (storage == null) return true;
        java.math.BigDecimal amountDecimal = money.getAmount();
        double netAmount = amountDecimal.doubleValue();

        UUID fromUuid = from.getUniqueId();
        String toName = (to != null) ? to.getName() : null;
        OfflinePlayer resolved = to != null ? to : (toName != null ? Bukkit.getOfflinePlayer(toName) : null);
        if (resolved == null) {
            MessageUtils.send(from, plugin, "player_not_found");
            return true;
        }
        if (resolved.getUniqueId().equals(fromUuid)) {
            MessageUtils.send(from, plugin, "cannot_pay_self");
            return true;
        }

        // Ensure recipient exists in storage
        java.util.Map<UUID, Double> all = storage.getAllBalances(currency);
        boolean exists = resolved.hasPlayedBefore() || all.containsKey(resolved.getUniqueId());
        if (!exists) {
            MessageUtils.send(from, plugin, "player_not_found");
            return true;
        }

        PlayerPayPlayerEvent payEvent = new PlayerPayPlayerEvent(fromUuid, resolved.getUniqueId(), amountDecimal);
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

        TransferResult tr = storage.transfer(fromUuid, resolved.getUniqueId(), currency, netAmount);
        if (!tr.isSuccess()) {
            MessageUtils.send(from, plugin, "not_enough_money");
            return true;
        }

        String amountWithSymbol = plugin.format(netAmount, currency);
        MessageUtils.send(from, plugin, "paid", java.util.Map.of("player", resolved.getName(), "amount", amountWithSymbol));
        if (resolved.isOnline() && resolved.getPlayer() != null) {
            MessageUtils.send(resolved.getPlayer(), plugin, "received", java.util.Map.of("player", from.getName(), "amount", amountWithSymbol));
        }
        return true;
    }
}
