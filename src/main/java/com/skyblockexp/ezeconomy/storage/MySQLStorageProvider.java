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
import java.util.concurrent.TimeUnit;
import java.math.BigDecimal;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.skyblockexp.ezeconomy.api.events.BankPreTransactionEvent;
import com.skyblockexp.ezeconomy.api.events.BankPostTransactionEvent;
import com.skyblockexp.ezeconomy.api.events.TransactionType;

/**
 * MySQL implementation of the StorageProvider interface for EzEconomy.
 * Uses HikariCP connection pooling for high-performance concurrent access.
 */
public class MySQLStorageProvider implements StorageProvider {
    private final EzEconomyPlugin plugin;
    private HikariDataSource dataSource;
    private String table;
    private final YamlConfiguration dbConfig;

    public MySQLStorageProvider(EzEconomyPlugin plugin, YamlConfiguration dbConfig) {
        this.plugin = plugin;
        this.dbConfig = dbConfig;
        if (dbConfig == null) throw new IllegalArgumentException("MySQL config is missing!");
        this.table = dbConfig.getString("mysql.table", "balances");
    }

    private String buildJdbcUrl() {
        String host = dbConfig.getString("mysql.host");
        int port = dbConfig.getInt("mysql.port");
        String database = dbConfig.getString("mysql.database");
        return "jdbc:mysql://" + host + ":" + port + "/" + database
            + "?autoReconnect=true&useSSL=false&allowPublicKeyRetrieval=true";
    }

