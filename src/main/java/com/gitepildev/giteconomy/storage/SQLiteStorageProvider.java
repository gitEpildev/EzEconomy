package com.gitepildev.giteconomy.storage;

import com.gitepildev.giteconomy.core.GitEconomyPlugin;
import com.gitepildev.giteconomy.api.storage.StorageProvider;
import com.gitepildev.giteconomy.api.storage.exceptions.StorageInitException;
import com.gitepildev.giteconomy.api.storage.exceptions.StorageLoadException;
import com.gitepildev.giteconomy.api.storage.exceptions.StorageSaveException;
import com.gitepildev.giteconomy.api.storage.models.Transaction;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * SQLite implementation of the StorageProvider interface for GitEconomy.
 * Handles player balances using a local SQLite database.
 * Thread-safe and ready for open-source use.
 *
 * <p>Usage: Instantiate with plugin and config, or call init() if using the default constructor.</p>
 */
public class SQLiteStorageProvider implements StorageProvider {
    // --- Fields ---
    private String fileName;
    private final GitEconomyPlugin plugin;
    private Connection connection;
    private String table;
    private final Object lock = new Object();
    private final YamlConfiguration dbConfig;

    // --- Constructors ---
    /**
     * Default constructor for legacy compatibility. Not recommended for production.
     */
    public SQLiteStorageProvider(GitEconomyPlugin plugin) {
        this.plugin = plugin;
        this.dbConfig = null;
        this.fileName = "economy.db";
        this.table = "balances";
    }

    /**
     * Main constructor. Reads config and initializes tables if needed.
     * Throws RuntimeException if initialization fails.
     * @param plugin GitEconomy plugin instance
     * @param dbConfig YAML configuration for SQLite
     */
    public SQLiteStorageProvider(GitEconomyPlugin plugin, YamlConfiguration dbConfig) {
        this.plugin = plugin;
        this.dbConfig = dbConfig;
        if (dbConfig == null) throw new IllegalArgumentException("SQLite config is missing!");
        this.fileName = dbConfig.getString("sqlite.file", "giteconomy.db");
        this.table = dbConfig.getString("sqlite.table", "balances");
        try {
            File file = new File(plugin.getDataFolder(), this.fileName);
            connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
            Statement stmt = connection.createStatement();
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS '" + table + "' (uuid TEXT, currency TEXT, balance DOUBLE, PRIMARY KEY (uuid, currency))");
            // Optional players table to persist last-known name/displayName
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS players (uuid TEXT PRIMARY KEY, name TEXT, displayName TEXT)");
        } catch (SQLException e) {
            plugin.getLogger().severe("SQLite connection failed: " + e.getMessage());
            throw new RuntimeException("Failed to initialize SQLiteStorageProvider", e);
        }
    }

    // --- Public API: StorageProvider interface ---
    @Override
    public java.util.List<Transaction> getTransactions(java.util.UUID uuid, String currency) {
        java.util.List<Transaction> transactions = new java.util.ArrayList<>();
        synchronized (lock) {
            try {
                // Assumes a table: transactions(uuid TEXT, currency TEXT, amount DOUBLE, timestamp INTEGER)
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
                plugin.getLogger().severe("[GitEconomy] SQLite getTransactions failed for " + uuid + " (" + currency + "): " + e.getMessage());
            }
        }
        return transactions;
    }

