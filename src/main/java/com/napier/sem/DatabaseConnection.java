
package com.napier.sem;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    public Connection connect() throws SQLException {

        String host = System.getenv().getOrDefault("DB_HOST", "db");
        String port = System.getenv().getOrDefault("DB_PORT", "3306");
        String user = System.getenv().getOrDefault("DB_USER", "root");
        String password = System.getenv("DB_PASSWORD");

        if (password == null || password.isBlank()) {
            throw new SQLException("DB_PASSWORD is not set");
        }

        String url = "jdbc:mysql://" + host + ":" + port + "/world";

        return DriverManager.getConnection(url, user, password);
    }
}
