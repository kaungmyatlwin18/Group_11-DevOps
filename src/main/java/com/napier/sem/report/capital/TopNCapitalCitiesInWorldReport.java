package com.napier.sem.report.capital;

import com.napier.sem.report.capital.AllCapitalCitiesQuery.CapitalRow;

import com.napier.sem.ReportDisplay;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Issue #23: Displays the top N populated capital cities across the entire world. */
public class TopNCapitalCitiesInWorldReport {

    private static final String[] HEADERS = {
            "Name", "Country", "Population"
    };
    private static final int DEFAULT_TOP_N = 5;

    /**
     * Reads user-selected N from a JVM property, then from an environment variable.
     * In IntelliJ, set VM Options to: -Dcapital.topN.world=10
     * Alternatively set CAPITAL_TOP_N_WORLD=10; otherwise the demo defaults to five.
     */
    public static int configuredTopN() {
        String raw = System.getProperty("capital.topN.world");
        if (raw == null || raw.isBlank()) {
            raw = System.getenv("CAPITAL_TOP_N_WORLD");
        }
        if (raw == null || raw.isBlank()) {
            return DEFAULT_TOP_N;
        }
        try {
            int n = Integer.parseInt(raw.trim());
            if (n <= 0) {
                throw new IllegalArgumentException("Capital Top N must be greater than zero");
            }
            return n;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Capital Top N must be a positive whole number", error);
        }
    }

    /** Displays the exact requested number, or all available capitals if fewer exist. */
    public void display(Connection connection, int n) throws SQLException {
        List<CapitalRow> capitals =
                new TopNCapitalCitiesInWorldQuery().findTopNCapitals(connection, n);

        List<String[]> rows = new ArrayList<>();
        for (CapitalRow city : capitals) {
            rows.add(new String[] {
                    city.name(),
                    city.country(),
                    Long.toString(city.population())
            });
        }
        ReportDisplay.printTable("Top " + n + " capital cities in the world by population", HEADERS, rows);
    }
}
