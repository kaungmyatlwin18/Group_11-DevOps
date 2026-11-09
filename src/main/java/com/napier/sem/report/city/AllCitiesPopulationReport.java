package com.napier.sem.report.city;

import com.napier.sem.ReportDisplay;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays all cities in the World database in descending population order.
 * Issue #10: View all cities by population.
 */
public class AllCitiesPopulationReport {

    private static final String[] HEADERS = {
            "Name", "Country", "District", "Population"
    };

    /**
     * Prints all cities using the shared Group 11 table formatter.
     *
     * @param connection an active World database connection
     * @throws SQLException if the query fails
     */
    public void display(Connection connection) throws SQLException {
        List<CityReport.CityRow> cities =
                new AllCitiesPopulationQuery().findAllCities(connection);

        List<String[]> rows = new ArrayList<>();
        for (CityReport.CityRow city : cities) {
            rows.add(new String[]{
                    city.name(),
                    city.country(),
                    city.district(),
                    Long.toString(city.population())
            });
        }

        ReportDisplay.printTable("All cities in the world by population", HEADERS, rows);
    }
}
