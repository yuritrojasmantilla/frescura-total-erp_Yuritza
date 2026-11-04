package com.frescuratotal.erp.common;

import java.sql.*;

public class AuthDAO {
    private final ConnectionProvider connections;

    public AuthDAO() {
        this(DatabaseConnection::getConnection);
    }

    public AuthDAO(ConnectionProvider connections) {
        this.connections = connections;
    }

    public User login(String username, String password) throws SQLException {
        String sql =
                "SELECT id, username, role, password_salt, password_hash FROM app_users WHERE"
                    + " username=? AND active=TRUE";
        try (Connection c = connections.open();
                PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, username);
            try (ResultSet r = s.executeQuery()) {
                if (r.next()
                        && PasswordHasher.matches(
                                password,
                                r.getString("password_salt"),
                                r.getString("password_hash")))
                    return new User(r.getInt("id"), r.getString("username"), r.getString("role"));
            }
        }
        throw new IllegalArgumentException("Invalid username or password");
    }
}