    /**
     * Initializes the SQLite connection and tables. Call before use if not using the config constructor.
     * @throws StorageInitException if the JDBC driver is missing or connection fails
     */
    public void init() throws StorageInitException {
        try {
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + new File(plugin.getDataFolder(), fileName).getAbsolutePath());
            createTableIfNotExists();
        } catch (ClassNotFoundException e) {
            throw new StorageInitException("SQLite JDBC driver not found.", e);
        } catch (SQLException e) {
            throw new StorageInitException("Failed to connect to the database.", e);
        }
    }

    /**
     * Creates the default economy table if it does not exist.
     */
    private void createTableIfNotExists() throws StorageInitException {
        String sql = "CREATE TABLE IF NOT EXISTS economy (" +
                "uuid TEXT PRIMARY KEY NOT NULL," +
                "balance REAL DEFAULT 0," +
                "last_updated INTEGER" +
                ");";
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            throw new StorageInitException("Failed to create table in the database.", e);
        }
    }

    /**
     * Loads all player balances from the economy table. No-op unless you add caching.
     * @throws StorageLoadException if loading fails
     */
    public void load() throws StorageLoadException {
        // No in-memory cache, so nothing to load. If you add caching, load from DB here.
    }

    /**
     * Saves all in-memory data to the database. No-op unless you add caching.
     * @throws StorageSaveException if saving fails
     */
    public void save() throws StorageSaveException {
        // No in-memory cache, so nothing to save. If you add caching, flush to DB here.
    }
    // Optionally, override equals/hashCode/toString if needed for provider management
    @Override
    public String toString() {
        return "SQLiteStorageProvider{" +
                "fileName='" + fileName + '\'' +
                ", table='" + table + '\'' +
                '}';
    }

    /**
     * Closes the SQLite connection.
     */
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to close the database connection.", e);
            }
        }
    }

    @Override
    public double getBalance(UUID uuid, String currency) {
        com.gitepildev.giteconomy.lock.LockManager lm = plugin.getLockManager();
        if (lm != null) {
            String token = null;
            try {
                token = lm.acquire(uuid, plugin.getConfig().getLong("redis.ttl-ms", 5000), plugin.getConfig().getLong("redis.retry-ms", 50), plugin.getConfig().getInt("redis.max-attempts", 100));
                if (token != null) {
                    try {
                        PreparedStatement ps = connection.prepareStatement("SELECT balance FROM '" + table + "' WHERE uuid=? AND currency=?");
                        ps.setString(1, uuid.toString());
                        ps.setString(2, currency);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) return rs.getDouble(1);
                    } catch (SQLException e) {
                        plugin.getLogger().severe("[GitEconomy] SQLite getBalance failed for " + uuid + " (" + currency + "): " + e.getMessage());
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
                PreparedStatement ps = connection.prepareStatement("SELECT balance FROM '" + table + "' WHERE uuid=? AND currency=?");
                ps.setString(1, uuid.toString());
                ps.setString(2, currency);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getDouble(1);
            } catch (SQLException e) {
                plugin.getLogger().severe("[GitEconomy] SQLite getBalance failed for " + uuid + " (" + currency + "): " + e.getMessage());
            }
            return 0.0;
        }
    }

    @Override
    public com.gitepildev.giteconomy.dto.EconomyPlayer getPlayer(UUID uuid) {
        com.gitepildev.giteconomy.lock.LockManager lm = plugin.getLockManager();
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
                            return new com.gitepildev.giteconomy.dto.EconomyPlayer(uuid, name, display);
                        }
                    } catch (Exception ignored) {}
                    org.bukkit.OfflinePlayer of = plugin.getServer().getOfflinePlayer(uuid);
                    String name = of != null && of.getName() != null ? of.getName() : uuid.toString();
                    String display = (of instanceof org.bukkit.entity.Player) ? ((org.bukkit.entity.Player) of).getDisplayName() : name;
                    return new com.gitepildev.giteconomy.dto.EconomyPlayer(uuid, name, display);
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
                    return new com.gitepildev.giteconomy.dto.EconomyPlayer(uuid, name, display);
                }
            } catch (Exception ignored) {}
            org.bukkit.OfflinePlayer of = plugin.getServer().getOfflinePlayer(uuid);
            String name = of != null && of.getName() != null ? of.getName() : uuid.toString();
            String display = (of instanceof org.bukkit.entity.Player) ? ((org.bukkit.entity.Player) of).getDisplayName() : name;
            return new com.gitepildev.giteconomy.dto.EconomyPlayer(uuid, name, display);
        }
    }

    @Override
    public boolean playerExists(UUID uuid) {
        com.gitepildev.giteconomy.lock.LockManager lm = plugin.getLockManager();
        if (lm != null) {
            String token = null;
            try {
                token = lm.acquire(uuid, plugin.getConfig().getLong("redis.ttl-ms", 5000), plugin.getConfig().getLong("redis.retry-ms", 50), plugin.getConfig().getInt("redis.max-attempts", 100));
                if (token != null) {
                    try {
                        PreparedStatement ps = connection.prepareStatement("SELECT 1 FROM '" + table + "' WHERE uuid=? LIMIT 1");
                        ps.setString(1, uuid.toString());
                        ResultSet rs = ps.executeQuery();
                        return rs.next();
                    } catch (SQLException e) {
                        plugin.getLogger().severe("[GitEconomy] SQLite playerExists failed for " + uuid + ": " + e.getMessage());
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
                PreparedStatement ps = connection.prepareStatement("SELECT 1 FROM '" + table + "' WHERE uuid=? LIMIT 1");
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                return rs.next();
            } catch (SQLException e) {
                plugin.getLogger().severe("[GitEconomy] SQLite playerExists failed for " + uuid + ": " + e.getMessage());
                return false;
            }
        }
    }

    @Override
    public void setBalance(UUID uuid, String currency, double amount) {
        com.gitepildev.giteconomy.lock.LockManager lm = plugin.getLockManager();
        if (lm != null) {
            String token = null;
            try {
                token = lm.acquire(uuid, plugin.getConfig().getLong("redis.ttl-ms", 5000), plugin.getConfig().getLong("redis.retry-ms", 50), plugin.getConfig().getInt("redis.max-attempts", 100));
                if (token != null) {
                    try {
                        PreparedStatement ps = connection.prepareStatement("REPLACE INTO '" + table + "' (uuid, currency, balance) VALUES (?, ?, ?)");
                        ps.setString(1, uuid.toString());
                        ps.setString(2, currency);
                        ps.setDouble(3, amount);
                        ps.executeUpdate();
                        try {
                            PreparedStatement ps2 = connection.prepareStatement("REPLACE INTO players (uuid, name, displayName) VALUES (?, ?, ?)");
                            org.bukkit.OfflinePlayer of = plugin.getServer().getOfflinePlayer(uuid);
                            String name = of != null && of.getName() != null ? of.getName() : uuid.toString();
                            String display = (of instanceof org.bukkit.entity.Player) ? ((org.bukkit.entity.Player) of).getDisplayName() : name;
                            ps2.setString(1, uuid.toString());
                            ps2.setString(2, name);
                            ps2.setString(3, display);
                            ps2.executeUpdate();
                        } catch (Exception ignored) {}
                        return;
                    } catch (SQLException e) {
                        plugin.getLogger().severe("[GitEconomy] SQLite setBalance failed for " + uuid + " (" + currency + "): " + e.getMessage());
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
                PreparedStatement ps = connection.prepareStatement("REPLACE INTO '" + table + "' (uuid, currency, balance) VALUES (?, ?, ?)");
                ps.setString(1, uuid.toString());
                ps.setString(2, currency);
                ps.setDouble(3, amount);
                ps.executeUpdate();
            try {
                PreparedStatement ps2 = connection.prepareStatement("REPLACE INTO players (uuid, name, displayName) VALUES (?, ?, ?)");
                org.bukkit.OfflinePlayer of = plugin.getServer().getOfflinePlayer(uuid);
                String name = of != null && of.getName() != null ? of.getName() : uuid.toString();
                String display = (of instanceof org.bukkit.entity.Player) ? ((org.bukkit.entity.Player) of).getDisplayName() : name;
                ps2.setString(1, uuid.toString());
                ps2.setString(2, name);
                ps2.setString(3, display);
                ps2.executeUpdate();
            } catch (Exception ignored) {}
            } catch (SQLException e) {
                plugin.getLogger().severe("[GitEconomy] SQLite setBalance failed for " + uuid + " (" + currency + "): " + e.getMessage());
            }
        }
    }

    @Override
    public boolean tryWithdraw(UUID uuid, String currency, double amount) {
        com.gitepildev.giteconomy.lock.LockManager lm = plugin.getLockManager();
        if (lm != null) {
            String token = null;
            try {
                token = lm.acquire(uuid, plugin.getConfig().getLong("redis.ttl-ms", 5000), plugin.getConfig().getLong("redis.retry-ms", 50), plugin.getConfig().getInt("redis.max-attempts", 100));
                if (token != null) {
                    try {
                        PreparedStatement ps = connection.prepareStatement(
                            "UPDATE '" + table + "' SET balance = balance - ? WHERE uuid=? AND currency=? AND balance >= ?"
                        );
                        ps.setDouble(1, amount);
                        ps.setString(2, uuid.toString());
                        ps.setString(3, currency);
                        ps.setDouble(4, amount);
                        return ps.executeUpdate() > 0;
                    } catch (SQLException e) {
                        plugin.getLogger().severe("[GitEconomy] SQLite tryWithdraw failed for " + uuid + " (" + currency + "): " + e.getMessage());
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
                    "UPDATE '" + table + "' SET balance = balance - ? WHERE uuid=? AND currency=? AND balance >= ?"
                );
                ps.setDouble(1, amount);
                ps.setString(2, uuid.toString());
                ps.setString(3, currency);
                ps.setDouble(4, amount);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().severe("[GitEconomy] SQLite tryWithdraw failed for " + uuid + " (" + currency + "): " + e.getMessage());
                return false;
            }
        }
    }

    @Override
    public void deposit(UUID uuid, String currency, double amount) {
        com.gitepildev.giteconomy.lock.LockManager lm = plugin.getLockManager();
        if (lm != null) {
            String token = null;
            try {
                token = lm.acquire(uuid, plugin.getConfig().getLong("redis.ttl-ms", 5000), plugin.getConfig().getLong("redis.retry-ms", 50), plugin.getConfig().getInt("redis.max-attempts", 100));
                if (token != null) {
                    try {
                        PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO '" + table + "' (uuid, currency, balance) VALUES (?, ?, ?) " +
                                "ON CONFLICT(uuid, currency) DO UPDATE SET balance = balance + excluded.balance"
                        );
                        ps.setString(1, uuid.toString());
                        ps.setString(2, currency);
                        ps.setDouble(3, amount);
                        ps.executeUpdate();
                    try {
                        PreparedStatement ps2 = connection.prepareStatement("REPLACE INTO players (uuid, name, displayName) VALUES (?, ?, ?)");
                        org.bukkit.OfflinePlayer of = plugin.getServer().getOfflinePlayer(uuid);
                        String name = of != null && of.getName() != null ? of.getName() : uuid.toString();
                        String display = (of instanceof org.bukkit.entity.Player) ? ((org.bukkit.entity.Player) of).getDisplayName() : name;
                        ps2.setString(1, uuid.toString());
                        ps2.setString(2, name);
                        ps2.setString(3, display);
                        ps2.executeUpdate();
                    } catch (Exception ignored) {}
                        return;
                    } catch (SQLException e) {
                        plugin.getLogger().severe("[GitEconomy] SQLite deposit failed for " + uuid + " (" + currency + "): " + e.getMessage());
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
                    "INSERT INTO '" + table + "' (uuid, currency, balance) VALUES (?, ?, ?) " +
                        "ON CONFLICT(uuid, currency) DO UPDATE SET balance = balance + excluded.balance"
                );
                ps.setString(1, uuid.toString());
                ps.setString(2, currency);
                ps.setDouble(3, amount);
                ps.executeUpdate();
            try {
                PreparedStatement ps2 = connection.prepareStatement("REPLACE INTO players (uuid, name, displayName) VALUES (?, ?, ?)");
                org.bukkit.OfflinePlayer of = plugin.getServer().getOfflinePlayer(uuid);
                String name = of != null && of.getName() != null ? of.getName() : uuid.toString();
                String display = (of instanceof org.bukkit.entity.Player) ? ((org.bukkit.entity.Player) of).getDisplayName() : name;
                ps2.setString(1, uuid.toString());
                ps2.setString(2, name);
                ps2.setString(3, display);
                ps2.executeUpdate();
            } catch (Exception ignored) {}
            } catch (SQLException e) {
                plugin.getLogger().severe("[GitEconomy] SQLite deposit failed for " + uuid + " (" + currency + "): " + e.getMessage());
            }
        }
    }

    @Override
    public Map<UUID, Double> getAllBalances(String currency) {
        Map<UUID, Double> map = new HashMap<>();
        synchronized (lock) {
            try {
                PreparedStatement ps = connection.prepareStatement("SELECT uuid, balance FROM '" + table + "' WHERE currency=?");
                ps.setString(1, currency);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    map.put(UUID.fromString(rs.getString(1)), rs.getDouble(2));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("[GitEconomy] SQLite getAllBalances failed: " + e.getMessage());
            }
        }
        return map;
    }

    @Override
    public com.gitepildev.giteconomy.storage.TransferResult transfer(UUID fromUuid, UUID toUuid, String currency, double debitAmount, double creditAmount) {
        com.gitepildev.giteconomy.lock.LockManager lm = plugin.getLockManager();
        if (lm == null) {
            double fromBefore = getBalance(fromUuid, currency);
            double toBefore = getBalance(toUuid, currency);
            com.gitepildev.giteconomy.api.events.PreTransactionEvent pre = new com.gitepildev.giteconomy.api.events.PreTransactionEvent(fromUuid, toUuid, java.math.BigDecimal.valueOf(debitAmount), com.gitepildev.giteconomy.api.events.TransactionType.TRANSFER);
            try {
                plugin.getServer().getScheduler().callSyncMethod(plugin, () -> {
                    plugin.getServer().getPluginManager().callEvent(pre);
                    return null;
                }).get();
            } catch (Exception e) {
                plugin.getLogger().warning("[GitEconomy] Failed to fire PreTransactionEvent: " + e.getMessage());
            }
            if (pre.isCancelled()) {
                return com.gitepildev.giteconomy.storage.TransferResult.failure(fromBefore, toBefore);
            }
            com.gitepildev.giteconomy.storage.TransferResult result = StorageProvider.super.transfer(fromUuid, toUuid, currency, debitAmount, creditAmount);
            com.gitepildev.giteconomy.api.events.PostTransactionEvent post = new com.gitepildev.giteconomy.api.events.PostTransactionEvent(
                fromUuid, toUuid, java.math.BigDecimal.valueOf(debitAmount), com.gitepildev.giteconomy.api.events.TransactionType.TRANSFER,
                result.isSuccess(), java.math.BigDecimal.valueOf(fromBefore), java.math.BigDecimal.valueOf(result.getFromBalance()),
                java.math.BigDecimal.valueOf(toBefore), java.math.BigDecimal.valueOf(result.getToBalance())
            );
            try {
                plugin.getServer().getScheduler().callSyncMethod(plugin, () -> {
                    plugin.getServer().getPluginManager().callEvent(post);
                    return null;
                }).get();
            } catch (Exception e) {
                plugin.getLogger().warning("[GitEconomy] Failed to fire PostTransactionEvent: " + e.getMessage());
            }
            return result;
        }

        UUID[] ordered = new UUID[]{fromUuid, toUuid};
        if (fromUuid.compareTo(toUuid) > 0) ordered = new UUID[]{toUuid, fromUuid};
        String[] tokens = null;
        try {
            tokens = lm.acquireOrdered(ordered, plugin.getConfig().getLong("redis.ttl-ms", 5000), plugin.getConfig().getLong("redis.retry-ms", 50), plugin.getConfig().getInt("redis.max-attempts", 100));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        if (tokens == null) {
            return StorageProvider.super.transfer(fromUuid, toUuid, currency, debitAmount, creditAmount);
        }

        try {
            double fromBefore;
            double toBefore;
            try {
                PreparedStatement ps = connection.prepareStatement("SELECT balance FROM '" + table + "' WHERE uuid=? AND currency=?");
                ps.setString(1, fromUuid.toString());
                ps.setString(2, currency);
                ResultSet rs = ps.executeQuery();
                fromBefore = rs.next() ? rs.getDouble(1) : 0.0;
                PreparedStatement ps2 = connection.prepareStatement("SELECT balance FROM '" + table + "' WHERE uuid=? AND currency=?");
                ps2.setString(1, toUuid.toString());
                ps2.setString(2, currency);
                ResultSet rs2 = ps2.executeQuery();
                toBefore = rs2.next() ? rs2.getDouble(1) : 0.0;
            } catch (SQLException e) {
                plugin.getLogger().severe("[GitEconomy] SQLite transfer balance read failed: " + e.getMessage());
                return com.gitepildev.giteconomy.storage.TransferResult.failure(0.0, 0.0);
            }

            com.gitepildev.giteconomy.api.events.PreTransactionEvent pre = new com.gitepildev.giteconomy.api.events.PreTransactionEvent(fromUuid, toUuid, java.math.BigDecimal.valueOf(debitAmount), com.gitepildev.giteconomy.api.events.TransactionType.TRANSFER);
            try {
                plugin.getServer().getScheduler().callSyncMethod(plugin, () -> {
                    plugin.getServer().getPluginManager().callEvent(pre);
                    return null;
                }).get();
            } catch (Exception e) {
                plugin.getLogger().warning("[GitEconomy] Failed to fire PreTransactionEvent: " + e.getMessage());
            }
            if (pre.isCancelled()) return com.gitepildev.giteconomy.storage.TransferResult.failure(fromBefore, toBefore);

            try {
                PreparedStatement psw = connection.prepareStatement(
                    "UPDATE '" + table + "' SET balance = balance - ? WHERE uuid=? AND currency=? AND balance >= ?"
                );
                psw.setDouble(1, debitAmount);
                psw.setString(2, fromUuid.toString());
                psw.setString(3, currency);
                psw.setDouble(4, debitAmount);
                int updated = psw.executeUpdate();
                if (updated <= 0) {
                    double refreshedFrom = getBalance(fromUuid, currency);
                    double refreshedTo = getBalance(toUuid, currency);
                    return com.gitepildev.giteconomy.storage.TransferResult.failure(refreshedFrom, refreshedTo);
                }
                if (creditAmount > 0) {
                    PreparedStatement psd = connection.prepareStatement("INSERT INTO '" + table + "' (uuid, currency, balance) VALUES (?, ?, ?) ON CONFLICT(uuid, currency) DO UPDATE SET balance = balance + excluded.balance");
                    psd.setString(1, toUuid.toString());
                    psd.setString(2, currency);
                    psd.setDouble(3, creditAmount);
                    psd.executeUpdate();
                }
                double updatedFrom = getBalance(fromUuid, currency);
                double updatedTo = getBalance(toUuid, currency);
                com.gitepildev.giteconomy.storage.TransferResult tr = com.gitepildev.giteconomy.storage.TransferResult.success(updatedFrom, updatedTo);

                com.gitepildev.giteconomy.api.events.PostTransactionEvent post = new com.gitepildev.giteconomy.api.events.PostTransactionEvent(
                    fromUuid, toUuid, java.math.BigDecimal.valueOf(debitAmount), com.gitepildev.giteconomy.api.events.TransactionType.TRANSFER,
                    tr.isSuccess(), java.math.BigDecimal.valueOf(fromBefore), java.math.BigDecimal.valueOf(tr.getFromBalance()),
                    java.math.BigDecimal.valueOf(toBefore), java.math.BigDecimal.valueOf(tr.getToBalance())
                );
                try {
                    plugin.getServer().getScheduler().callSyncMethod(plugin, () -> {
                        plugin.getServer().getPluginManager().callEvent(post);
                        return null;
                    }).get();
                } catch (Exception e) {
                    plugin.getLogger().warning("[GitEconomy] Failed to fire PostTransactionEvent: " + e.getMessage());
                }
                return tr;
            } catch (SQLException e) {
                plugin.getLogger().severe("[GitEconomy] SQLite transfer failed: " + e.getMessage());
                return com.gitepildev.giteconomy.storage.TransferResult.failure(fromBefore, toBefore);
            }
        } finally {
            lm.releaseOrdered(ordered, tokens);
        }
    }

    @Override
    public void shutdown() {
        try { if (connection != null) connection.close(); } catch (SQLException ignored) {}
    }

    @Override
    public void logTransaction(Transaction tx) {
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
                plugin.getLogger().severe("[GitEconomy] SQLite logTransaction failed: " + e.getMessage());
            }
        }
    }

    /**
     * Removes balances for UUIDs that do not resolve to a known player.
     * @return Set of removed UUIDs as strings
     */
    public Set<String> cleanupOrphanedPlayers() {
        Set<String> removed = new HashSet<>();
        synchronized (lock) {
            try {
                PreparedStatement ps = connection.prepareStatement("SELECT uuid FROM '" + table + "'");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String uuidStr = rs.getString(1);
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        org.bukkit.OfflinePlayer player = org.bukkit.Bukkit.getOfflinePlayer(uuid);
                        if (player == null || player.getName() == null) {
                            PreparedStatement del = connection.prepareStatement("DELETE FROM '" + table + "' WHERE uuid=?");
                            del.setString(1, uuidStr);
                            del.executeUpdate();
                            removed.add(uuidStr);
                        }
                    } catch (IllegalArgumentException ignored) {}
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("[GitEconomy] SQLite cleanupOrphanedPlayers failed: " + e.getMessage());
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
                PreparedStatement ps = connection.prepareStatement("SELECT uuid FROM '" + table + "'");
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
                plugin.getLogger().severe("[GitEconomy] SQLite previewOrphanedPlayers failed: " + e.getMessage());
            }
        }
        return orphaned;
    }

    // --- Helper methods ---
}

