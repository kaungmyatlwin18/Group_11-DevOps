package com.napier.sem;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Application entry point for the Group 11 population reports.
 * All available reports are launched through AutoDisplay.
 */
public class App {

    public static void main(String[] args) {
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
