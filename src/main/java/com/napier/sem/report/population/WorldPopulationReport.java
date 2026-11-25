package com.napier.sem.report.population;

import com.napier.sem.ReportDisplay;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Issue #29: Displays the total population recorded across all countries. */
public class WorldPopulationReport {

    private static final String[] HEADERS = {"Population"};

    /** Displays the total population calculated from the country table. */
    public void display(Connection connection) throws SQLException {
        long population = new WorldPopulationQuery().findWorldPopulation(connection);
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{Long.toString(population)});
        ReportDisplay.printTable("Total Population of the World", HEADERS, rows);
    }
}
