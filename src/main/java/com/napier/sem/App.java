package com.napier.sem;

import java.sql.Connection;
import java.sql.SQLException;

/** IntelliJ entry point: automatically displays completed reports without keyboard input. */
public class App {

    public static void main(String[] args) {
        // Requires the official project's DatabaseConnection.connect() method.
        // If App.java already exists, MERGE only the AutoDisplay call instead.
        DatabaseConnection database = new DatabaseConnection();

        try (Connection connection = database.connect()) {
            if (connection == null) {
                System.err.println("Database connection failed.");
                return;
            }
            System.out.println("Successfully connected to World database!");
            AutoDisplay.displayAllReports(connection);
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }
}
