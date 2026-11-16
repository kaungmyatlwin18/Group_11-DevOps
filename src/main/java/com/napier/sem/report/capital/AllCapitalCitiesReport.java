package com.napier.sem.report.capital;

import com.napier.sem.ReportDisplay;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Issue #20: Displays all capital cities in the World database. */
public class AllCapitalCitiesReport {

    private static final String[] HEADERS = {"Name", "Country", "Population"};

    public void display(Connection connection) throws SQLException {
        List<AllCapitalCitiesQuery.CapitalRow> capitals =
                new AllCapitalCitiesQuery().findAllCapitals(connection);
        List<String[]> rows = new ArrayList<>();
        for (AllCapitalCitiesQuery.CapitalRow capital : capitals) {
            rows.add(new String[]{capital.name(), capital.country(),
                    Long.toString(capital.population())});
        }
        ReportDisplay.printTable("All capital cities in the world by population", HEADERS, rows);
    }
}
