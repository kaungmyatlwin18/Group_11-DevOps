package com.napier.sem.report.population;

import com.napier.sem.ReportDisplay;
import com.napier.sem.report.population.ContinentPopulationQuery.PopulationRow;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Issue #26: Display the population distribution of every continent. */
public class ContinentPopulationReport {
    private static final String[] HEADERS = {
            "Continent", "Total Population", "Population in Cities", "% in Cities",
            "Population not in Cities", "% not in Cities"
    };

    public void display(Connection connection) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        for (PopulationRow row : new ContinentPopulationQuery().findAllContinents(connection)) {
            rows.add(new String[] {
                    row.continent(),
                    String.format(Locale.ROOT, "%,d", row.totalPopulation()),
                    String.format(Locale.ROOT, "%,d", row.cityPopulation()),
                    String.format(Locale.ROOT, "%.2f%%", row.cityPercentage()),
                    String.format(Locale.ROOT, "%,d", row.nonCityPopulation()),
                    String.format(Locale.ROOT, "%.2f%%", row.nonCityPercentage())
            });
        }
        ReportDisplay.printTable("Population report for each continent", HEADERS, rows);
    }
}
