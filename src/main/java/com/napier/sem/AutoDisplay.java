package com.napier.sem;

import com.napier.sem.report.country.AllCountriesReport;
import com.napier.sem.report.country.CountriesByContinentReport;
import com.napier.sem.report.country.CountriesByRegionReport;
import com.napier.sem.report.country.TopNCountriesByContinentReport;
import com.napier.sem.report.country.TopNCountriesByRegionReport;

import java.sql.Connection;
import java.sql.SQLException;

/** Run the implemented reports automatically; other developers can add theirs later. */
public final class AutoDisplay {

    private static final String DEFAULT_CONTINENT = "Asia";
    private static final String DEFAULT_REGION = "Eastern Asia";
    private static final int DEFAULT_TOP_N = 3;

    private AutoDisplay() {
    }

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

        System.out.println("\n========== AVAILABLE REPORTS COMPLETED ==========");
    }
}
