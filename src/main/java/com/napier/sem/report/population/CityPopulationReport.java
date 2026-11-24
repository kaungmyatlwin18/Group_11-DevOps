package com.napier.sem.report.population;

import com.napier.sem.ReportDisplay;
import com.napier.sem.report.population.CityPopulationQuery.PopulationRow;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Issue #34: Displays the recorded population of a selected city. */
public class CityPopulationReport {

    private static final String[] HEADERS = {"City", "Country", "District", "Population"};
    private static final String DEFAULT_CITY = "Tokyo";

    /** Read -Dpopulation.city or POPULATION_CITY; default to Tokyo. */
    public static String configuredCity() {
        return configuredValue("population.city", "POPULATION_CITY", DEFAULT_CITY);
    }

    /** Optional country name/code; an empty value means all countries. */
    public static String configuredCountry() {
        return configuredValue("population.city.country", "POPULATION_CITY_COUNTRY", "");
    }

    private static String configuredValue(String property, String environment, String fallback) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            value = System.getenv(environment);
        }
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    /** Displays the selected city and its population; ambiguous names are not combined. */
    public void display(Connection connection, String city, String country) throws SQLException {
        List<PopulationRow> cities = new CityPopulationQuery().findPopulation(connection, city, country);
        List<String[]> rows = new ArrayList<>();
        for (PopulationRow row : cities) {
            rows.add(new String[]{row.city(), row.country(), row.district(),
                    Long.toString(row.population())});
        }

        String title = "Population of city: " + city.trim();
        if (country != null && !country.isBlank()) {
            title += " (" + country.trim() + ")";
        }
        if (cities.size() > 1) {
            System.out.println("Multiple matching cities found. Use country and district to identify the intended city.");
        }
        ReportDisplay.printTable(title, HEADERS, rows);
    }
}
