package com.napier.sem.report.capital;

import com.napier.sem.ReportDisplay;
import com.napier.sem.report.capital.AllCapitalCitiesQuery.CapitalRow;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Issue #21: Displays capital cities in a selected continent. */
public class CapitalCitiesByContinentReport {

    private static final String[] HEADERS = {"Name", "Country", "Population"};
    private static final String DEFAULT_CONTINENT = "Asia";

    /** Reads -Dcapital.continent=Europe or CAPITAL_CONTINENT; defaults to Asia. */
    public static String configuredContinent() {
        String continent = System.getProperty("capital.continent");
        if (continent == null || continent.isBlank()) {
            continent = System.getenv("CAPITAL_CONTINENT");
        }
        return continent == null || continent.isBlank() ? DEFAULT_CONTINENT : continent.trim();
    }

    public void display(Connection connection, String continent) throws SQLException {
        List<CapitalRow> capitals = new CapitalCitiesByContinentQuery().findCapitals(connection, continent);
        List<String[]> rows = new ArrayList<>();
        for (CapitalRow capital : capitals) {
            rows.add(new String[]{capital.name(), capital.country(), Long.toString(capital.population())});
        }
        ReportDisplay.printTable("Capital cities in continent: " + continent.trim(), HEADERS, rows);
    }
}
