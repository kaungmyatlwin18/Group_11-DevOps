package com.napier.sem.report.population;

import com.napier.sem.ReportDisplay;
import com.napier.sem.report.population.CountryPopulationQuery.PopulationRow;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Issue #28: Display the population distribution of every country. */
public class CountryPopulationReport {
    private static final String[] HEADERS = {
            "Country", "Total Population", "Population in Cities", "% in Cities",
            "Population not in Cities", "% not in Cities"
    };

    public void display(Connection connection) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        List<String> inconsistentCountries = new ArrayList<>();
        for (PopulationRow row : new CountryPopulationQuery().findAllCountries(connection)) {
            if (row.cityPopulation() > row.totalPopulation()) {
                inconsistentCountries.add(row.country());
            }
            rows.add(new String[] {
                    row.country(),
                    String.format(Locale.ROOT, "%,d", row.totalPopulation()),
                    String.format(Locale.ROOT, "%,d", row.cityPopulation()),
                    String.format(Locale.ROOT, "%.2f%%", row.cityPercentage()),
                    String.format(Locale.ROOT, "%,d", row.nonCityPopulation()),
                    String.format(Locale.ROOT, "%.2f%%", row.nonCityPercentage())
            });
        }
        ReportDisplay.printTable("Population report for each country", HEADERS, rows);
        if (!inconsistentCountries.isEmpty()) {
            System.out.println("Data warning: recorded city population exceeds total population for: "
                    + String.join(", ", inconsistentCountries) + ". Raw dataset figures are retained; negative non-city values are not valid demographic estimates.");
        }
    }
}
