package com.napier.sem.report.country;

import java.sql.Connection;
import java.sql.SQLException;

/** GitHub Issue #8: View top N countries in a continent by population. */
public class TopNCountriesByContinentReport {

    public void display(Connection connection, String continent, int topN) throws SQLException {
        new CountryReport().displayTopNCountriesInContinent(connection, continent, topN);
    }
}