    @Override
    public void init() throws StorageInitException {
        try (Connection conn = dataSource != null ? dataSource.getConnection()
                : DriverManager.getConnection(buildJdbcUrl(),
                    dbConfig.getString("mysql.username"), dbConfig.getString("mysql.password"))) {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS `" + table + "` (uuid VARCHAR(36), currency VARCHAR(32), balance DOUBLE, PRIMARY KEY (uuid, currency))");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS banks (name VARCHAR(64), currency VARCHAR(32), balance DOUBLE, PRIMARY KEY (name, currency))");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS bank_members (bank VARCHAR(64), uuid VARCHAR(36), owner BOOLEAN, PRIMARY KEY (bank, uuid))");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS players (uuid VARCHAR(36) PRIMARY KEY, name VARCHAR(64), displayName VARCHAR(128))");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS transactions (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), currency VARCHAR(32), amount DOUBLE, timestamp BIGINT, from_uuid VARCHAR(36), to_uuid VARCHAR(36), from_balance_after DOUBLE, to_balance_after DOUBLE, INDEX idx_tx_uuid(uuid))");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS pending_notifications (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36), message TEXT, created_at BIGINT, INDEX idx_pn_uuid(uuid))");
        } catch (SQLException e) {
            plugin.getLogger().warning("MySQL schema init failed: " + e.getMessage());
            throw new StorageInitException("Failed to initialize MySQL schema", e);
        }
    }

    @Override
    public void load() throws StorageLoadException {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(buildJdbcUrl());
        config.setUsername(dbConfig.getString("mysql.username"));
        config.setPassword(dbConfig.getString("mysql.password"));
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(600000);
        config.setConnectionTimeout(5000);
        config.setPoolName("EzEconomy-HikariPool");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        try {
            dataSource = new HikariDataSource(config);
            plugin.getLogger().info("HikariCP connection pool initialized (max=10).");
        } catch (Exception e) {
            plugin.getLogger().warning("HikariCP pool init failed: " + e.getMessage());
            throw new StorageLoadException("Failed to create connection pool", e);
        }
    }

    private Connection getConn() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void save() throws StorageSaveException {
        // No in-memory cache, so nothing to save
    }

    @Override
    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }

    @Override
    public java.util.List<Transaction> getTransactions(java.util.UUID uuid, String currency) {
        java.util.List<Transaction> transactions = new java.util.ArrayList<>();
        try (Connection conn = getConn()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT amount, timestamp FROM transactions WHERE uuid=? AND currency=? ORDER BY timestamp DESC");
            ps.setString(1, uuid.toString());
            ps.setString(2, currency);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                transactions.add(new Transaction(uuid, currency, rs.getDouble("amount"), rs.getLong("timestamp")));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[EzEconomy] MySQL getTransactions failed for " + uuid + " (" + currency + "): " + e.getMessage());
        }
        return transactions;
    }

    @Override
    public double getBalance(UUID uuid, String currency) {
        try (Connection conn = getConn()) {
            PreparedStatement ps = conn.prepareStatement("SELECT balance FROM `" + table + "` WHERE uuid=? AND currency=?");
            ps.setString(1, uuid.toString());
            ps.setString(2, currency);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            plugin.getLogger().severe("[EzEconomy] MySQL getBalance failed for " + uuid + " (" + currency + "): " + e.getMessage());
        }
        return 0.0;
    }

    @Override
    public com.skyblockexp.ezeconomy.dto.EconomyPlayer getPlayer(UUID uuid) {
        try (Connection conn = getConn()) {
            PreparedStatement ps = conn.prepareStatement("SELECT name, displayName FROM players WHERE uuid=?");
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

    @Override
    public boolean playerExists(UUID uuid) {
        try (Connection conn = getConn()) {
            PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM `" + table + "` WHERE uuid=? LIMIT 1");
            ps.setString(1, uuid.toString());
            return ps.executeQuery().next();
        } catch (SQLException e) {
            plugin.getLogger().severe("[EzEconomy] MySQL playerExists failed for " + uuid + ": " + e.getMessage());
            return false;
        }
    }

    @Override
    public void setBalance(UUID uuid, String currency, double amount) {
        try (Connection conn = getConn()) {
            PreparedStatement ps = conn.prepareStatement("REPLACE INTO `" + table + "` (uuid, currency, balance) VALUES (?, ?, ?)");
            ps.setString(1, uuid.toString());
            ps.setString(2, currency);
            ps.setDouble(3, amount);
            ps.executeUpdate();
            safeUpdatePlayerInfo(conn, uuid);
        } catch (SQLException e) {
            plugin.getLogger().severe("[EzEconomy] MySQL setBalance failed for " + uuid + " (" + currency + "): " + e.getMessage());
        }
    }

    @Override
    public boolean tryWithdraw(UUID uuid, String currency, double amount) {
        try (Connection conn = getConn()) {
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE `" + table + "` SET balance = balance - ? WHERE uuid=? AND currency=? AND balance >= ?");
            ps.setDouble(1, amount);
            ps.setString(2, uuid.toString());
            ps.setString(3, currency);
            ps.setDouble(4, amount);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("[EzEconomy] MySQL tryWithdraw failed for " + uuid + " (" + currency + "): " + e.getMessage());
            return false;
        }
    }

    @Override
    public void deposit(UUID uuid, String currency, double amount) {
        try (Connection conn = getConn()) {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO `" + table + "` (uuid, currency, balance) VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE balance = balance + VALUES(balance)");
            ps.setString(1, uuid.toString());
            ps.setString(2, currency);
            ps.setDouble(3, amount);
            ps.executeUpdate();
            safeUpdatePlayerInfo(conn, uuid);
        } catch (SQLException e) {
            plugin.getLogger().severe("[EzEconomy] MySQL deposit failed for " + uuid + " (" + currency + "): " + e.getMessage());
        }
    }

    private void safeUpdatePlayerInfo(Connection conn, UUID uuid) {
        try {
            org.bukkit.entity.Player online = org.bukkit.Bukkit.getPlayer(uuid);
            if (online != null) {
                PreparedStatement ps = conn.prepareStatement(
                    "REPLACE INTO players (uuid, name, displayName) VALUES (?, ?, ?)");
                ps.setString(1, uuid.toString());
                ps.setString(2, online.getName());
                ps.setString(3, online.getDisplayName());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[EzEconomy] safeUpdatePlayerInfo failed: " + e.getMessage());
        }
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("HikariCP connection pool shut down.");
        }
    }

    public Map<UUID, Double> getAllBalances(String currency) {
        Map<UUID, Double> map = new ConcurrentHashMap<>();
        try (Connection conn = getConn()) {
            PreparedStatement ps = conn.prepareStatement("SELECT uuid, balance FROM `" + table + "` WHERE currency=?");
            ps.setString(1, currency);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    map.put(UUID.fromString(rs.getString(1)), rs.getDouble(2));
                } catch (IllegalArgumentException ignored) {}
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[EzEconomy] MySQL getAllBalances failed (" + currency + "): " + e.getMessage());
        }
        return map;
    }

    @Override
    public com.skyblockexp.ezeconomy.storage.TransferResult transfer(UUID fromUuid, UUID toUuid, String currency, double debitAmount, double creditAmount) {
        double fromBefore = getBalance(fromUuid, currency);
        double toBefore = getBalance(toUuid, currency);

        firePreTransaction(fromUuid, toUuid, debitAmount);

        try (Connection conn = getConn()) {
            conn.setAutoCommit(false);
            try {
                PreparedStatement psw = conn.prepareStatement(
                    "UPDATE `" + table + "` SET balance = balance - ? WHERE uuid=? AND currency=? AND balance >= ?");
                psw.setDouble(1, debitAmount);
                psw.setString(2, fromUuid.toString());
                psw.setString(3, currency);
                psw.setDouble(4, debitAmount);
                if (psw.executeUpdate() <= 0) {
                    conn.rollback();
                    return com.skyblockexp.ezeconomy.storage.TransferResult.failure(fromBefore, toBefore);
                }
                double credit = creditAmount > 0 ? creditAmount : debitAmount;
                PreparedStatement psd = conn.prepareStatement(
                    "INSERT INTO `" + table + "` (uuid, currency, balance) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE balance = balance + VALUES(balance)");
                psd.setString(1, toUuid.toString());
                psd.setString(2, currency);
                psd.setDouble(3, credit);
                psd.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                plugin.getLogger().severe("[EzEconomy] MySQL transfer failed: " + e.getMessage());
                return com.skyblockexp.ezeconomy.storage.TransferResult.failure(fromBefore, toBefore);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[EzEconomy] MySQL transfer connection error: " + e.getMessage());
            return com.skyblockexp.ezeconomy.storage.TransferResult.failure(fromBefore, toBefore);
        }

        double updatedFrom = getBalance(fromUuid, currency);
        double updatedTo = getBalance(toUuid, currency);
        com.skyblockexp.ezeconomy.storage.TransferResult tr = com.skyblockexp.ezeconomy.storage.TransferResult.success(updatedFrom, updatedTo);
        firePostTransaction(fromUuid, toUuid, debitAmount, tr, fromBefore, toBefore);
        return tr;
    }

    private void firePreTransaction(UUID from, UUID to, double amount) {
        com.skyblockexp.ezeconomy.api.events.PreTransactionEvent pre = new com.skyblockexp.ezeconomy.api.events.PreTransactionEvent(
            from, to, java.math.BigDecimal.valueOf(amount), com.skyblockexp.ezeconomy.api.events.TransactionType.TRANSFER);
        try {
            if (plugin.getServer().isPrimaryThread()) {
                plugin.getServer().getPluginManager().callEvent(pre);
            } else {
                plugin.getServer().getScheduler().callSyncMethod(plugin, () -> {
                    plugin.getServer().getPluginManager().callEvent(pre);
                    return null;
                }).get(5, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[EzEconomy] Failed to fire PreTransactionEvent: " + e.getMessage());
        }
    }

    private void firePostTransaction(UUID from, UUID to, double amount, com.skyblockexp.ezeconomy.storage.TransferResult tr, double fromBefore, double toBefore) {
        com.skyblockexp.ezeconomy.api.events.PostTransactionEvent post = new com.skyblockexp.ezeconomy.api.events.PostTransactionEvent(
            from, to, java.math.BigDecimal.valueOf(amount), com.skyblockexp.ezeconomy.api.events.TransactionType.TRANSFER,
            tr.isSuccess(), java.math.BigDecimal.valueOf(fromBefore), java.math.BigDecimal.valueOf(tr.getFromBalance()),
            java.math.BigDecimal.valueOf(toBefore), java.math.BigDecimal.valueOf(tr.getToBalance()));
        try {
            if (plugin.getServer().isPrimaryThread()) {
                plugin.getServer().getPluginManager().callEvent(post);
            } else {
                plugin.getServer().getScheduler().callSyncMethod(plugin, () -> {
                    plugin.getServer().getPluginManager().callEvent(post);
                    return null;
                }).get(5, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[EzEconomy] Failed to fire PostTransactionEvent: " + e.getMessage());
        }
    }

    public void insertPendingNotification(UUID uuid, String message) {
        try (Connection conn = getConn()) {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO pending_notifications (uuid, message, created_at) VALUES (?, ?, ?)");
            ps.setString(1, uuid.toString());
            ps.setString(2, message);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("[EzEconomy] insertPendingNotification failed: " + e.getMessage());
        }
    }

    public java.util.List<String> pollPendingNotifications(UUID uuid) {
        java.util.List<String> messages = new java.util.ArrayList<>();
        try (Connection conn = getConn()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT id, message FROM pending_notifications WHERE uuid=? ORDER BY created_at ASC");
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            java.util.List<Long> ids = new java.util.ArrayList<>();
            while (rs.next()) {
                ids.add(rs.getLong("id"));
                messages.add(rs.getString("message"));
            }
            if (!ids.isEmpty()) {
                StringBuilder sb = new StringBuilder("DELETE FROM pending_notifications WHERE id IN (");
                for (int i = 0; i < ids.size(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append(ids.get(i));
                }
                sb.append(")");
                conn.createStatement().executeUpdate(sb.toString());
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[EzEconomy] pollPendingNotifications failed: " + e.getMessage());
        }
        return messages;
    }

    public void cleanupOldNotifications(long maxAgeMs) {
        try (Connection conn = getConn()) {
            PreparedStatement ps = conn.prepareStatement("DELETE FROM pending_notifications WHERE created_at < ?");
            ps.setLong(1, System.currentTimeMillis() - maxAgeMs);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("[EzEconomy] cleanupOldNotifications failed: " + e.getMessage());
        }
    }

    @Override
    public UUID resolvePlayerByName(String name) {
        try (Connection conn = getConn()) {
            PreparedStatement ps = conn.prepareStatement("SELECT uuid FROM players WHERE name=? LIMIT 1");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return UUID.fromString(rs.getString(1));
        } catch (Exception e) {
            plugin.getLogger().warning("[EzEconomy] resolvePlayerByName failed for " + name + ": " + e.getMessage());
        }
        return null;
    }

    @Override
    public void persistPlayerInfo(UUID uuid, String name, String displayName) {
        if (uuid == null || name == null) return;
        if (displayName == null) displayName = name;
        try (Connection conn = getConn()) {
            PreparedStatement ps = conn.prepareStatement(
                "REPLACE INTO players (uuid, name, displayName) VALUES (?, ?, ?)");
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setString(3, displayName);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("[EzEconomy] persistPlayerInfo failed: " + e.getMessage());
        }
    }

    // --- Bank support ---

    public boolean createBank(String name, UUID owner) {
        try (Connection conn = getConn()) {
            PreparedStatement ps = conn.prepareStatement("INSERT INTO banks (name, currency, balance) VALUES (?, ?, 0.0)");
            ps.setString(1, name);
            ps.setString(2, "dollar");
            ps.executeUpdate();
            ps = conn.prepareStatement("INSERT INTO bank_members (bank, uuid, owner) VALUES (?, ?, ?)");
            ps.setString(1, name);
            ps.setString(2, owner.toString());
            ps.setBoolean(3, true);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { return false; }
    }

    public boolean deleteBank(String name) {
        try (Connection conn = getConn()) {
            PreparedStatement ps = conn.prepareStatement("DELETE FROM banks WHERE name=?");
            ps.setString(1, name);
            int affected = ps.executeUpdate();
            ps = conn.prepareStatement("DELETE FROM bank_members WHERE bank=?");
            ps.setString(1, name);
            ps.executeUpdate();
            return affected > 0;
        } catch (SQLException e) { return false; }
    }

    public boolean bankExists(String name) {
        try (Connection conn = getConn()) {
            PreparedStatement ps = conn.prepareStatement("SELECT name FROM banks WHERE name=?");
            ps.setString(1, name);
            return ps.executeQuery().next();
        } catch (SQLException e) { return false; }
    }

    public double getBankBalance(String name, String currency) {
        try (Connection conn = getConn()) {
            PreparedStatement ps = conn.prepareStatement("SELECT balance FROM banks WHERE name=? AND currency=?");
            ps.setString(1, name);
            ps.setString(2, currency);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {}
        return 0.0;
    }

    public void setBankBalance(String name, String currency, double amount) {
        try (Connection conn = getConn()) {
            PreparedStatement ps = conn.prepareStatement("REPLACE INTO banks (name, currency, balance) VALUES (?, ?, ?)");
            ps.setString(1, name);
            ps.setString(2, currency);
            ps.setDouble(3, amount);
            ps.executeUpdate();
        } catch (SQLException e) {}
    }

    @Override
    public boolean tryWithdrawBank(String name, String currency, double amount) {
        try (Connection conn = getConn()) {
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE banks SET balance = balance - ? WHERE name=? AND currency=? AND balance >= ?");
            ps.setDouble(1, amount);
            ps.setString(2, name);
            ps.setString(3, currency);
            ps.setDouble(4, amount);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("[EzEconomy] MySQL tryWithdrawBank failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void depositBank(String name, String currency, double amount) {
        try (Connection conn = getConn()) {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO banks (name, currency, balance) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE balance = balance + VALUES(balance)");
            ps.setString(1, name);
            ps.setString(2, currency);
            ps.setDouble(3, amount);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[EzEconomy] MySQL depositBank failed: " + e.getMessage());
        }
    }

    public Set<String> getBanks() {
        Set<String> set = ConcurrentHashMap.newKeySet();
        try (Connection conn = getConn()) {
            ResultSet rs = conn.prepareStatement("SELECT DISTINCT name FROM banks").executeQuery();
            while (rs.next()) set.add(rs.getString(1));
        } catch (SQLException e) {}
        return set;
    }

    public boolean isBankOwner(String name, UUID uuid) {
        try (Connection conn = getConn()) {
            PreparedStatement ps = conn.prepareStatement("SELECT owner FROM bank_members WHERE bank=? AND uuid=?");
            ps.setString(1, name);
            ps.setString(2, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getBoolean(1);
        } catch (SQLException e) {}
        return false;
    }

    public boolean isBankMember(String name, UUID uuid) {
        try (Connection conn = getConn()) {
            PreparedStatement ps = conn.prepareStatement("SELECT uuid FROM bank_members WHERE bank=? AND uuid=?");
            ps.setString(1, name);
            ps.setString(2, uuid.toString());
            return ps.executeQuery().next();
        } catch (SQLException e) {}
        return false;
    }

    public boolean addBankMember(String name, UUID uuid) {
        if (isBankMember(name, uuid)) return false;
        try (Connection conn = getConn()) {
            PreparedStatement ps = conn.prepareStatement("INSERT INTO bank_members (bank, uuid, owner) VALUES (?, ?, ?)");
            ps.setString(1, name);
            ps.setString(2, uuid.toString());
            ps.setBoolean(3, false);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { return false; }
    }

    public boolean removeBankMember(String name, UUID uuid) {
        try (Connection conn = getConn()) {
            PreparedStatement ps = conn.prepareStatement("DELETE FROM bank_members WHERE bank=? AND uuid=?");
            ps.setString(1, name);
            ps.setString(2, uuid.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public Set<UUID> getBankMembers(String name) {
        Set<UUID> set = ConcurrentHashMap.newKeySet();
        try (Connection conn = getConn()) {
            PreparedStatement ps = conn.prepareStatement("SELECT uuid FROM bank_members WHERE bank=?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                try { set.add(UUID.fromString(rs.getString(1))); } catch (IllegalArgumentException ignored) {}
            }
        } catch (SQLException e) {}
        return set;
    }

    @Override
    public void logTransaction(com.skyblockexp.ezeconomy.api.storage.models.Transaction tx) {
        try (Connection conn = getConn()) {
            PreparedStatement ps = conn.prepareStatement("INSERT INTO transactions (uuid, currency, amount, timestamp) VALUES (?, ?, ?, ?)");
            ps.setString(1, tx.getUuid().toString());
            ps.setString(2, tx.getCurrency());
            ps.setDouble(3, tx.getAmount());
            ps.setLong(4, tx.getTimestamp());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[EzEconomy] MySQL logTransaction failed: " + e.getMessage());
        }
    }

    public java.util.Set<String> cleanupOrphanedPlayers() {
        java.util.Set<String> removed = new java.util.HashSet<>();
        try (Connection conn = getConn()) {
            ResultSet rs = conn.prepareStatement("SELECT uuid FROM `" + table + "`").executeQuery();
            while (rs.next()) {
                String uuidStr = rs.getString(1);
                try {
                    org.bukkit.OfflinePlayer player = org.bukkit.Bukkit.getOfflinePlayer(java.util.UUID.fromString(uuidStr));
                    if (player == null || player.getName() == null) {
                        PreparedStatement del = conn.prepareStatement("DELETE FROM `" + table + "` WHERE uuid=?");
                        del.setString(1, uuidStr);
                        del.executeUpdate();
                        removed.add(uuidStr);
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[EzEconomy] MySQL cleanupOrphanedPlayers failed: " + e.getMessage());
        }
        return removed;
    }

    public java.util.Set<String> previewOrphanedPlayers() {
        java.util.Set<String> orphaned = new java.util.HashSet<>();
        try (Connection conn = getConn()) {
            ResultSet rs = conn.prepareStatement("SELECT uuid FROM `" + table + "`").executeQuery();
            while (rs.next()) {
                String uuidStr = rs.getString(1);
                try {
                    org.bukkit.OfflinePlayer player = org.bukkit.Bukkit.getOfflinePlayer(java.util.UUID.fromString(uuidStr));
                    if (player == null || player.getName() == null) orphaned.add(uuidStr);
                } catch (IllegalArgumentException ignored) {}
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[EzEconomy] MySQL previewOrphanedPlayers failed: " + e.getMessage());
        }
        return orphaned;
    }
}
