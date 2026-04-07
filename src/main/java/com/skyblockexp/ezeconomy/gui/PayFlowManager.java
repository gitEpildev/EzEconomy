package com.skyblockexp.ezeconomy.gui;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.skyblockexp.ezeconomy.core.Money;

/**
 * Tracks pending pay flows for players that are entering custom amounts via chat
 * and stores pending confirmation transfers created by the `/pay` command.
 */
public class PayFlowManager {
    private static class Flow {
        UUID targetUuid;
        String targetName;
        Money amount; // null until set
        String currency;
        long expiresAtMillis;
        boolean awaitingAmount;

        Flow(UUID targetUuid, String targetName, boolean awaitingAmount) {
            this.targetUuid = targetUuid;
            this.targetName = targetName;
            this.awaitingAmount = awaitingAmount;
        }
    }

    private final Map<UUID, Flow> flows = new ConcurrentHashMap<>();

    public static class PendingTransfer {
        private final UUID toUuid; // may be null
        private final String toName;
        private final Money amount;
        private final String currency;
        private final long expiresAtMillis;

        public PendingTransfer(UUID toUuid, String toName, Money amount, String currency, long expiresAtMillis) {
            this.toUuid = toUuid;
            this.toName = toName;
            this.amount = amount;
            this.currency = currency;
            this.expiresAtMillis = expiresAtMillis;
        }

        public UUID getToUuid() { return toUuid; }
        public String getToName() { return toName; }
        public Money getAmount() { return amount; }
        public String getCurrency() { return currency; }
        public long getExpiresAtMillis() { return expiresAtMillis; }
    }

    // Start a flow that is awaiting a custom amount via chat
    public void startAwaiting(UUID source, UUID targetUuid, String targetName) {
        flows.put(source, new Flow(targetUuid, targetName, true));
    }

    public void setCurrency(UUID source, String currency) {
        Flow f = flows.get(source);
        if (f == null) {
            f = new Flow(null, null, false);
            flows.put(source, f);
        }
        f.currency = currency;
    }

    public String getCurrency(UUID source) {
        Flow f = flows.get(source);
        return f == null ? null : f.currency;
    }

    // Set or create a pending transfer (awaiting confirmation)
    public void createPendingTransfer(UUID source, UUID toUuid, String toName, Money amount, String currency, long expiresAtMillis) {
        Flow f = flows.get(source);
        if (f == null) {
            f = new Flow(toUuid, toName, false);
            flows.put(source, f);
        }
        f.targetUuid = toUuid;
        f.targetName = toName;
        f.amount = amount;
        f.currency = currency;
        f.expiresAtMillis = expiresAtMillis;
        f.awaitingAmount = false;
    }

    // Poll (remove and return) the pending transfer for the given source
    public PendingTransfer pollPendingTransfer(UUID source) {
        Flow f = flows.remove(source);
        if (f == null || f.amount == null) return null;
        return new PendingTransfer(f.targetUuid, f.targetName, f.amount, f.currency, f.expiresAtMillis);
    }

    public PendingTransfer getPendingTransfer(UUID source) {
        Flow f = flows.get(source);
        if (f == null || f.amount == null) return null;
        return new PendingTransfer(f.targetUuid, f.targetName, f.amount, f.currency, f.expiresAtMillis);
    }

    public UUID getTarget(UUID source) {
        Flow f = flows.get(source);
        return f == null ? null : f.targetUuid;
    }

    public void stopAwaiting(UUID source) {
        flows.remove(source);
    }

    public boolean isAwaiting(UUID source) {
        Flow f = flows.get(source);
        return f != null && f.awaitingAmount && f.amount == null;
    }

    public boolean isPendingConfirm(UUID source) {
        Flow f = flows.get(source);
        return f != null && f.amount != null;
    }

    public void removeIfExpired(UUID source) {
        Flow f = flows.get(source);
        if (f != null && f.amount != null && f.expiresAtMillis <= System.currentTimeMillis()) {
            flows.remove(source);
        }
    }
}
