package com.napier.sem.report.country;

import java.sql.Connection;
import java.sql.SQLException;

/** GitHub Issue #4: View countries in a continent ordered by population. */
public class CountriesByContinentReport {

    public void display(Connection connection, String continent) throws SQLException {
        new CountryReport().displayCountriesInContinent(connection, continent);
    }
}
