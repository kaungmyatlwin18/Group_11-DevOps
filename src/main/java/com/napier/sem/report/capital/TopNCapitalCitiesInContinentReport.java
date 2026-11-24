package com.napier.sem.report.capital;

import com.napier.sem.ReportDisplay;
import com.napier.sem.report.capital.AllCapitalCitiesQuery.CapitalRow;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Issue #24: Displays the top N populated capital cities in a selected continent. */
public class TopNCapitalCitiesInContinentReport {

    private static final String[] HEADERS = {
            "Name", "Country", "Population"
    };

    private static final int DEFAULT_TOP_N = 5;

    /**
     * Reads N from -Dcapital.topN.continent=5 or CAPITAL_TOP_N_CONTINENT.
     * Defaults to five when no user setting is supplied.
     */
    public static int configuredTopN() {
        String raw = System.getProperty("capital.topN.continent");

        if (raw == null || raw.isBlank()) {
            raw = System.getenv("CAPITAL_TOP_N_CONTINENT");
        }

        if (raw == null || raw.isBlank()) {
            return DEFAULT_TOP_N;
        }

        try {
            int n = Integer.parseInt(raw.trim());

            if (n <= 0) {
                throw new IllegalArgumentException(
                        "Capital Top N must be greater than zero");
            }

            return n;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(
                    "Capital Top N must be a positive whole number", error);
        }
    }

    /** Displays no more than N matching capital cities in population order. */
    public void display(Connection connection, String continent, int n) throws SQLException {
        List<CapitalRow> capitals =
                new TopNCapitalCitiesInContinentQuery()
                        .findTopNCapitals(connection, continent, n);

        List<String[]> rows = new ArrayList<>();

        for (CapitalRow capital : capitals) {
            rows.add(new String[]{
                    capital.name(),
                    capital.country(),
                    Long.toString(capital.population())
            });
        }

        ReportDisplay.printTable(
                "Top " + n + " capital cities in continent: " + continent.trim(),
                HEADERS,
                rows);
    }
}
