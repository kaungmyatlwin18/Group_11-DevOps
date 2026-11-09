package com.napier.sem.report.city;

import com.napier.sem.ReportDisplay;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Displays all cities in a selected continent, ordered by population. */
public class CitiesByContinentReport {

    private static final String[] HEADERS = {
            "Name", "Country", "District", "Population"
    };

    public void display(Connection connection, String continent) throws SQLException {
        List<CityReport.CityRow> cities =
                new CitiesByContinentQuery().findCitiesInContinent(connection, continent);

        List<String[]> rows = new ArrayList<>();
        for (CityReport.CityRow city : cities) {
            rows.add(new String[]{
                    city.name(),
                    city.country(),
                    city.district(),
                    Long.toString(city.population())
            });
        }

        ReportDisplay.printTable("Cities in continent: " + continent.trim(), HEADERS, rows);
    }
}
