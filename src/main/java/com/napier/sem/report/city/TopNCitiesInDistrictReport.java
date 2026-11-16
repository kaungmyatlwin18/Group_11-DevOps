package com.napier.sem.report.city;

import com.napier.sem.ReportDisplay;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Issue #19: Displays the top N populated cities in a selected district. */
public class TopNCitiesInDistrictReport {

    private static final String[] HEADERS = {
            "Name", "Country", "District", "Population"
    };
    private static final String DEFAULT_DISTRICT = "California";
    private static final int DEFAULT_TOP_N = 5;

    /**
     * Read N from -Dcity.topN.district=5 or CITY_TOP_N_DISTRICT.
     * Defaults to 5 when neither setting is provided.
     */
    public static int configuredTopN() {
        String raw = System.getProperty("city.topN.district");
        if (raw == null || raw.isBlank()) {
            raw = System.getenv("CITY_TOP_N_DISTRICT");
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
     * Reuses the -Dcity.district=California setting for this report.
     * Alternatively uses CITY_DISTRICT, or California when unspecified.
     */
    public static String configuredDistrict() {
        String district = System.getProperty("city.district");
        if (district == null || district.isBlank()) {
            district = System.getenv("CITY_DISTRICT");
        }
        if (district == null || district.isBlank()) {
            return DEFAULT_DISTRICT;
        }
        return district.trim();
    }

    /** Display up to N cities in the selected district, highest population first. */
    public void display(Connection connection, String district, int n) throws SQLException {
        List<CityReport.CityRow> cities =
                new TopNCitiesInDistrictQuery().findTopNCities(connection, district, n);

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
                "Top " + n + " cities in district: " + district.trim(), HEADERS, rows);
    }
}
