package com.skyblockexp.ezeconomy.messaging;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.api.storage.StorageProvider;
import com.skyblockexp.ezeconomy.storage.MySQLStorageProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CrossServerMessenger implements PluginMessageListener {
    public static final String CHANNEL = "ezeconomy:notify";
    private final EzEconomyPlugin plugin;
    private final Set<String> networkPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, List<String>> localPendingNotifications = new ConcurrentHashMap<>();

    public CrossServerMessenger(EzEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);
        logVerbose("Registered cross-server messaging channel: " + CHANNEL);
    }

    public void unregister() {
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);
        Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL, this);
    }

    public Set<String> getNetworkPlayers() {
        return Collections.unmodifiableSet(networkPlayers);
    }

    public boolean isNetworkPlayer(String name) {
        return networkPlayers.contains(name);
    }

    public void sendPaymentNotification(UUID recipientUuid, String recipientName,
                                         String senderName, String amount, String currency) {
        Player relay = findRelayPlayer();
        if (relay == null) {
            storePendingNotification(recipientUuid, recipientName, senderName, amount, currency);
            return;
        }

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bos);
            out.writeUTF("NOTIFY");
            out.writeUTF(recipientUuid.toString());
            out.writeUTF(recipientName);
            out.writeUTF(senderName);
            out.writeUTF(amount);
            out.writeUTF(currency);
            relay.sendPluginMessage(plugin, CHANNEL, bos.toByteArray());
            logVerbose("Relayed cross-server notification for recipient=" + recipientName + " via " + relay.getName());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to send cross-server notification: " + e.getMessage());
            storePendingNotification(recipientUuid, recipientName, senderName, amount, currency);
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] data) {
        if (!CHANNEL.equals(channel)) return;

        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
            String type = in.readUTF();

            if ("NOTIFY".equals(type)) {
                String recipientUuidStr = in.readUTF();
                String recipientName = in.readUTF();
                String senderName = in.readUTF();
                String amount = in.readUTF();
                String currency = in.readUTF();
                logVerbose("Received cross-server NOTIFY: recipient=" + recipientName
                    + " uuid=" + recipientUuidStr + " from=" + senderName + " amount=" + amount);

                Player recipient = Bukkit.getPlayer(UUID.fromString(recipientUuidStr));
                if (recipient != null && recipient.isOnline()) {
                    String msg = plugin.getMessageProvider().get("received",
                        Map.of("player", senderName, "amount", amount));
                    logVerbose("Delivering cross-server message to " + recipientName + ": " + msg);
                    recipient.sendMessage(msg);
                } else {
                    logVerbose("Cross-server NOTIFY: recipient " + recipientName + " not found locally (uuid=" + recipientUuidStr + ")");
                }
            } else if ("RECIPIENT_OFFLINE".equals(type)) {
                String recipientUuidStr = in.readUTF();
                String senderName = in.readUTF();
                String amount = in.readUTF();
                String currency = in.readUTF();
                storePendingNotification(UUID.fromString(recipientUuidStr), recipientUuidStr, senderName, amount, currency);
            } else if ("PLAYER_LIST".equals(type)) {
                int count = in.readInt();
                Set<String> newList = ConcurrentHashMap.newKeySet();
                for (int i = 0; i < count; i++) {
                    newList.add(in.readUTF());
                }
                networkPlayers.clear();
                networkPlayers.addAll(newList);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to read cross-server message: " + e.getMessage());
        }
    }

    public void deliverPendingNotifications(Player player) {
        StorageProvider storage = plugin.getStorageOrWarn();
        List<String> messages = new ArrayList<>();
        if (storage instanceof MySQLStorageProvider) {
            MySQLStorageProvider mysql = (MySQLStorageProvider) storage;
            messages.addAll(mysql.pollPendingNotifications(player.getUniqueId()));
        } else {
            List<String> drained = localPendingNotifications.remove(player.getUniqueId());
            if (drained != null && !drained.isEmpty()) {
                messages.addAll(drained);
                logVerbose("Delivered " + drained.size() + " in-memory pending notifications to " + player.getName());
            }
        }
        for (String msg : messages) {
            player.sendMessage(msg);
        }
    }

    private void storePendingNotification(UUID recipientUuid, String recipientName, String senderName, String amount, String currency) {
        StorageProvider storage = plugin.getStorageOrWarn();
        String msg = plugin.getMessageProvider().get("received",
            Map.of("player", senderName, "amount", amount));
        if (storage instanceof MySQLStorageProvider) {
            MySQLStorageProvider mysql = (MySQLStorageProvider) storage;
            mysql.insertPendingNotification(recipientUuid, msg);
            return;
        }

        localPendingNotifications.computeIfAbsent(recipientUuid, ignored -> Collections.synchronizedList(new ArrayList<>())).add(msg);
        logVerbose("Queued in-memory pending notification for recipient=" + recipientName + " (storage is not MySQL)");
    }

    private Player findRelayPlayer() {
        Collection<? extends Player> online = Bukkit.getOnlinePlayers();
        return online.isEmpty() ? null : online.iterator().next();
    }

    private void logVerbose(String message) {
        if (plugin.getConfig().getBoolean("cross-server.verbose-logging", false)) {
            plugin.getLogger().info(message);
        }
    }
}
