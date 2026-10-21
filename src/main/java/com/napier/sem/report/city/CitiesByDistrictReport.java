
package com.napier.sem.report.city;

import com.napier.sem.ReportDisplay;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays cities in a selected district by population.
 */
public class CitiesByDistrictReport {

    private static final String[] HEADERS = {
            "Name", "Country", "District", "Population"
    };

    /**
     * Retrieves and displays cities for the specified district.
     *
     * @param connection an active database connection
     * @param district district to include in the report
     * @throws SQLException if the database query fails
     */
    public void display(Connection connection, String district)
            throws SQLException {

        CityReport cityReport = new CityReport();

        List<CityReport.CityRow> cities =
                cityReport.findCitiesInDistrict(connection, district);

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
                "Cities in district: " + district.trim(),
                HEADERS,
                rows
        );
    }
}
