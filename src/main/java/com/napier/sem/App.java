package com.napier.sem;

import java.sql.Connection;
import java.sql.SQLException;

/** Application entry point for the Group 11 population reports. */
public class App {
    private static final int MAX_ATTEMPTS = 60;
    private static final long RETRY_DELAY_MS = 2000;

    @FunctionalInterface
    interface Connector {
        Connection connect() throws SQLException;
    }

    @FunctionalInterface
    interface Reporter {
        void display(Connection connection) throws SQLException;
    }

    @FunctionalInterface
    interface RetryWait {
        void pause() throws InterruptedException;
    }

    public static void main(String[] args) {
        // Missing credentials cannot be fixed by waiting for database startup.
        String password = System.getenv("DB_PASSWORD");
        if (password == null || password.isBlank()) {
            System.err.println("Database error: DB_PASSWORD is not set");
            System.exit(1);
            return;
        }
        int status = run(new DatabaseConnection()::connect,
                AutoDisplay::displayAllReports, MAX_ATTEMPTS,
                () -> Thread.sleep(RETRY_DELAY_MS));
        if (status != 0) {
            System.exit(status);
        }
    }

    /** Retry connection startup only; do not repeat partially executed reports. */
    static int run(Connector connector, Reporter reporter, int maxAttempts, RetryWait wait) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            Connection connection;
            try {
                connection = connector.connect();
                if (connection == null) {
                    throw new SQLException("Database connection returned null");
                }
            } catch (SQLException error) {
                System.err.println("Connection attempt " + attempt + " failed: " + error.getMessage());
                if (attempt == maxAttempts) {
                    System.err.println("Database connection failed.");
                    return 1;
                }
                try {
                    wait.pause();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    System.err.println("Database connection failed: retry interrupted.");
                    return 1;
                }
                continue;
            }

            try (Connection active = connection) {
                System.out.println("Successfully connected to World database!");
                reporter.display(active);
                return 0;
            } catch (SQLException error) {
                System.err.println("Database error: " + error.getMessage());
                return 1;
            }
        }
        System.err.println("Database connection failed.");
        return 1;
    }
}
