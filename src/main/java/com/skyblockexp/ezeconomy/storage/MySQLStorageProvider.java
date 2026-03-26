package com.skyblockexp.ezeconomy.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import com.skyblockexp.ezeconomy.api.storage.StorageProvider;
import com.skyblockexp.ezeconomy.api.storage.models.Transaction;
import com.skyblockexp.ezeconomy.api.storage.exceptions.StorageInitException;
import com.skyblockexp.ezeconomy.api.storage.exceptions.StorageLoadException;
import com.skyblockexp.ezeconomy.api.storage.exceptions.StorageSaveException;

import java.sql.*;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.math.BigDecimal;
import com.skyblockexp.ezeconomy.api.events.BankPreTransactionEvent;
import com.skyblockexp.ezeconomy.api.events.BankPostTransactionEvent;
import com.skyblockexp.ezeconomy.api.events.TransactionType;

/**
 * MySQL implementation of the StorageProvider interface for EzEconomy.
 * Handles player and bank balances using a MySQL database.
 * Thread-safe and ready for open-source use.
 */
public class MySQLStorageProvider implements StorageProvider {
    private final EzEconomyPlugin plugin;
    private Connection connection;
    private String table;
    private final Object lock = new Object();
    private final YamlConfiguration dbConfig;

    /**
     * Constructs a MySQLStorageProvider with the given plugin and configuration.
     * @param plugin EzEconomy plugin instance
     * @param dbConfig YAML configuration for MySQL
     */
    public MySQLStorageProvider(EzEconomyPlugin plugin, YamlConfiguration dbConfig) {
        this.plugin = plugin;
        this.dbConfig = dbConfig;
        if (dbConfig == null) throw new IllegalArgumentException("MySQL config is missing!");
        this.table = dbConfig.getString("mysql.table", "balances");
    }

    @Override
    public void init() throws StorageInitException {
        // Create tables/schema if needed
        if (connection == null) {
            // Establish a temporary connection for schema creation
            String host = dbConfig.getString("mysql.host");
            int port = dbConfig.getInt("mysql.port");
            String database = dbConfig.getString("mysql.database");
            String username = dbConfig.getString("mysql.username");
            String password = dbConfig.getString("mysql.password");
            try (Connection tempConn = DriverManager.getConnection(
                    "jdbc:mysql://" + host + ":" + port + "/" + database,
                    username, password)) {
                Statement stmt = tempConn.createStatement();
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS `" + table + "` (uuid VARCHAR(36), currency VARCHAR(32), balance DOUBLE, PRIMARY KEY (uuid, currency))");
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS banks (name VARCHAR(64), currency VARCHAR(32), balance DOUBLE, PRIMARY KEY (name, currency))");
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS bank_members (bank VARCHAR(64), uuid VARCHAR(36), owner BOOLEAN, PRIMARY KEY (bank, uuid))");
                // Optional player info table to persist last-known name and displayName
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS players (uuid VARCHAR(36) PRIMARY KEY, name VARCHAR(64), displayName VARCHAR(128))");
            } catch (SQLException e) {
                plugin.getLogger().warning("MySQL schema init failed: " + e.getMessage());
                throw new StorageInitException("Failed to initialize MySQL schema", e);
            }
        }
    }

