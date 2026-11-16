package com.napier.sem.report.city;

import com.napier.sem.ReportDisplay;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Issue #18: Displays the top N populated cities in a selected country. */
public class TopNCitiesInCountryReport {

    private static final String[] HEADERS = {
            "Name", "Country", "District", "Population"
    };
    private static final String DEFAULT_COUNTRY = "Myanmar";
    private static final int DEFAULT_TOP_N = 5;

    /**
     * Read N from -Dcity.topN.country=5 or CITY_TOP_N_COUNTRY.
     * Defaults to 5 when neither setting is provided.
     */
    public static int configuredTopN() {
        String raw = System.getProperty("city.topN.country");
        if (raw == null || raw.isBlank()) {
            raw = System.getenv("CITY_TOP_N_COUNTRY");
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
     * Reuses the -Dcity.country=Myanmar setting from Issue #13.
     * Alternatively uses CITY_COUNTRY, or Myanmar when unspecified.
     */
    public static String configuredCountry() {
        String country = System.getProperty("city.country");
        if (country == null || country.isBlank()) {
            country = System.getenv("CITY_COUNTRY");
        }
        if (country == null || country.isBlank()) {
            return DEFAULT_COUNTRY;
        }
        return country.trim();
    }

    /** Display up to N cities in the selected country, highest population first. */
    public void display(Connection connection, String country, int n) throws SQLException {
        List<CityReport.CityRow> cities =
                new TopNCitiesInCountryQuery().findTopNCities(connection, country, n);

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
                "Top " + n + " cities in country: " + country.trim(), HEADERS, rows);
    }
}
