package com.napier.sem.report.city;

import com.napier.sem.ReportDisplay;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Issue #15: Displays the top N populated cities across the entire world. */
public class TopNCitiesInWorldReport {

    private static final String[] HEADERS = {
            "Name", "Country", "District", "Population"
    };
    private static final int DEFAULT_TOP_N = 5;

    /**
     * Reads user-selected N from a JVM property, then from an environment variable.
     * In IntelliJ, set VM Options to: -Dcity.topN.world=10
     * Alternatively set CITY_TOP_N_WORLD=10; otherwise the demo defaults to five.
     */
    public static int configuredTopN() {
        String raw = System.getProperty("city.topN.world");
        if (raw == null || raw.isBlank()) {
            raw = System.getenv("CITY_TOP_N_WORLD");
        }
        if (raw == null || raw.isBlank()) {
            return DEFAULT_TOP_N;
        }
        try {
            int n = Integer.parseInt(raw.trim());
            if (n <= 0) {
                throw new IllegalArgumentException("City Top N must be greater than zero");
            }
            return n;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("City Top N must be a positive whole number", error);
        }
    }

    /** Displays the exact requested number, or all available cities if fewer exist. */
    public void display(Connection connection, int n) throws SQLException {
        List<CityReport.CityRow> cities =
                new TopNCitiesInWorldQuery().findTopNCities(connection, n);

        List<String[]> rows = new ArrayList<>();
        for (CityReport.CityRow city : cities) {
            rows.add(new String[] {
                    city.name(),
                    city.country(),
                    city.district(),
                    Long.toString(city.population())
            });
        }
        ReportDisplay.printTable("Top " + n + " cities in the world by population", HEADERS, rows);
    }
}
