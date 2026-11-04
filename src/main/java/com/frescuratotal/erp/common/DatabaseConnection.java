package com.frescuratotal.erp.common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {
    private DatabaseConnection() {}

    public static Connection getConnection() throws SQLException {
        String password = System.getenv("DB_PASSWORD");
        if (password == null) throw new SQLException("DB_PASSWORD is not configured");
        String url =
                System.getenv()
                        .getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/erp_frescura_total");
        String user = System.getenv().getOrDefault("DB_USER", "root");
        return DriverManager.getConnection(url, user, password);
    }
}
