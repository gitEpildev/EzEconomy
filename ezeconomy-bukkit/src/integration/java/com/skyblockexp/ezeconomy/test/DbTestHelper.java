package com.skyblockexp.ezeconomy.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DbTestHelper {
    private DbTestHelper() {}

    /**
     * Create an in-memory H2 connection configured to be MySQL-compatible for tests.
     */
    public static Connection createH2MemoryMysql() throws SQLException {
        // MODE=MySQL enables many MySQL-specific syntactic compatibilities
        String url = "jdbc:h2:mem:test;MODE=MySQL;DB_CLOSE_DELAY=-1";
        return DriverManager.getConnection(url, "sa", "");
    }
}
