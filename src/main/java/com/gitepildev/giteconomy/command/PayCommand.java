package com.gitepildev.giteconomy.command;

import com.gitepildev.giteconomy.util.MessageUtils;
import com.gitepildev.giteconomy.util.NumberUtil;
import com.gitepildev.giteconomy.core.Money;

import com.gitepildev.giteconomy.core.GitEconomyPlugin;
import com.gitepildev.giteconomy.api.storage.StorageProvider;
import com.gitepildev.giteconomy.util.CurrencyUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class PayCommand implements CommandExecutor {
    private final GitEconomyPlugin plugin;

    /** Pending confirmation transfers keyed by payer UUID. */
    private static final Map<UUID, PendingTransfer> PENDING = new ConcurrentHashMap<>();

    static final class PendingTransfer {
        private final UUID toUuid;
        private final String toName;
        private final Money amount;
        private final String currency;
        private final long expiresAtMillis;

        PendingTransfer(UUID toUuid, String toName, Money amount, String currency, long expiresAtMillis) {
            this.toUuid = toUuid;
            this.toName = toName;
            this.amount = amount;
            this.currency = currency;
            this.expiresAtMillis = expiresAtMillis;
        }

        UUID getToUuid() { return toUuid; }
        String getToName() { return toName; }
        Money getAmount() { return amount; }
        String getCurrency() { return currency; }
        long getExpiresAtMillis() { return expiresAtMillis; }
    }

    /** Package-visible helpers for tests. */
    public static void createPendingTransfer(UUID source, UUID toUuid, String toName, Money amount, String currency, long expiresAtMillis) {
        PENDING.put(source, new PendingTransfer(toUuid, toName, amount, currency, expiresAtMillis));
    }

    static PendingTransfer pollPendingTransfer(UUID source) {
        return PENDING.remove(source);
    }

    public static boolean isPendingConfirm(UUID source) {
        PendingTransfer pt = PENDING.get(source);
        return pt != null && pt.amount != null;
    }

    static void removeIfExpired(UUID source) {
        PendingTransfer pt = PENDING.get(source);
        if (pt != null && pt.expiresAtMillis <= System.currentTimeMillis()) {
            PENDING.remove(source);
        }
    }

    public PayCommand(GitEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtils.send(sender, plugin, "only_players");
            return true;
        }
        if (!sender.hasPermission("giteconomy.pay")) {
            MessageUtils.send(sender, plugin, "no_permission");
            return true;
        }
        // Support: /payall <amount> as alias for /pay * <amount>
        String[] computedArgs;
        if (command != null && command.getName().equalsIgnoreCase("payall")) {
            String[] newArgs = new String[args.length + 1];
            newArgs[0] = "*";
            if (args.length > 0) System.arraycopy(args, 0, newArgs, 1, args.length);
            computedArgs = newArgs;
        } else {
            computedArgs = args;
        }
        final String[] operands = computedArgs;
        // Support: /pay confirm
        if (operands.length == 1 && operands[0].equalsIgnoreCase("confirm")) {
            if (!(sender instanceof Player)) {
                MessageUtils.send(sender, plugin, "only_players");
                return true;
            }
            Player p = (Player) sender;
            PendingTransfer pt = pollPendingTransfer(p.getUniqueId());
            if (pt == null || System.currentTimeMillis() > pt.getExpiresAtMillis()) {
                MessageUtils.send(sender, plugin, "no_pending_payment");
                return true;
            }
            // Execute pending transfer via PaymentExecutor
            java.math.BigDecimal amountDecimal = pt.getAmount().getAmount();
            String toName = pt.getToName();
            com.gitepildev.giteconomy.service.PaymentExecutor.execute(plugin, p, toName, amountDecimal, pt.getCurrency());
            return true;
        }

        if (operands.length < 2) {
            MessageUtils.send(sender, plugin, "usage_pay");
            return true;
        }
        // Accept either: /pay <player> <amount> OR /pay <player> <amount> <currency>
        if (operands.length > 3) {
            MessageUtils.send(sender, plugin, "usage_pay");
            return true;
        }
        Player from = (Player) sender;
        // Prefer online exact match to avoid blocking lookups.
        Player online = Bukkit.getPlayerExact(operands[0]);
        OfflinePlayer to = (online != null) ? online : null;
        // Determine currency (optional 3rd arg) - use before parsing so parseMoney can know currency context
        final String currency;
        if (operands.length == 3) {
            String tmpCurrency = operands[2].toLowerCase();
            java.util.Map<String, Object> currencies = plugin.getConfig().getConfigurationSection("multi-currency.currencies") != null
                ? plugin.getConfig().getConfigurationSection("multi-currency.currencies").getValues(false)
                : java.util.Collections.emptyMap();
            if (!currencies.containsKey(tmpCurrency)) {
                MessageUtils.send(sender, plugin, "unknown_currency", java.util.Map.of("currency", tmpCurrency));
                return true;
            }
            currency = tmpCurrency;
        } else {
            currency = plugin.getDefaultCurrency();
        }

        Money money = NumberUtil.parseMoney(operands[1], currency);

        // Validate amount and parsing
        if (money == null) {
            MessageUtils.send(sender, plugin, "invalid_amount", java.util.Map.of("input", operands[1]));
            return true;
        }
        java.math.BigDecimal amountDecimal = money.getAmount();
        if (amountDecimal.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            MessageUtils.send(sender, plugin, "must_be_positive");
            return true;
        }

        // Confirmation threshold check: if configured and amount >= threshold, create pending transfer
        double threshold = plugin.getConfig().getDouble("pay.confirmation.threshold", -1.0);
        int timeoutSeconds = plugin.getConfig().getInt("pay.confirmation.timeout_seconds", 30);
        boolean requireConfirm = threshold >= 0 && amountDecimal.compareTo(java.math.BigDecimal.valueOf(threshold)) >= 0;
        if (requireConfirm && !operands[1].equalsIgnoreCase("confirm")) {
            long expiresAt = System.currentTimeMillis() + (timeoutSeconds * 1000L);
            createPendingTransfer(from.getUniqueId(), to == null ? null : to.getUniqueId(), operands[0], money, currency, expiresAt);
            MessageUtils.send(sender, plugin, "payment_confirm_required", java.util.Map.of("amount", plugin.getCurrencyFormatter().formatPriceForMessage(amountDecimal.doubleValue(), currency), "timeout", String.valueOf(timeoutSeconds)));
            Bukkit.getScheduler().runTaskLater(plugin, () -> removeIfExpired(from.getUniqueId()), timeoutSeconds * 20L);
            return true;
        }

        // Support: /pay * <amount>  -> pay all accounts
        if (operands[0].equals("*")) {
            StorageProvider storage = plugin.getStorageOrWarn();
            if (storage == null) return true;

            boolean enabled = plugin.getConfig().getBoolean("pay.pay_all.enabled", true);
            if (!enabled) {
                MessageUtils.send(sender, plugin, "no_permission");
                return true;
            }
            boolean requirePerm = plugin.getConfig().getBoolean("pay.pay_all.require_permission", true);
            if (requirePerm && !sender.hasPermission("giteconomy.payall")) {
                MessageUtils.send(sender, plugin, "no_permission");
                return true;
            }

            boolean bypassWithdraw = sender.hasPermission("giteconomy.payall.bypasswithdraw");

            boolean includeOffline = plugin.getConfig().getBoolean("pay.pay_all.include_offline", false);
            UUID fromUuid = ((Player) sender).getUniqueId();
            List<UUID> recipients = new ArrayList<>();
            java.util.Set<UUID> localOnlineUuids = new java.util.HashSet<>();
            plugin.getLogger().info("[PayAll] sender=" + sender.getName() + " uuid=" + fromUuid
                + " onlinePlayers=" + Bukkit.getOnlinePlayers().size()
                + " includeOffline=" + includeOffline
                + " crossServer=" + (plugin.getCrossServerMessenger() != null));
            if (includeOffline) {
                Map<UUID, Double> all = storage.getAllBalances(currency);
                if (all == null || all.isEmpty()) {
                    MessageUtils.send(sender, plugin, "player_not_found");
                    return true;
                }
                for (UUID u : all.keySet()) {
                    if (!u.equals(fromUuid)) recipients.add(u);
                }
            } else {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!p.getUniqueId().equals(fromUuid)) {
                        recipients.add(p.getUniqueId());
                        localOnlineUuids.add(p.getUniqueId());
                    }
                }
                // Include players from other servers via Velocity network list
                var messenger = plugin.getCrossServerMessenger();
                if (messenger != null) {
                    String senderName = ((Player) sender).getName();
                    for (String netPlayer : messenger.getNetworkPlayers()) {
                        if (netPlayer.equalsIgnoreCase(senderName)) continue;
                        if (Bukkit.getPlayerExact(netPlayer) != null) continue;
                        UUID netUuid = messenger.getNetworkPlayerUuid(netPlayer);
                        if (netUuid == null) {
                            netUuid = storage.resolvePlayerByName(netPlayer);
                        }
                        if (netUuid == null) {
                            var lookup = com.gitepildev.giteconomy.util.PlayerLookup.findByName(netPlayer);
                            if (lookup.isPresent()) {
                                netUuid = lookup.get().getUniqueId();
                            }
                        }
                        if (netUuid != null && !netUuid.equals(fromUuid) && !localOnlineUuids.contains(netUuid)) {
                            recipients.add(netUuid);
                        }
                    }
                }
                plugin.getLogger().info("[PayAll] after enumeration: recipients=" + recipients.size() + " localOnline=" + localOnlineUuids.size());
                if (recipients.isEmpty()) {
                    MessageUtils.send(sender, plugin, "player_not_found");
                    return true;
                }
            }
            if (recipients.isEmpty()) {
                plugin.getLogger().info("[PayAll] final recipients empty");
                MessageUtils.send(sender, plugin, "player_not_found");
                return true;
            }

            java.math.BigDecimal totalCost = amountDecimal.multiply(java.math.BigDecimal.valueOf(recipients.size()));
            // Withdraw total cost first (unless bypass permission)
            if (!bypassWithdraw) {
                double bal = storage.getBalance(fromUuid, currency);
                if (bal < totalCost.doubleValue()) {
                    MessageUtils.send(sender, plugin, "not_enough_money");
                    return true;
                }
                boolean withdrew = storage.tryWithdraw(fromUuid, currency, totalCost.doubleValue());
                if (!withdrew) {
                    MessageUtils.send(sender, plugin, "not_enough_money");
                    return true;
                }
            }

            // Deposit to each recipient, honoring their currency preference when possible
            for (UUID recip : recipients) {
                String recipPref = plugin.getCurrencyPreferenceManager().getPreferredCurrency(recip);
                if (recipPref == null || recipPref.equalsIgnoreCase(currency)) {
                    storage.deposit(recip, currency, amountDecimal.doubleValue());
                } else {
                    double credit = CurrencyUtil.convert(plugin, amountDecimal.doubleValue(), currency, recipPref);
                    if (Double.isNaN(credit)) {
                        // rollback if we withdrew earlier
                        if (!bypassWithdraw) storage.deposit(fromUuid, currency, totalCost.doubleValue());
                        MessageUtils.send(sender, plugin, "unknown_currency", java.util.Map.of("currency", recipPref));
                        return true;
                    }
                    storage.deposit(recip, recipPref, credit);
                }
                // Notify recipients: local or cross-server
                String amountStr = plugin.getCurrencyFormatter().formatPriceForMessage(amountDecimal.doubleValue(), currency);
                OfflinePlayer op = Bukkit.getOfflinePlayer(recip);
                if (op != null && op.isOnline() && op.getPlayer() != null) {
                    MessageUtils.send(op.getPlayer(), plugin, "received", java.util.Map.of("player", ((Player) sender).getName(), "amount", amountStr));
                } else {
                    var messenger = plugin.getCrossServerMessenger();
                    if (messenger != null) {
                        String recipName = (op != null && op.getName() != null) ? op.getName() : recip.toString();
                        messenger.sendPaymentNotification(recip, recipName, ((Player) sender).getName(), amountStr, currency);
                    }
                }
            }

            // Summary to sender
            String totalStr = plugin.getCurrencyFormatter().formatPriceForMessage(totalCost.doubleValue(), currency);
            String msg = MessageUtils.format(plugin, "paid_all_summary", java.util.Map.of("count", String.valueOf(recipients.size()), "total", totalStr, "amount", plugin.getCurrencyFormatter().formatPriceForMessage(amountDecimal.doubleValue(), currency)));
            // Fallback: if message key missing, send simple text
            if (msg == null || msg.isEmpty() || msg.contains("Message system not initialized")) {
                sender.sendMessage("Paid " + recipients.size() + " players " + plugin.getCurrencyFormatter().formatPriceForMessage(amountDecimal.doubleValue(), currency) + " each (total " + totalStr + ")");
            } else {
                sender.sendMessage(msg);
            }
            return true;
        }

        // Delegate the actual transfer logic to PaymentExecutor (handles online/offline flows)
        // If the recipient is currently online, run synchronously so callers see immediate result in tests.
        // If recipient is offline, run asynchronously to avoid blocking I/O.
        // Determine whether the recipient is known to the server (main-thread check)
        boolean knownOffline = false;
        if (online != null) {
            knownOffline = true;
        } else {
            var storage = plugin.getStorageOrWarn();
            if (storage != null && storage.resolvePlayerByName(operands[0]) != null) {
                knownOffline = true;
            }
            if (!knownOffline) {
                var messenger = plugin.getCrossServerMessenger();
                if (messenger != null && messenger.getNetworkPlayerUuid(operands[0]) != null) {
                    knownOffline = true;
                }
            }
            // Use PlayerLookup to avoid expensive or blocking lookups.
            if (!knownOffline) {
                var maybe = com.gitepildev.giteconomy.util.PlayerLookup.findByName(operands[0]);
                if (maybe.isPresent()) {
                    OfflinePlayer sample = maybe.get();
                    if (sample.hasPlayedBefore()) {
                        knownOffline = true;
                    } else {
                        try {
                            if (storage != null) {
                                java.util.Map<java.util.UUID, Double> all = storage.getAllBalances(currency);
                                if (all.containsKey(sample.getUniqueId())) {
                                    knownOffline = true;
                                }
                            }
                        } catch (Exception ignored) {
                            // swallow and treat as unknown
                        }
                    }
                }
            }
        }

        if (online != null) {
            // Execute immediately on current thread for online recipient (tests expect immediate result)
            com.gitepildev.giteconomy.service.PaymentExecutor.execute(plugin, from, operands[0], amountDecimal, currency, knownOffline);
        } else {
            boolean ko = knownOffline;
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                com.gitepildev.giteconomy.service.PaymentExecutor.execute(plugin, from, operands[0], amountDecimal, currency, ko);
            });
        }
        return true;
    }

    // executeTransfer removed: PaymentExecutor centralizes transfer logic
}
