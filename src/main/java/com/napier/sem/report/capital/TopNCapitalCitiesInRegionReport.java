package com.napier.sem.report.capital;

import com.napier.sem.ReportDisplay;
import com.napier.sem.report.capital.AllCapitalCitiesQuery.CapitalRow;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Issue #25: Displays the top N populated capital cities in a selected region. */
public class TopNCapitalCitiesInRegionReport {

    private static final String[] HEADERS = {
            "Name", "Country", "Population"
    };

    private static final int DEFAULT_TOP_N = 5;

    /**
     * Reads N from -Dcapital.topN.region=5 or CAPITAL_TOP_N_REGION.
     * Defaults to five when no user setting is supplied.
     */
    public static int configuredTopN() {
        String raw = System.getProperty("capital.topN.region");

        if (raw == null || raw.isBlank()) {
            raw = System.getenv("CAPITAL_TOP_N_REGION");
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

    /**
     * Displays up to N matching capital cities in the selected region.
     * The region is supplied by the existing Issue #22 configuration.
     */
    public void display(Connection connection, String region, int n) throws SQLException {
        List<CapitalRow> capitals =
                new TopNCapitalCitiesInRegionQuery()
                        .findTopNCapitals(connection, region, n);

        List<String[]> rows = new ArrayList<>();

        for (CapitalRow capital : capitals) {
            rows.add(new String[]{
                    capital.name(),
                    capital.country(),
                    Long.toString(capital.population())
            });
        }

        ReportDisplay.printTable(
                "Top " + n + " capital cities in region: " + region.trim(),
                HEADERS,
                rows
        );
    }
}
