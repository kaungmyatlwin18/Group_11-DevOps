package com.napier.sem.report.population;

import com.napier.sem.ReportDisplay;
import com.napier.sem.report.population.DistrictPopulationQuery.PopulationRow;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Issue #33: Displays district population totals from the World database. */
public class DistrictPopulationReport {
    private static final String[] HEADERS = {"District", "Country", "Population"};
    private static final String DEFAULT_DISTRICT = "California";

    public static String configuredDistrict() {
        return configuredValue("population.district", "POPULATION_DISTRICT", DEFAULT_DISTRICT);
    }

    public static String configuredCountry() {
        return configuredValue("population.district.country", "POPULATION_DISTRICT_COUNTRY", "");
    }

    private static String configuredValue(String property, String environment, String fallback) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) value = System.getenv(environment);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public void display(Connection connection, String district, String country) throws SQLException {
        List<PopulationRow> districts = new DistrictPopulationQuery().findPopulation(connection, district, country);
        List<String[]> rows = new ArrayList<>();
        for (PopulationRow row : districts) {
            rows.add(new String[]{row.district(), row.country(), Long.toString(row.population())});
        }
        String title = "Population of district: " + district.trim();
        if (country != null && !country.isBlank()) title += " (" + country.trim() + ")";
        if (districts.size() > 1) {
            System.out.println("Multiple countries have this district name. Use a country name or code to select one district.");
        }
        ReportDisplay.printTable(title, HEADERS, rows);
    }
}
