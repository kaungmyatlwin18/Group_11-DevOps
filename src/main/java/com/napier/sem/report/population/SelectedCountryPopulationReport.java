package com.napier.sem.report.population;

import com.napier.sem.ReportDisplay;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Issue #32: Displays the recorded population of one selected country. */
public class SelectedCountryPopulationReport {

    private static final String DEFAULT_COUNTRY = "Myanmar";
    private static final String[] HEADERS = {"Country", "Population"};

    /**
     * Returns the configured country. The JVM property takes precedence over
     * the environment variable; the default is Myanmar.
     */
    public static String configuredCountry() {
        String selected = System.getProperty("population.selected.country");
        if (selected == null || selected.isBlank()) {
            selected = System.getenv("POPULATION_SELECTED_COUNTRY");
        }
        return selected == null || selected.isBlank() ? DEFAULT_COUNTRY : selected.trim();
    }

    /** Displays the configured country's population. */
    public void display(Connection connection) throws SQLException {
        display(connection, configuredCountry());
    }

    /** Displays the population recorded for the supplied country. */
    public void display(Connection connection, String country) throws SQLException {
        String selected = country == null ? null : country.trim();
        long population = new SelectedCountryPopulationQuery()
                .findCountryPopulation(connection, country);
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{selected, Long.toString(population)});
        ReportDisplay.printTable("Population of Country: " + selected, HEADERS, rows);
    }
}
