package com.napier.sem.report.population;

import com.napier.sem.ReportDisplay;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Issue #31: Displays the recorded population of one selected region. */
public class SelectedRegionPopulationReport {

    private static final String DEFAULT_REGION = "Southeast Asia";
    private static final String[] HEADERS = {"Region", "Population"};

    /**
     * Returns the configured region. The JVM property takes precedence over the
     * environment variable; the default is Southeast Asia.
     */
    public static String configuredRegion() {
        String selected = System.getProperty("population.selected.region");
        if (selected == null || selected.isBlank()) {
            selected = System.getenv("POPULATION_SELECTED_REGION");
        }
        return selected == null || selected.isBlank() ? DEFAULT_REGION : selected.trim();
    }

    /** Displays the configured region population. */
    public void display(Connection connection) throws SQLException {
        display(connection, configuredRegion());
    }

    /** Displays the population recorded for the supplied region. */
    public void display(Connection connection, String region) throws SQLException {
        String selected = region == null ? null : region.trim();
        long population = new SelectedRegionPopulationQuery()
                .findRegionPopulation(connection, region);
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{selected, Long.toString(population)});
        ReportDisplay.printTable("Population of Region: " + selected, HEADERS, rows);
    }
}
