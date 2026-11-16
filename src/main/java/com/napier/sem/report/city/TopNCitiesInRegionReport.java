package com.napier.sem.report.city;

import com.napier.sem.ReportDisplay;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Issue #17: Displays the top N populated cities in a selected region. */
public class TopNCitiesInRegionReport {

    private static final String[] HEADERS = {
            "Name", "Country", "District", "Population"
    };
    private static final String DEFAULT_REGION = "Eastern Asia";
    private static final int DEFAULT_TOP_N = 5;

    /**
     * Read N from -Dcity.topN.region=5 or CITY_TOP_N_REGION.
     * Defaults to 5 when neither setting is provided.
     */
    public static int configuredTopN() {
        String raw = System.getProperty("city.topN.region");
        if (raw == null || raw.isBlank()) {
            raw = System.getenv("CITY_TOP_N_REGION");
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
     * Reuses the -Dcity.region=Eastern Asia setting from Issue #12.
     * Alternatively uses CITY_REGION, or Eastern Asia when unspecified.
     */
    public static String configuredRegion() {
        String region = System.getProperty("city.region");
        if (region == null || region.isBlank()) {
            region = System.getenv("CITY_REGION");
        }
        if (region == null || region.isBlank()) {
            return DEFAULT_REGION;
        }
        return region.trim();
    }

    /** Display up to N cities in the selected region, highest population first. */
    public void display(Connection connection, String region, int n) throws SQLException {
        List<CityReport.CityRow> cities =
                new TopNCitiesInRegionQuery().findTopNCities(connection, region, n);

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
                "Top " + n + " cities in region: " + region.trim(), HEADERS, rows);
    }
}
