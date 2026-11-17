package com.napier.sem.report.capital;

import com.napier.sem.ReportDisplay;
import com.napier.sem.report.capital.AllCapitalCitiesQuery.CapitalRow;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Issue #22: Displays capital cities in a selected region. */
public class CapitalCitiesByRegionReport {

    private static final String[] HEADERS = {"Name", "Country", "Population"};
    private static final String DEFAULT_REGION = "Eastern Asia";

    /** Reads -Dcapital.region=Western Europe or CAPITAL_REGION; defaults to Eastern Asia. */
    public static String configuredRegion() {
        String region = System.getProperty("capital.region");
        if (region == null || region.isBlank()) {
            region = System.getenv("CAPITAL_REGION");
        }
        return region == null || region.isBlank() ? DEFAULT_REGION : region.trim();
    }

    public void display(Connection connection, String region) throws SQLException {
        List<CapitalRow> capitals = new CapitalCitiesByRegionQuery().findCapitals(connection, region);
        List<String[]> rows = new ArrayList<>();
        for (CapitalRow capital : capitals) {
            rows.add(new String[]{capital.name(), capital.country(), Long.toString(capital.population())});
        }
        ReportDisplay.printTable("Capital cities in region: " + region.trim(), HEADERS, rows);
    }
}
