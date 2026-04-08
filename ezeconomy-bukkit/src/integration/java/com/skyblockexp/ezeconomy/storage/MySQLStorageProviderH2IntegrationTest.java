package com.skyblockexp.ezeconomy.storage;

import com.skyblockexp.ezeconomy.core.EzEconomyPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class MySQLStorageProviderH2IntegrationTest {

    private HikariDataSource testDataSource;

    @BeforeEach
    void setup() throws Exception {
        try { MockBukkit.mock(); } catch (IllegalStateException e) { MockBukkit.unmock(); MockBukkit.mock(); }
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl("jdbc:h2:mem:test;MODE=MySQL;DB_CLOSE_DELAY=-1");
        hc.setUsername("sa");
        hc.setPassword("");
        hc.setMaximumPoolSize(2);
        testDataSource = new HikariDataSource(hc);
    }

    @AfterEach
    void teardown() throws Exception {
        try { if (testDataSource != null && !testDataSource.isClosed()) testDataSource.close(); } catch (Exception ignored) {}
        try { MockBukkit.unmock(); } catch (Exception ignored) {}
    }

    @Test
    void setGetDepositWithdrawFlow() throws Exception {
        EzEconomyPlugin plugin = (EzEconomyPlugin) MockBukkit.load(com.skyblockexp.ezeconomy.core.EzEconomyPlugin.class);
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("mysql.table", "balances");
        cfg.set("mysql.host", "localhost");
        cfg.set("mysql.port", 3306);
        cfg.set("mysql.database", "test");
        cfg.set("mysql.username", "sa");
        cfg.set("mysql.password", "");

        MySQLStorageProvider provider = new MySQLStorageProvider(plugin, cfg);

        // Inject H2-backed datasource (provider uses HikariCP, not a raw Connection field).
        Field dsField = MySQLStorageProvider.class.getDeclaredField("dataSource");
        dsField.setAccessible(true);
        dsField.set(provider, testDataSource);
        provider.init();

        UUID u = UUID.randomUUID();
        // Initially zero
        assertEquals(0.0, provider.getBalance(u, "dollar"), 0.0001);

        provider.setBalance(u, "dollar", 123.45);
        assertEquals(123.45, provider.getBalance(u, "dollar"), 0.0001);

        // withdraw portion that is available
        assertTrue(provider.tryWithdraw(u, "dollar", 23.45));
        assertEquals(100.0, provider.getBalance(u, "dollar"), 0.0001);

        // withdraw more than available should fail
        assertFalse(provider.tryWithdraw(u, "dollar", 200.0));
        assertEquals(100.0, provider.getBalance(u, "dollar"), 0.0001);

        // deposit
        provider.deposit(u, "dollar", 50.0);
        assertEquals(150.0, provider.getBalance(u, "dollar"), 0.0001);

        // transfer between two accounts
        UUID v = UUID.randomUUID();
        com.skyblockexp.ezeconomy.storage.TransferResult tr = provider.transfer(u, v, "dollar", 25.0, 25.0);
        assertTrue(tr.isSuccess());
        assertEquals(125.0, provider.getBalance(u, "dollar"), 0.0001);
        assertEquals(25.0, provider.getBalance(v, "dollar"), 0.0001);

        try { plugin.getServer().getPluginManager().disablePlugin(plugin); } catch (Exception ignored) {}
    }
}
