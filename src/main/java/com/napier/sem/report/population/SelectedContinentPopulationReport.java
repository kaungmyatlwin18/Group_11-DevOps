package com.napier.sem.report.population;

import com.napier.sem.ReportDisplay;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Issue #30: Displays the recorded population of a selected continent. */
public class SelectedContinentPopulationReport {

    private static final String DEFAULT_CONTINENT = "Asia";
    private static final String[] HEADERS = {"Continent", "Population"};

    /**
     * Returns the configured continent. The JVM property takes precedence over the
     * environment variable; the default is Asia.
     */
    public static String configuredContinent() {
        String selected = System.getProperty("population.continent");
        if (selected == null || selected.isBlank()) {
            selected = System.getenv("POPULATION_CONTINENT");
        }
        return selected == null || selected.isBlank() ? DEFAULT_CONTINENT : selected.trim();
    }

    /** Displays the configured continent population. */
    public void display(Connection connection) throws SQLException {
        display(connection, configuredContinent());
    }

    /** Displays the population recorded for the supplied continent. */
    public void display(Connection connection, String continent) throws SQLException {
        String selected = continent == null ? null : continent.trim();
        long population = new SelectedContinentPopulationQuery()
                .findContinentPopulation(connection, continent);
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{selected, Long.toString(population)});
        ReportDisplay.printTable("Population of Continent: " + selected, HEADERS, rows);
    }
}
