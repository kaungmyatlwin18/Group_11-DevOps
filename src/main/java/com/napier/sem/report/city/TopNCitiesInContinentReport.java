package com.napier.sem.report.city;

import com.napier.sem.ReportDisplay;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Issue #16: Displays the top N populated cities in a chosen continent. */
public class TopNCitiesInContinentReport {

    private static final String[] HEADERS = {
            "Name", "Country", "District", "Population"
    };
    private static final int DEFAULT_TOP_N = 5;
    private static final String DEFAULT_CONTINENT = "Asia";

    /**
     * Read N from -Dcity.topN.continent=5 or CITY_TOP_N_CONTINENT.
     * Defaults to five when no user setting is supplied.
     */
    public static int configuredTopN() {
        String raw = System.getProperty("city.topN.continent");
        if (raw == null || raw.isBlank()) {
            raw = System.getenv("CITY_TOP_N_CONTINENT");
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

    /**
     * Read the continent from -Dcity.continent=Europe or CITY_CONTINENT.
     * Reuses Issue #11's city.continent property; defaults to Asia.
     */
    public static String configuredContinent() {
        String continent = System.getProperty("city.continent");
        if (continent == null || continent.isBlank()) {
            continent = System.getenv("CITY_CONTINENT");
        }
        if (continent == null || continent.isBlank()) {
            return DEFAULT_CONTINENT;
        }
        return continent.trim();
    }

    /** Display no more than N matching cities, in population order. */
    public void display(Connection connection, String continent, int n) throws SQLException {
        List<CityReport.CityRow> cities =
                new TopNCitiesInContinentQuery().findTopNCities(connection, continent, n);

        List<String[]> rows = new ArrayList<>();
        for (CityReport.CityRow city : cities) {
            rows.add(new String[]{
                    city.name(),
                    city.country(),
                    city.district(),
                    Long.toString(city.population())
            });
        }

        ReportDisplay.printTable(
                "Top " + n + " cities in continent: " + continent.trim(), HEADERS, rows);
    }
}
