package com.napier.sem;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Main {

    public static void main(String[] args) {

        DatabaseConnection db = new DatabaseConnection();

        for (int attempt = 1; attempt <= 20; attempt++) {
            try (
                    Connection con = db.connect();
                    Statement stmt = con.createStatement();
                    ResultSet results = stmt.executeQuery(
                            "SELECT Name, Population FROM country " +
                                    "ORDER BY Population DESC LIMIT 5")
            ) {
                System.out.println("Successfully connected to World database!");

                while (results.next()) {
                    System.out.println(
                            results.getString("Name") + " - " +
                                    results.getLong("Population")
                    );
                }

                return;
            }
            catch (SQLException e) {
                System.out.println(
                        "Connection attempt " + attempt +
                                " failed: " + e.getMessage()
                );

                if (attempt == 20) {
                    System.exit(1);
                }

                try {
                    Thread.sleep(2000);
                }
                catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    System.exit(1);
                }
            }
        }
    }
}