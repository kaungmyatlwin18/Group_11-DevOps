package com.napier.sem.report.city;

import com.napier.sem.ReportDisplay;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays cities from a selected country ordered by population.
 */
public class CitiesByCountryReport {

    private static final String[] HEADERS = {
            "Name", "Country", "District", "Population"
    };

    /**
     * Retrieves all cities in the requested country and prints a report.
     *
     * @param connection an active World database connection
     * @param country    the selected country name
     * @throws SQLException if the database query fails
     */
    public void display(Connection connection, String country) throws SQLException {
        List<CityReport.CityRow> cities =
                new CityReport().findCitiesInCountry(connection, country);

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
                "Cities in country: " + country.trim(),
                HEADERS,
                rows
        );
    }
}