    @Override
    public void load() throws StorageLoadException {
        // Establish connection only
        String host = dbConfig.getString("mysql.host");
        int port = dbConfig.getInt("mysql.port");
        String database = dbConfig.getString("mysql.database");
        String username = dbConfig.getString("mysql.username");
        String password = dbConfig.getString("mysql.password");
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
            connection = DriverManager.getConnection(
                "jdbc:mysql://" + host + ":" + port + "/" + database,
                username, password);
        } catch (SQLException e) {
            plugin.getLogger().warning("MySQL connection failed: " + e.getMessage());
            throw new StorageLoadException("Failed to connect to MySQL", e);
        }
    }

    @Override
    public void save() throws StorageSaveException {
        // No in-memory cache, so nothing to save
    }

    @Override
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public java.util.List<Transaction> getTransactions(java.util.UUID uuid, String currency) {
        java.util.List<Transaction> transactions = new java.util.ArrayList<>();
        synchronized (lock) {
            try {
                // Assumes a table: transactions(uuid VARCHAR(36), currency VARCHAR(32), amount DOUBLE, timestamp BIGINT)
                String sql = "SELECT amount, timestamp FROM transactions WHERE uuid=? AND currency=? ORDER BY timestamp DESC";
                PreparedStatement ps = connection.prepareStatement(sql);
                ps.setString(1, uuid.toString());
                ps.setString(2, currency);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    double amount = rs.getDouble("amount");
                    long timestamp = rs.getLong("timestamp");
                    Transaction t = new Transaction(uuid, currency, amount, timestamp);
                    transactions.add(t);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("[EzEconomy] MySQL getTransactions failed for " + uuid + " (" + currency + "): " + e.getMessage());
            }
        }
        return transactions;
    }

    /**
     * Gets the balance for a player and currency.
     */
    @Override
    public double getBalance(UUID uuid, String currency) {
        com.skyblockexp.ezeconomy.lock.LockManager lm = plugin.getLockManager();
        if (lm != null) {
            String token = null;
            try {
                token = lm.acquire(uuid, plugin.getConfig().getLong("redis.ttl-ms", 5000), plugin.getConfig().getLong("redis.retry-ms", 50), plugin.getConfig().getInt("redis.max-attempts", 100));
                if (token != null) {
                    try {
                        PreparedStatement ps = connection.prepareStatement("SELECT balance FROM `" + table + "` WHERE uuid=? AND currency=?");
                        ps.setString(1, uuid.toString());
                        ps.setString(2, currency);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) return rs.getDouble(1);
                    } catch (SQLException e) {
                        plugin.getLogger().severe("[EzEconomy] MySQL getBalance failed for " + uuid + " (" + currency + "): " + e.getMessage());
                    } catch (Exception e) {
                        plugin.getLogger().severe("[EzEconomy] Unexpected error in getBalance for " + uuid + " (" + currency + "): " + e.getMessage());
                    }
                    return 0.0;
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                if (token != null) lm.release(uuid, token);
            }
        }
        synchronized (lock) {
            try {
                PreparedStatement ps = connection.prepareStatement("SELECT balance FROM `" + table + "` WHERE uuid=? AND currency=?");
                ps.setString(1, uuid.toString());
                ps.setString(2, currency);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getDouble(1);
            } catch (SQLException e) {
                plugin.getLogger().severe("[EzEconomy] MySQL getBalance failed for " + uuid + " (" + currency + "): " + e.getMessage());
            } catch (Exception e) {
                plugin.getLogger().severe("[EzEconomy] Unexpected error in getBalance for " + uuid + " (" + currency + "): " + e.getMessage());
            }
            return 0.0;
        }
    }

    @Override
    public com.skyblockexp.ezeconomy.dto.EconomyPlayer getPlayer(UUID uuid) {
        com.skyblockexp.ezeconomy.lock.LockManager lm = plugin.getLockManager();
        if (lm != null) {
            String token = null;
            try {
                token = lm.acquire(uuid, plugin.getConfig().getLong("redis.ttl-ms", 5000), plugin.getConfig().getLong("redis.retry-ms", 50), plugin.getConfig().getInt("redis.max-attempts", 100));
                if (token != null) {
                    try {
                        PreparedStatement ps = connection.prepareStatement("SELECT name, displayName FROM players WHERE uuid=?");
                        ps.setString(1, uuid.toString());
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            String name = rs.getString(1);
                            String display = rs.getString(2);
                            if (name == null) name = uuid.toString();
                            if (display == null) display = name;
                            return new com.skyblockexp.ezeconomy.dto.EconomyPlayer(uuid, name, display);
                        }
                    } catch (Exception ignored) {}
                    org.bukkit.OfflinePlayer of = org.bukkit.Bukkit.getOfflinePlayer(uuid);
                    String name = of != null && of.getName() != null ? of.getName() : uuid.toString();
                    String display = (of instanceof org.bukkit.entity.Player) ? ((org.bukkit.entity.Player) of).getDisplayName() : name;
                    return new com.skyblockexp.ezeconomy.dto.EconomyPlayer(uuid, name, display);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                if (token != null) lm.release(uuid, token);
            }
        }
        synchronized (lock) {
            try {
                PreparedStatement ps = connection.prepareStatement("SELECT name, displayName FROM players WHERE uuid=?");
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String name = rs.getString(1);
                    String display = rs.getString(2);
                    if (name == null) name = uuid.toString();
                    if (display == null) display = name;
                    return new com.skyblockexp.ezeconomy.dto.EconomyPlayer(uuid, name, display);
                }
            } catch (Exception ignored) {}
            org.bukkit.OfflinePlayer of = org.bukkit.Bukkit.getOfflinePlayer(uuid);
            String name = of != null && of.getName() != null ? of.getName() : uuid.toString();
            String display = (of instanceof org.bukkit.entity.Player) ? ((org.bukkit.entity.Player) of).getDisplayName() : name;
            return new com.skyblockexp.ezeconomy.dto.EconomyPlayer(uuid, name, display);
        }
    }

    @Override
    public boolean playerExists(UUID uuid) {
        com.skyblockexp.ezeconomy.lock.LockManager lm = plugin.getLockManager();
        if (lm != null) {
            String token = null;
            try {
                token = lm.acquire(uuid, plugin.getConfig().getLong("redis.ttl-ms", 5000), plugin.getConfig().getLong("redis.retry-ms", 50), plugin.getConfig().getInt("redis.max-attempts", 100));
                if (token != null) {
                    try {
                        PreparedStatement ps = connection.prepareStatement("SELECT 1 FROM `" + table + "` WHERE uuid=? LIMIT 1");
                        ps.setString(1, uuid.toString());
                        ResultSet rs = ps.executeQuery();
                        return rs.next();
                    } catch (SQLException e) {
                        plugin.getLogger().severe("[EzEconomy] MySQL playerExists failed for " + uuid + ": " + e.getMessage());
                        return false;
                    }
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                if (token != null) lm.release(uuid, token);
            }
        }
        synchronized (lock) {
            try {
                PreparedStatement ps = connection.prepareStatement("SELECT 1 FROM `" + table + "` WHERE uuid=? LIMIT 1");
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                return rs.next();
            } catch (SQLException e) {
                plugin.getLogger().severe("[EzEconomy] MySQL playerExists failed for " + uuid + ": " + e.getMessage());
                return false;
            }
        }
    }

    @Override
    public void setBalance(UUID uuid, String currency, double amount) {
        com.skyblockexp.ezeconomy.lock.LockManager lm = plugin.getLockManager();
        if (lm != null) {
            String token = null;
            try {
                token = lm.acquire(uuid, plugin.getConfig().getLong("redis.ttl-ms", 5000), plugin.getConfig().getLong("redis.retry-ms", 50), plugin.getConfig().getInt("redis.max-attempts", 100));
                if (token != null) {
                    try {
                        PreparedStatement ps = connection.prepareStatement("REPLACE INTO `" + table + "` (uuid, currency, balance) VALUES (?, ?, ?)");
                        ps.setString(1, uuid.toString());
                        ps.setString(2, currency);
                        ps.setDouble(3, amount);
                        ps.executeUpdate();
                        org.bukkit.OfflinePlayer of = org.bukkit.Bukkit.getOfflinePlayer(uuid);
                        String name = of != null && of.getName() != null ? of.getName() : uuid.toString();
                        String display = (of instanceof org.bukkit.entity.Player) ? ((org.bukkit.entity.Player) of).getDisplayName() : name;
                        PreparedStatement ps2 = connection.prepareStatement("REPLACE INTO players (uuid, name, displayName) VALUES (?, ?, ?)");
                        ps2.setString(1, uuid.toString());
                        ps2.setString(2, name);
                        ps2.setString(3, display);
                        ps2.executeUpdate();
                        return;
                    } catch (SQLException e) {
                        plugin.getLogger().severe("[EzEconomy] MySQL setBalance failed for " + uuid + " (" + currency + "): " + e.getMessage());
                        return;
                    } catch (Exception e) {
                        plugin.getLogger().severe("[EzEconomy] Unexpected error in setBalance for " + uuid + " (" + currency + "): " + e.getMessage());
                        return;
                    }
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                if (token != null) lm.release(uuid, token);
            }
        }
        synchronized (lock) {
            try {
                PreparedStatement ps = connection.prepareStatement("REPLACE INTO `" + table + "` (uuid, currency, balance) VALUES (?, ?, ?)");
                ps.setString(1, uuid.toString());
                ps.setString(2, currency);
                ps.setDouble(3, amount);
                ps.executeUpdate();
                // Persist last known name/displayName
                org.bukkit.OfflinePlayer of = org.bukkit.Bukkit.getOfflinePlayer(uuid);
                String name = of != null && of.getName() != null ? of.getName() : uuid.toString();
                String display = (of instanceof org.bukkit.entity.Player) ? ((org.bukkit.entity.Player) of).getDisplayName() : name;
                PreparedStatement ps2 = connection.prepareStatement("REPLACE INTO players (uuid, name, displayName) VALUES (?, ?, ?)");
                ps2.setString(1, uuid.toString());
                ps2.setString(2, name);
                ps2.setString(3, display);
                ps2.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("[EzEconomy] MySQL setBalance failed for " + uuid + " (" + currency + "): " + e.getMessage());
            } catch (Exception e) {
                plugin.getLogger().severe("[EzEconomy] Unexpected error in setBalance for " + uuid + " (" + currency + "): " + e.getMessage());
            }
        }
    }

    @Override
    public boolean tryWithdraw(UUID uuid, String currency, double amount) {
        com.skyblockexp.ezeconomy.lock.LockManager lm = plugin.getLockManager();
        if (lm != null) {
            String token = null;
            try {
                token = lm.acquire(uuid, plugin.getConfig().getLong("redis.ttl-ms", 5000), plugin.getConfig().getLong("redis.retry-ms", 50), plugin.getConfig().getInt("redis.max-attempts", 100));
                if (token != null) {
                    try {
                        PreparedStatement ps = connection.prepareStatement(
                            "UPDATE `" + table + "` SET balance = balance - ? WHERE uuid=? AND currency=? AND balance >= ?"
                        );
                        ps.setDouble(1, amount);
                        ps.setString(2, uuid.toString());
                        ps.setString(3, currency);
                        ps.setDouble(4, amount);
                        return ps.executeUpdate() > 0;
                    } catch (SQLException e) {
                        plugin.getLogger().severe("[EzEconomy] MySQL tryWithdraw failed for " + uuid + " (" + currency + "): " + e.getMessage());
                        return false;
                    } catch (Exception e) {
                        plugin.getLogger().severe("[EzEconomy] Unexpected error in tryWithdraw for " + uuid + " (" + currency + "): " + e.getMessage());
                        return false;
                    }
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                if (token != null) lm.release(uuid, token);
            }
        }
        synchronized (lock) {
            try {
                PreparedStatement ps = connection.prepareStatement(
                    "UPDATE `" + table + "` SET balance = balance - ? WHERE uuid=? AND currency=? AND balance >= ?"
                );
                ps.setDouble(1, amount);
                ps.setString(2, uuid.toString());
                ps.setString(3, currency);
                ps.setDouble(4, amount);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().severe("[EzEconomy] MySQL tryWithdraw failed for " + uuid + " (" + currency + "): " + e.getMessage());
            } catch (Exception e) {
                plugin.getLogger().severe("[EzEconomy] Unexpected error in tryWithdraw for " + uuid + " (" + currency + "): " + e.getMessage());
            }
            return false;
        }
    }

    @Override
    public void deposit(UUID uuid, String currency, double amount) {
        com.skyblockexp.ezeconomy.lock.LockManager lm = plugin.getLockManager();
        if (lm != null) {
            String token = null;
            try {
                token = lm.acquire(uuid, plugin.getConfig().getLong("redis.ttl-ms", 5000), plugin.getConfig().getLong("redis.retry-ms", 50), plugin.getConfig().getInt("redis.max-attempts", 100));
                if (token != null) {
                    try {
                        PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO `" + table + "` (uuid, currency, balance) VALUES (?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE balance = balance + VALUES(balance)"
                        );
                        ps.setString(1, uuid.toString());
                        ps.setString(2, currency);
                        ps.setDouble(3, amount);
                        ps.executeUpdate();
                        org.bukkit.OfflinePlayer of = org.bukkit.Bukkit.getOfflinePlayer(uuid);
                        String name = of != null && of.getName() != null ? of.getName() : uuid.toString();
                        String display = (of instanceof org.bukkit.entity.Player) ? ((org.bukkit.entity.Player) of).getDisplayName() : name;
                        PreparedStatement ps2 = connection.prepareStatement("REPLACE INTO players (uuid, name, displayName) VALUES (?, ?, ?)");
                        ps2.setString(1, uuid.toString());
                        ps2.setString(2, name);
                        ps2.setString(3, display);
                        ps2.executeUpdate();
                        return;
                    } catch (SQLException e) {
                        plugin.getLogger().severe("[EzEconomy] MySQL deposit failed for " + uuid + " (" + currency + "): " + e.getMessage());
                        return;
                    } catch (Exception e) {
                        plugin.getLogger().severe("[EzEconomy] Unexpected error in deposit for " + uuid + " (" + currency + "): " + e.getMessage());
                        return;
                    }
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                if (token != null) lm.release(uuid, token);
            }
        }
        synchronized (lock) {
            try {
                PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO `" + table + "` (uuid, currency, balance) VALUES (?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE balance = balance + VALUES(balance)"
                );
                ps.setString(1, uuid.toString());
                ps.setString(2, currency);
                ps.setDouble(3, amount);
                ps.executeUpdate();
                // Persist last known name/displayName
                org.bukkit.OfflinePlayer of = org.bukkit.Bukkit.getOfflinePlayer(uuid);
                String name = of != null && of.getName() != null ? of.getName() : uuid.toString();
                String display = (of instanceof org.bukkit.entity.Player) ? ((org.bukkit.entity.Player) of).getDisplayName() : name;
                PreparedStatement ps2 = connection.prepareStatement("REPLACE INTO players (uuid, name, displayName) VALUES (?, ?, ?)");
                ps2.setString(1, uuid.toString());
                ps2.setString(2, name);
                ps2.setString(3, display);
                ps2.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("[EzEconomy] MySQL deposit failed for " + uuid + " (" + currency + "): " + e.getMessage());
            } catch (Exception e) {
                plugin.getLogger().severe("[EzEconomy] Unexpected error in deposit for " + uuid + " (" + currency + "): " + e.getMessage());
            }
        }
    }

    public void shutdown() {
        synchronized (lock) {
            try {
                if (connection != null && !connection.isClosed()) connection.close();
            } catch (SQLException e) {
                plugin.getLogger().severe("[EzEconomy] MySQL shutdown failed: " + e.getMessage());
            } catch (Exception e) {
                plugin.getLogger().severe("[EzEconomy] Unexpected error on shutdown: " + e.getMessage());
            }
        }
    }
    public Map<UUID, Double> getAllBalances(String currency) {
        synchronized (lock) {
            Map<UUID, Double> map = new ConcurrentHashMap<>();
            try {
                PreparedStatement ps = connection.prepareStatement("SELECT uuid, balance FROM `" + table + "` WHERE currency=?");
                ps.setString(1, currency);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    try {
                        UUID uuid = UUID.fromString(rs.getString(1));
                        double bal = rs.getDouble(2);
                        map.put(uuid, bal);
                    } catch (IllegalArgumentException ignored) {}
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("[EzEconomy] MySQL getAllBalances failed (" + currency + "): " + e.getMessage());
            }
            return map;
        }
    }

    @Override
    public com.skyblockexp.ezeconomy.storage.TransferResult transfer(UUID fromUuid, UUID toUuid, String currency, double debitAmount, double creditAmount) {
        com.skyblockexp.ezeconomy.lock.LockManager lm = plugin.getLockManager();
        if (lm == null) {
            // No distributed lock available, fall back to default behavior
            double fromBefore = getBalance(fromUuid, currency);
            double toBefore = getBalance(toUuid, currency);

            com.skyblockexp.ezeconomy.api.events.PreTransactionEvent pre = new com.skyblockexp.ezeconomy.api.events.PreTransactionEvent(fromUuid, toUuid, java.math.BigDecimal.valueOf(debitAmount), com.skyblockexp.ezeconomy.api.events.TransactionType.TRANSFER);
            try {
                if (plugin.getServer().isPrimaryThread()) {
                    plugin.getServer().getPluginManager().callEvent(pre);
                } else {
                    plugin.getServer().getScheduler().callSyncMethod(plugin, () -> {
                        plugin.getServer().getPluginManager().callEvent(pre);
                        return null;
                    }).get();
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[EzEconomy] Failed to fire PreTransactionEvent: " + e.getMessage());
            }
            if (pre.isCancelled()) {
                return com.skyblockexp.ezeconomy.storage.TransferResult.failure(fromBefore, toBefore);
            }

            com.skyblockexp.ezeconomy.storage.TransferResult result = StorageProvider.super.transfer(fromUuid, toUuid, currency, debitAmount, creditAmount);

            com.skyblockexp.ezeconomy.api.events.PostTransactionEvent post = new com.skyblockexp.ezeconomy.api.events.PostTransactionEvent(
                fromUuid, toUuid, java.math.BigDecimal.valueOf(debitAmount), com.skyblockexp.ezeconomy.api.events.TransactionType.TRANSFER,
                result.isSuccess(), java.math.BigDecimal.valueOf(fromBefore), java.math.BigDecimal.valueOf(result.getFromBalance()),
                java.math.BigDecimal.valueOf(toBefore), java.math.BigDecimal.valueOf(result.getToBalance())
            );
            try {
                if (plugin.getServer().isPrimaryThread()) {
                    plugin.getServer().getPluginManager().callEvent(post);
                } else {
                    plugin.getServer().getScheduler().callSyncMethod(plugin, () -> {
                        plugin.getServer().getPluginManager().callEvent(post);
                        return null;
                    }).get();
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[EzEconomy] Failed to fire PostTransactionEvent: " + e.getMessage());
            }

            return result;
        }

        // Acquire distributed locks for both UUIDs in canonical order
        UUID[] ordered = new UUID[]{fromUuid, toUuid};
        if (fromUuid.compareTo(toUuid) > 0) {
            ordered = new UUID[]{toUuid, fromUuid};
        }
        String[] tokens = null;
        try {
            tokens = lm.acquireOrdered(ordered, plugin.getConfig().getLong("redis.ttl-ms", 5000), plugin.getConfig().getLong("redis.retry-ms", 50), plugin.getConfig().getInt("redis.max-attempts", 100));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        if (tokens == null) {
            // Couldn't acquire distributed locks; fall back to default transfer
            return StorageProvider.super.transfer(fromUuid, toUuid, currency, debitAmount, creditAmount);
        }

        try {
            // Re-read balances while holding distributed locks
            double fromBefore;
            double toBefore;
            try {
                PreparedStatement ps = connection.prepareStatement("SELECT balance FROM `" + table + "` WHERE uuid=? AND currency=?");
                ps.setString(1, fromUuid.toString());
                ps.setString(2, currency);
                ResultSet rs = ps.executeQuery();
                fromBefore = rs.next() ? rs.getDouble(1) : 0.0;
                PreparedStatement ps2 = connection.prepareStatement("SELECT balance FROM `" + table + "` WHERE uuid=? AND currency=?");
                ps2.setString(1, toUuid.toString());
                ps2.setString(2, currency);
                ResultSet rs2 = ps2.executeQuery();
                toBefore = rs2.next() ? rs2.getDouble(1) : 0.0;
            } catch (SQLException e) {
                plugin.getLogger().severe("[EzEconomy] MySQL transfer balance read failed: " + e.getMessage());
                return com.skyblockexp.ezeconomy.storage.TransferResult.failure(0.0, 0.0);
            }

            com.skyblockexp.ezeconomy.api.events.PreTransactionEvent pre = new com.skyblockexp.ezeconomy.api.events.PreTransactionEvent(fromUuid, toUuid, java.math.BigDecimal.valueOf(debitAmount), com.skyblockexp.ezeconomy.api.events.TransactionType.TRANSFER);
            try {
                if (plugin.getServer().isPrimaryThread()) {
                    plugin.getServer().getPluginManager().callEvent(pre);
                } else {
                    plugin.getServer().getScheduler().callSyncMethod(plugin, () -> {
                        plugin.getServer().getPluginManager().callEvent(pre);
                        return null;
                    }).get();
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[EzEconomy] Failed to fire PreTransactionEvent: " + e.getMessage());
            }
            if (pre.isCancelled()) {
                return com.skyblockexp.ezeconomy.storage.TransferResult.failure(fromBefore, toBefore);
            }

            // Perform atomic withdraw
            try {
                PreparedStatement psw = connection.prepareStatement(
                    "UPDATE `" + table + "` SET balance = balance - ? WHERE uuid=? AND currency=? AND balance >= ?"
                );
                psw.setDouble(1, debitAmount);
                psw.setString(2, fromUuid.toString());
                psw.setString(3, currency);
                psw.setDouble(4, debitAmount);
                int updated = psw.executeUpdate();
                if (updated <= 0) {
                    double refreshedFrom = getBalance(fromUuid, currency);
                    double refreshedTo = getBalance(toUuid, currency);
                    return com.skyblockexp.ezeconomy.storage.TransferResult.failure(refreshedFrom, refreshedTo);
                }
                if (creditAmount > 0) {
                    PreparedStatement psd = connection.prepareStatement("INSERT INTO `" + table + "` (uuid, currency, balance) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE balance = balance + VALUES(balance)");
                    psd.setString(1, toUuid.toString());
                    psd.setString(2, currency);
                    psd.setDouble(3, creditAmount);
                    psd.executeUpdate();
                }
                double updatedFrom = getBalance(fromUuid, currency);
                double updatedTo = getBalance(toUuid, currency);
                com.skyblockexp.ezeconomy.storage.TransferResult tr = com.skyblockexp.ezeconomy.storage.TransferResult.success(updatedFrom, updatedTo);

                com.skyblockexp.ezeconomy.api.events.PostTransactionEvent post = new com.skyblockexp.ezeconomy.api.events.PostTransactionEvent(
                    fromUuid, toUuid, java.math.BigDecimal.valueOf(debitAmount), com.skyblockexp.ezeconomy.api.events.TransactionType.TRANSFER,
                    tr.isSuccess(), java.math.BigDecimal.valueOf(fromBefore), java.math.BigDecimal.valueOf(tr.getFromBalance()),
                    java.math.BigDecimal.valueOf(toBefore), java.math.BigDecimal.valueOf(tr.getToBalance())
                );
                try {
                    if (plugin.getServer().isPrimaryThread()) {
                        plugin.getServer().getPluginManager().callEvent(post);
                    } else {
                        plugin.getServer().getScheduler().callSyncMethod(plugin, () -> {
                            plugin.getServer().getPluginManager().callEvent(post);
                            return null;
                        }).get();
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("[EzEconomy] Failed to fire PostTransactionEvent: " + e.getMessage());
                }

                return tr;
            } catch (SQLException e) {
                plugin.getLogger().severe("[EzEconomy] MySQL transfer failed: " + e.getMessage());
                return com.skyblockexp.ezeconomy.storage.TransferResult.failure(fromBefore, toBefore);
            }
        } finally {
            lm.releaseOrdered(ordered, tokens);
        }
    }

    // --- Bank support ---
    private void ensureBankTables() {
        try {
            Statement stmt = connection.createStatement();
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS banks (name VARCHAR(64), currency VARCHAR(32), balance DOUBLE, PRIMARY KEY (name, currency))");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS bank_members (bank VARCHAR(64), uuid VARCHAR(36), owner BOOLEAN, PRIMARY KEY (bank, uuid))");
        } catch (SQLException e) {
            plugin.getLogger().severe("[EzEconomy] MySQL ensureBankTables failed: " + e.getMessage());
        }
    }

    public boolean createBank(String name, UUID owner) {
        com.skyblockexp.ezeconomy.lock.LockManager lm = plugin.getLockManager();
        UUID bankId = UUID.nameUUIDFromBytes(name.getBytes());
        if (lm != null) {
            String token = null;
            try {
                token = lm.acquire(bankId, plugin.getConfig().getLong("redis.ttl-ms", 5000), plugin.getConfig().getLong("redis.retry-ms", 50), plugin.getConfig().getInt("redis.max-attempts", 100));
                if (token != null) {
                    ensureBankTables();
                    try {
                        PreparedStatement ps = connection.prepareStatement("INSERT INTO banks (name, currency, balance) VALUES (?, ?, 0.0)");
                        ps.setString(1, name);
                        ps.setString(2, "dollar"); // default currency
                        ps.executeUpdate();
                        ps = connection.prepareStatement("INSERT INTO bank_members (bank, uuid, owner) VALUES (?, ?, ?)");
                        ps.setString(1, name);
                        ps.setString(2, owner.toString());
                        ps.setBoolean(3, true);
                        ps.executeUpdate();
                        return true;
                    } catch (SQLException e) {
                        return false;
                    }
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                if (token != null) lm.release(bankId, token);
            }
        }
        synchronized (lock) {
            ensureBankTables();
            try {
                PreparedStatement ps = connection.prepareStatement("INSERT INTO banks (name, currency, balance) VALUES (?, ?, 0.0)");
                ps.setString(1, name);
                ps.setString(2, "dollar"); // default currency
                ps.executeUpdate();
                ps = connection.prepareStatement("INSERT INTO bank_members (bank, uuid, owner) VALUES (?, ?, ?)");
                ps.setString(1, name);
                ps.setString(2, owner.toString());
                ps.setBoolean(3, true);
                ps.executeUpdate();
                return true;
            } catch (SQLException e) {
                return false;
            }
        }
    }

    public boolean deleteBank(String name) {
        com.skyblockexp.ezeconomy.lock.LockManager lm = plugin.getLockManager();
        UUID bankId = UUID.nameUUIDFromBytes(name.getBytes());
        if (lm != null) {
            String token = null;
            try {
                token = lm.acquire(bankId, plugin.getConfig().getLong("redis.ttl-ms", 5000), plugin.getConfig().getLong("redis.retry-ms", 50), plugin.getConfig().getInt("redis.max-attempts", 100));
                if (token != null) {
                    ensureBankTables();
                    try {
                        PreparedStatement ps = connection.prepareStatement("DELETE FROM banks WHERE name=?");
                        ps.setString(1, name);
                        int affected = ps.executeUpdate();
                        ps = connection.prepareStatement("DELETE FROM bank_members WHERE bank=?");
                        ps.setString(1, name);
                        ps.executeUpdate();
                        return affected > 0;
                    } catch (SQLException e) {
                        return false;
                    }
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                if (token != null) lm.release(bankId, token);
            }
        }
        synchronized (lock) {
            ensureBankTables();
            try {
                PreparedStatement ps = connection.prepareStatement("DELETE FROM banks WHERE name=?");
                ps.setString(1, name);
                int affected = ps.executeUpdate();
                ps = connection.prepareStatement("DELETE FROM bank_members WHERE bank=?");
                ps.setString(1, name);
                ps.executeUpdate();
                return affected > 0;
            } catch (SQLException e) {
                return false;
            }
        }
    }

    public boolean bankExists(String name) {
        com.skyblockexp.ezeconomy.lock.LockManager lm = plugin.getLockManager();
        UUID bankId = UUID.nameUUIDFromBytes(name.getBytes());
        if (lm != null) {
            String token = null;
            try {
                token = lm.acquire(bankId, plugin.getConfig().getLong("redis.ttl-ms", 5000), plugin.getConfig().getLong("redis.retry-ms", 50), plugin.getConfig().getInt("redis.max-attempts", 100));
                if (token != null) {
                    ensureBankTables();
                    try {
                        PreparedStatement ps = connection.prepareStatement("SELECT name FROM banks WHERE name=?");
                        ps.setString(1, name);
                        ResultSet rs = ps.executeQuery();
                        return rs.next();
                    } catch (SQLException e) {
                        return false;
                    }
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                if (token != null) lm.release(bankId, token);
            }
        }
        synchronized (lock) {
            ensureBankTables();
            try {
                PreparedStatement ps = connection.prepareStatement("SELECT name FROM banks WHERE name=?");
                ps.setString(1, name);
                ResultSet rs = ps.executeQuery();
                return rs.next();
            } catch (SQLException e) {
                return false;
            }
        }
    }

    public double getBankBalance(String name, String currency) {
        com.skyblockexp.ezeconomy.lock.LockManager lm = plugin.getLockManager();
        UUID bankId = UUID.nameUUIDFromBytes(name.getBytes());
        if (lm != null) {
            String token = null;
            try {
                token = lm.acquire(bankId, plugin.getConfig().getLong("redis.ttl-ms", 5000), plugin.getConfig().getLong("redis.retry-ms", 50), plugin.getConfig().getInt("redis.max-attempts", 100));
                if (token != null) {
                    ensureBankTables();
                    try {
                        PreparedStatement ps = connection.prepareStatement("SELECT balance FROM banks WHERE name=? AND currency=?");
                        ps.setString(1, name);
                        ps.setString(2, currency);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) return rs.getDouble(1);
                    } catch (SQLException e) {}
                    return 0.0;
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                if (token != null) lm.release(bankId, token);
            }
        }
        synchronized (lock) {
            ensureBankTables();
            try {
                PreparedStatement ps = connection.prepareStatement("SELECT balance FROM banks WHERE name=? AND currency=?");
                ps.setString(1, name);
                ps.setString(2, currency);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getDouble(1);
            } catch (SQLException e) {}
            return 0.0;
        }
    }

    public void setBankBalance(String name, String currency, double amount) {
        com.skyblockexp.ezeconomy.lock.LockManager lm = plugin.getLockManager();
        UUID bankId = UUID.nameUUIDFromBytes(name.getBytes());
        if (lm != null) {
            String token = null;
            try {
                token = lm.acquire(bankId, plugin.getConfig().getLong("redis.ttl-ms", 5000), plugin.getConfig().getLong("redis.retry-ms", 50), plugin.getConfig().getInt("redis.max-attempts", 100));
                if (token != null) {
                    ensureBankTables();
                    try {
                        PreparedStatement ps = connection.prepareStatement("REPLACE INTO banks (name, currency, balance) VALUES (?, ?, ?)");
                        ps.setString(1, name);
                        ps.setString(2, currency);
                        ps.setDouble(3, amount);
                        ps.executeUpdate();
                        return;
                    } catch (SQLException e) {}
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                if (token != null) lm.release(bankId, token);
            }
        }
        synchronized (lock) {
            ensureBankTables();
            try {
                PreparedStatement ps = connection.prepareStatement("REPLACE INTO banks (name, currency, balance) VALUES (?, ?, ?)");
                ps.setString(1, name);
                ps.setString(2, currency);
                ps.setDouble(3, amount);
                ps.executeUpdate();
            } catch (SQLException e) {}
        }
    }

    @Override
    public boolean tryWithdrawBank(String name, String currency, double amount) {
        com.skyblockexp.ezeconomy.lock.LockManager lm = plugin.getLockManager();
        UUID bankId = UUID.nameUUIDFromBytes(name.getBytes());
        if (lm != null) {
            String token = null;
            try {
                token = lm.acquire(bankId, plugin.getConfig().getLong("redis.ttl-ms", 5000), plugin.getConfig().getLong("redis.retry-ms", 50), plugin.getConfig().getInt("redis.max-attempts", 100));
                if (token != null) {
                    ensureBankTables();
                    try {
                        PreparedStatement sel = connection.prepareStatement("SELECT balance FROM banks WHERE name=? AND currency=?");
                        sel.setString(1, name);
                        sel.setString(2, currency);
                        ResultSet rs = sel.executeQuery();
                        if (!rs.next()) return false;
                        double current = rs.getDouble(1);

                        BankPreTransactionEvent pre = new BankPreTransactionEvent(name, null, BigDecimal.valueOf(amount), TransactionType.BANK_WITHDRAW);
                        if (plugin.getServer().isPrimaryThread()) {
                            plugin.getServer().getPluginManager().callEvent(pre);
                        } else {
                            try {
                                plugin.getServer().getScheduler().callSyncMethod(plugin, () -> {
                                    plugin.getServer().getPluginManager().callEvent(pre);
                                    return null;
                                }).get();
                            } catch (Exception e) {
                                plugin.getLogger().warning("[EzEconomy] Failed to fire BankPreTransactionEvent: " + e.getMessage());
                            }
                        }
                        if (pre.isCancelled()) return false;

                        PreparedStatement ps = connection.prepareStatement(
                            "UPDATE banks SET balance = balance - ? WHERE name=? AND currency=? AND balance >= ?"
                        );
                        ps.setDouble(1, amount);
                        ps.setString(2, name);
                        ps.setString(3, currency);
                        ps.setDouble(4, amount);
                        boolean ok = ps.executeUpdate() > 0;
                        if (ok) {
                            BankPostTransactionEvent post = new BankPostTransactionEvent(name, null, BigDecimal.valueOf(amount), TransactionType.BANK_WITHDRAW, true, BigDecimal.valueOf(current), BigDecimal.valueOf(current - amount));
                            try {
                                plugin.getServer().getScheduler().callSyncMethod(plugin, () -> {
                                    plugin.getServer().getPluginManager().callEvent(post);
                                    return null;
                                }).get();
                            } catch (Exception e) {
                                plugin.getLogger().warning("[EzEconomy] Failed to fire BankPostTransactionEvent: " + e.getMessage());
                            }
                        }
                        return ok;
                    } catch (SQLException e) {
                        plugin.getLogger().severe("[EzEconomy] MySQL tryWithdrawBank failed: " + e.getMessage());
                        return false;
                    }
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                if (token != null) lm.release(bankId, token);
            }
        }
        synchronized (lock) {
            ensureBankTables();
            try {
                PreparedStatement sel = connection.prepareStatement("SELECT balance FROM banks WHERE name=? AND currency=?");
                sel.setString(1, name);
                sel.setString(2, currency);
                ResultSet rs = sel.executeQuery();
                if (!rs.next()) return false;
                double current = rs.getDouble(1);

                BankPreTransactionEvent pre = new BankPreTransactionEvent(name, null, BigDecimal.valueOf(amount), TransactionType.BANK_WITHDRAW);
                if (plugin.getServer().isPrimaryThread()) {
                    plugin.getServer().getPluginManager().callEvent(pre);
                } else {
                    try {
                        plugin.getServer().getScheduler().callSyncMethod(plugin, () -> {
                            plugin.getServer().getPluginManager().callEvent(pre);
                            return null;
                        }).get();
                    } catch (Exception e) {
                        plugin.getLogger().warning("[EzEconomy] Failed to fire BankPreTransactionEvent: " + e.getMessage());
                    }
                }
                if (pre.isCancelled()) return false;

                PreparedStatement ps = connection.prepareStatement(
                    "UPDATE banks SET balance = balance - ? WHERE name=? AND currency=? AND balance >= ?"
                );
                ps.setDouble(1, amount);
                ps.setString(2, name);
                ps.setString(3, currency);
                ps.setDouble(4, amount);
                boolean ok = ps.executeUpdate() > 0;
                if (ok) {
                    BankPostTransactionEvent post = new BankPostTransactionEvent(name, null, BigDecimal.valueOf(amount), TransactionType.BANK_WITHDRAW, true, BigDecimal.valueOf(current), BigDecimal.valueOf(current - amount));
                    try {
                        plugin.getServer().getScheduler().callSyncMethod(plugin, () -> {
                            plugin.getServer().getPluginManager().callEvent(post);
                            return null;
                        }).get();
                    } catch (Exception e) {
                        plugin.getLogger().warning("[EzEconomy] Failed to fire BankPostTransactionEvent: " + e.getMessage());
                    }
                }
                return ok;
            } catch (SQLException e) {
                plugin.getLogger().severe("[EzEconomy] MySQL tryWithdrawBank failed: " + e.getMessage());
                return false;
            }
        }
    }

    @Override
    public void depositBank(String name, String currency, double amount) {
        com.skyblockexp.ezeconomy.lock.LockManager lm = plugin.getLockManager();
        UUID bankId = UUID.nameUUIDFromBytes(name.getBytes());
        if (lm != null) {
            String token = null;
            try {
                token = lm.acquire(bankId, plugin.getConfig().getLong("redis.ttl-ms", 5000), plugin.getConfig().getLong("redis.retry-ms", 50), plugin.getConfig().getInt("redis.max-attempts", 100));
                if (token != null) {
                    ensureBankTables();
                    try {
                        PreparedStatement sel = connection.prepareStatement("SELECT balance FROM banks WHERE name=? AND currency=?");
                        sel.setString(1, name);
                        sel.setString(2, currency);
                        ResultSet rs = sel.executeQuery();
                        double before = 0.0;
                        if (rs.next()) before = rs.getDouble(1);

                        BankPreTransactionEvent pre = new BankPreTransactionEvent(name, null, BigDecimal.valueOf(amount), TransactionType.BANK_DEPOSIT);
                        try {
                            plugin.getServer().getScheduler().callSyncMethod(plugin, () -> {
                                plugin.getServer().getPluginManager().callEvent(pre);
                                return null;
                            }).get();
                        } catch (Exception e) {
                            plugin.getLogger().warning("[EzEconomy] Failed to fire BankPreTransactionEvent: " + e.getMessage());
                        }
                        if (pre.isCancelled()) return;

                        PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO banks (name, currency, balance) VALUES (?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE balance = balance + VALUES(balance)"
                        );
                        ps.setString(1, name);
                        ps.setString(2, currency);
                        ps.setDouble(3, amount);
                        ps.executeUpdate();

                        BankPostTransactionEvent post = new BankPostTransactionEvent(name, null, BigDecimal.valueOf(amount), TransactionType.BANK_DEPOSIT, true, BigDecimal.valueOf(before), BigDecimal.valueOf(before + amount));
                        if (plugin.getServer().isPrimaryThread()) {
                            plugin.getServer().getPluginManager().callEvent(post);
                        } else {
                            try {
                                plugin.getServer().getScheduler().callSyncMethod(plugin, () -> {
                                    plugin.getServer().getPluginManager().callEvent(post);
                                    return null;
                                }).get();
                            } catch (Exception e) {
                                plugin.getLogger().warning("[EzEconomy] Failed to fire BankPostTransactionEvent: " + e.getMessage());
                            }
                        }
                        return;
                    } catch (SQLException e) {
                        plugin.getLogger().severe("[EzEconomy] MySQL depositBank failed: " + e.getMessage());
                        return;
                    }
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                if (token != null) lm.release(bankId, token);
            }
        }
        synchronized (lock) {
            ensureBankTables();
            try {
                PreparedStatement sel = connection.prepareStatement("SELECT balance FROM banks WHERE name=? AND currency=?");
                sel.setString(1, name);
                sel.setString(2, currency);
                ResultSet rs = sel.executeQuery();
                double before = 0.0;
                if (rs.next()) before = rs.getDouble(1);

                BankPreTransactionEvent pre = new BankPreTransactionEvent(name, null, BigDecimal.valueOf(amount), TransactionType.BANK_DEPOSIT);
                try {
                    plugin.getServer().getScheduler().callSyncMethod(plugin, () -> {
                        plugin.getServer().getPluginManager().callEvent(pre);
                        return null;
                    }).get();
                } catch (Exception e) {
                    plugin.getLogger().warning("[EzEconomy] Failed to fire BankPreTransactionEvent: " + e.getMessage());
                }
                if (pre.isCancelled()) return;

                PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO banks (name, currency, balance) VALUES (?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE balance = balance + VALUES(balance)"
                );
                ps.setString(1, name);
                ps.setString(2, currency);
                ps.setDouble(3, amount);
                ps.executeUpdate();

                BankPostTransactionEvent post = new BankPostTransactionEvent(name, null, BigDecimal.valueOf(amount), TransactionType.BANK_DEPOSIT, true, BigDecimal.valueOf(before), BigDecimal.valueOf(before + amount));
                if (plugin.getServer().isPrimaryThread()) {
                    plugin.getServer().getPluginManager().callEvent(post);
                } else {
                    try {
                        plugin.getServer().getScheduler().callSyncMethod(plugin, () -> {
                            plugin.getServer().getPluginManager().callEvent(post);
                            return null;
                        }).get();
                    } catch (Exception e) {
                        plugin.getLogger().warning("[EzEconomy] Failed to fire BankPostTransactionEvent: " + e.getMessage());
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("[EzEconomy] MySQL depositBank failed: " + e.getMessage());
            }
        }
    }

    public Set<String> getBanks() {
        com.skyblockexp.ezeconomy.lock.LockManager lm = plugin.getLockManager();
        // bank list is global; use local lock for list operations to avoid distributed overhead
        synchronized (lock) {
            ensureBankTables();
            Set<String> set = ConcurrentHashMap.newKeySet();
            try {
                PreparedStatement ps = connection.prepareStatement("SELECT name FROM banks");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) set.add(rs.getString(1));
            } catch (SQLException e) {}
            return set;
        }
    }

    public boolean isBankOwner(String name, UUID uuid) {
        com.skyblockexp.ezeconomy.lock.LockManager lm = plugin.getLockManager();
        UUID bankId = UUID.nameUUIDFromBytes(name.getBytes());
        if (lm != null) {
            String token = null;
            try {
                token = lm.acquire(bankId, plugin.getConfig().getLong("redis.ttl-ms", 5000), plugin.getConfig().getLong("redis.retry-ms", 50), plugin.getConfig().getInt("redis.max-attempts", 100));
                if (token != null) {
                    ensureBankTables();
                    try {
                        PreparedStatement ps = connection.prepareStatement("SELECT owner FROM bank_members WHERE bank=? AND uuid=?");
                        ps.setString(1, name);
                        ps.setString(2, uuid.toString());
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) return rs.getBoolean(1);
                    } catch (SQLException e) {}
                    return false;
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                if (token != null) lm.release(bankId, token);
            }
        }
        synchronized (lock) {
            ensureBankTables();
            try {
                PreparedStatement ps = connection.prepareStatement("SELECT owner FROM bank_members WHERE bank=? AND uuid=?");
                ps.setString(1, name);
                ps.setString(2, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getBoolean(1);
            } catch (SQLException e) {}
            return false;
        }
    }

    public boolean isBankMember(String name, UUID uuid) {
        com.skyblockexp.ezeconomy.lock.LockManager lm = plugin.getLockManager();
        UUID bankId = UUID.nameUUIDFromBytes(name.getBytes());
        if (lm != null) {
            String token = null;
            try {
                token = lm.acquire(bankId, plugin.getConfig().getLong("redis.ttl-ms", 5000), plugin.getConfig().getLong("redis.retry-ms", 50), plugin.getConfig().getInt("redis.max-attempts", 100));
                if (token != null) {
                    ensureBankTables();
                    try {
                        PreparedStatement ps = connection.prepareStatement("SELECT uuid FROM bank_members WHERE bank=? AND uuid=?");
                        ps.setString(1, name);
                        ps.setString(2, uuid.toString());
                        ResultSet rs = ps.executeQuery();
                        return rs.next();
                    } catch (SQLException e) {}
                    return false;
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                if (token != null) lm.release(bankId, token);
            }
        }
        synchronized (lock) {
            ensureBankTables();
            try {
                PreparedStatement ps = connection.prepareStatement("SELECT uuid FROM bank_members WHERE bank=? AND uuid=?");
                ps.setString(1, name);
                ps.setString(2, uuid.toString());
                ResultSet rs = ps.executeQuery();
                return rs.next();
            } catch (SQLException e) {}
            return false;
        }
    }

    public boolean addBankMember(String name, UUID uuid) {
        com.skyblockexp.ezeconomy.lock.LockManager lm = plugin.getLockManager();
        UUID bankId = UUID.nameUUIDFromBytes(name.getBytes());
        if (lm != null) {
            String token = null;
            try {
                token = lm.acquire(bankId, plugin.getConfig().getLong("redis.ttl-ms", 5000), plugin.getConfig().getLong("redis.retry-ms", 50), plugin.getConfig().getInt("redis.max-attempts", 100));
                if (token != null) {
                    ensureBankTables();
                    if (isBankMember(name, uuid)) return false;
                    try {
                        PreparedStatement ps = connection.prepareStatement("INSERT INTO bank_members (bank, uuid, owner) VALUES (?, ?, ?)");
                        ps.setString(1, name);
                        ps.setString(2, uuid.toString());
                        ps.setBoolean(3, false);
                        ps.executeUpdate();
                        return true;
                    } catch (SQLException e) { return false; }
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                if (token != null) lm.release(bankId, token);
            }
        }
        synchronized (lock) {
            ensureBankTables();
            if (isBankMember(name, uuid)) return false;
            try {
                PreparedStatement ps = connection.prepareStatement("INSERT INTO bank_members (bank, uuid, owner) VALUES (?, ?, ?)");
                ps.setString(1, name);
                ps.setString(2, uuid.toString());
                ps.setBoolean(3, false);
                ps.executeUpdate();
                return true;
            } catch (SQLException e) { return false; }
        }
    }

    public boolean removeBankMember(String name, UUID uuid) {
        com.skyblockexp.ezeconomy.lock.LockManager lm = plugin.getLockManager();
        UUID bankId = UUID.nameUUIDFromBytes(name.getBytes());
        if (lm != null) {
            String token = null;
            try {
                token = lm.acquire(bankId, plugin.getConfig().getLong("redis.ttl-ms", 5000), plugin.getConfig().getLong("redis.retry-ms", 50), plugin.getConfig().getInt("redis.max-attempts", 100));
                if (token != null) {
                    ensureBankTables();
                    try {
                        PreparedStatement ps = connection.prepareStatement("DELETE FROM bank_members WHERE bank=? AND uuid=?");
                        ps.setString(1, name);
                        ps.setString(2, uuid.toString());
                        int affected = ps.executeUpdate();
                        return affected > 0;
                    } catch (SQLException e) { return false; }
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                if (token != null) lm.release(bankId, token);
            }
        }
        synchronized (lock) {
            ensureBankTables();
            try {
                PreparedStatement ps = connection.prepareStatement("DELETE FROM bank_members WHERE bank=? AND uuid=?");
                ps.setString(1, name);
                ps.setString(2, uuid.toString());
                int affected = ps.executeUpdate();
                return affected > 0;
            } catch (SQLException e) { return false; }
        }
    }

    public Set<UUID> getBankMembers(String name) {
        com.skyblockexp.ezeconomy.lock.LockManager lm = plugin.getLockManager();
        UUID bankId = UUID.nameUUIDFromBytes(name.getBytes());
        if (lm != null) {
            String token = null;
            try {
                token = lm.acquire(bankId, plugin.getConfig().getLong("redis.ttl-ms", 5000), plugin.getConfig().getLong("redis.retry-ms", 50), plugin.getConfig().getInt("redis.max-attempts", 100));
                if (token != null) {
                    ensureBankTables();
                    Set<UUID> set = ConcurrentHashMap.newKeySet();
                    try {
                        PreparedStatement ps = connection.prepareStatement("SELECT uuid FROM bank_members WHERE bank=?");
                        ps.setString(1, name);
                        ResultSet rs = ps.executeQuery();
                        while (rs.next()) {
                            try { set.add(UUID.fromString(rs.getString(1))); } catch (IllegalArgumentException ignored) {}
                        }
                    } catch (SQLException e) {}
                    return set;
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                if (token != null) lm.release(bankId, token);
            }
        }
        synchronized (lock) {
            ensureBankTables();
            Set<UUID> set = ConcurrentHashMap.newKeySet();
            try {
                PreparedStatement ps = connection.prepareStatement("SELECT uuid FROM bank_members WHERE bank=?");
                ps.setString(1, name);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    try { set.add(UUID.fromString(rs.getString(1))); } catch (IllegalArgumentException ignored) {}
                }
            } catch (SQLException e) {}
            return set;
        }
    }

    @Override
    public void logTransaction(com.skyblockexp.ezeconomy.api.storage.models.Transaction tx) {
        synchronized (lock) {
            try {
                String sql = "INSERT INTO transactions (uuid, currency, amount, timestamp) VALUES (?, ?, ?, ?)";
                PreparedStatement ps = connection.prepareStatement(sql);
                ps.setString(1, tx.getUuid().toString());
                ps.setString(2, tx.getCurrency());
                ps.setDouble(3, tx.getAmount());
                ps.setLong(4, tx.getTimestamp());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("[EzEconomy] MySQL logTransaction failed: " + e.getMessage());
            }
        }
    }

    /**
     * Removes balances for UUIDs that do not resolve to a known player.
     * @return Set of removed UUIDs as strings
     */
    public java.util.Set<String> cleanupOrphanedPlayers() {
        java.util.Set<String> removed = new java.util.HashSet<>();
        synchronized (lock) {
            try {
                PreparedStatement ps = connection.prepareStatement("SELECT uuid FROM `" + table + "`");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String uuidStr = rs.getString(1);
                    try {
                        java.util.UUID uuid = java.util.UUID.fromString(uuidStr);
                        org.bukkit.OfflinePlayer player = org.bukkit.Bukkit.getOfflinePlayer(uuid);
                        if (player == null || player.getName() == null) {
                            PreparedStatement del = connection.prepareStatement("DELETE FROM `" + table + "` WHERE uuid=?");
                            del.setString(1, uuidStr);
                            del.executeUpdate();
                            removed.add(uuidStr);
                        }
                    } catch (IllegalArgumentException ignored) {}
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("[EzEconomy] MySQL cleanupOrphanedPlayers failed: " + e.getMessage());
            }
        }
        return removed;
    }

    /**
     * Returns the set of orphaned UUIDs that would be deleted by cleanup.
     */
    public java.util.Set<String> previewOrphanedPlayers() {
        java.util.Set<String> orphaned = new java.util.HashSet<>();
        synchronized (lock) {
            try {
                PreparedStatement ps = connection.prepareStatement("SELECT uuid FROM `" + table + "`");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String uuidStr = rs.getString(1);
                    try {
                        java.util.UUID uuid = java.util.UUID.fromString(uuidStr);
                        org.bukkit.OfflinePlayer player = org.bukkit.Bukkit.getOfflinePlayer(uuid);
                        if (player == null || player.getName() == null) {
                            orphaned.add(uuidStr);
                        }
                    } catch (IllegalArgumentException ignored) {}
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("[EzEconomy] MySQL previewOrphanedPlayers failed: " + e.getMessage());
            }
        }
        return orphaned;
    }
}
