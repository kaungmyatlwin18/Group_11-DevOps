package com.napier.sem;

import com.napier.sem.report.city.CitiesByDistrictReport;
import com.napier.sem.report.city.CitiesByRegionReport;
import com.napier.sem.report.country.AllCountriesReport;
import com.napier.sem.report.country.CountriesByContinentReport;
import com.napier.sem.report.country.CountriesByRegionReport;
import com.napier.sem.report.country.TopNCountriesByContinentReport;
import com.napier.sem.report.country.TopNCountriesByRegionReport;
import com.napier.sem.report.country.TopNCountriesInWorldReport;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Runs the implemented country and city population reports automatically.
 */
public final class AutoDisplay {

    private static final String DEFAULT_CONTINENT = "Asia";
    private static final String DEFAULT_REGION = "Eastern Asia";
    private static final int DEFAULT_TOP_N = 3;
    private static final String DEFAULT_DISTRICT = "California";

    /**
     * User-configured N for Issue #7. A JVM property takes precedence over the
     * environment variable. With neither set, the automatic demo uses N = 3.
     */
    private static int worldTopN() {
        String raw = System.getProperty("topN.world");
        if (raw == null || raw.isBlank()) {
            raw = System.getenv("TOP_N_WORLD");
        }
        if (raw == null || raw.isBlank()) {
            return DEFAULT_TOP_N;
        }
        try {
            int selected = Integer.parseInt(raw.trim());
            if (selected <= 0) {
                throw new IllegalArgumentException("World Top N must be greater than zero");
            }
            return selected;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("World Top N must be a positive whole number", error);
        }
    }

    /**
     * Selects a city region through the optional -Dcity.region JVM property.
     * Uses the existing default region when no value is supplied.
     */
    private static String cityRegion() {
        String region = System.getProperty("city.region");
        return region == null || region.isBlank() ? DEFAULT_REGION : region.trim();
    }

    private AutoDisplay() {
    }

    /**
     * Checks for the full city table schema before running city reports.
     * Existing country-only tests use a smaller city table containing capitals.
     * Querying metadata without fetching rows avoids changing those test fixtures.
     */
    private static boolean hasDistrictCityColumns(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT * FROM city WHERE 1 = 0")) {

            ResultSetMetaData metadata = result.getMetaData();
            Set<String> columns = new HashSet<>();
            for (int index = 1; index <= metadata.getColumnCount(); index++) {
                columns.add(metadata.getColumnName(index).toLowerCase(Locale.ROOT));
            }
            return columns.containsAll(Set.of("id", "name", "countrycode", "district", "population"));
        }
    }

    /**
     * Displays available country reports, then compatible city reports.
     *
     * @param connection an active connection to the World database
     * @throws SQLException if a database operation fails
     */
    public static void displayAllReports(Connection connection) throws SQLException {
        System.out.println("\n========== GROUP 11 COUNTRY REPORTS ==========");

        System.out.println("\n===== ISSUE #2 =====");
        new AllCountriesReport().display(connection);

        System.out.println("\n===== ISSUE #4 =====");
        new CountriesByContinentReport().display(connection, DEFAULT_CONTINENT);

        System.out.println("\n===== ISSUE #6 =====");
        new CountriesByRegionReport().display(connection, DEFAULT_REGION);

        System.out.println("\n===== ISSUE #8 =====");
        new TopNCountriesByContinentReport().display(connection, DEFAULT_CONTINENT, DEFAULT_TOP_N);

        System.out.println("\n===== ISSUE #9 =====");
        new TopNCountriesByRegionReport().display(connection, DEFAULT_REGION, DEFAULT_TOP_N);

        System.out.println("\n===== ISSUE #7 =====");
        new TopNCountriesInWorldReport().display(connection, worldTopN());

        // Existing country-only tests use a minimal H2 city table.
        // Run city reports when the complete World database schema is available.
        if (hasDistrictCityColumns(connection)) {
            System.out.println("\n========== GROUP 11 CITY REPORTS ==========");

            System.out.println("\n===== ISSUE #14 =====");
            new CitiesByDistrictReport().display(connection, DEFAULT_DISTRICT);

            System.out.println("\n===== ISSUE #12 =====");
            new CitiesByRegionReport().display(connection, cityRegion());
        }

        System.out.println("\n========== AVAILABLE REPORTS COMPLETED ==========");
    }
}
