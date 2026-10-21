package com.napier.sem.report.city;

import com.napier.sem.ReportDisplay;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays cities from countries in a selected geographic region.
 */
public class CitiesByRegionReport {

    private static final String[] HEADERS = {
            "Name", "Country", "District", "Population"
    };

    /**
     * Retrieves and displays all cities in the selected country region,
     * ordered from the highest population to the lowest.
     *
     * @param connection an active World database connection
     * @param region     region to include in the report
     * @throws SQLException if the database query fails
     */
    public void display(Connection connection, String region) throws SQLException {
        List<CityReport.CityRow> cities =
                new CityReport().findCitiesInRegion(connection, region);

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
                "Cities in region: " + region.trim(),
                HEADERS,
                rows
        );
    }
}
