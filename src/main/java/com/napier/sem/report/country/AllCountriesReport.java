package com.napier.sem.report.country;

import java.sql.Connection;
import java.sql.SQLException;

/** GitHub Issue #2: View all countries ordered by population. */
public class AllCountriesReport {

    public void display(Connection connection) throws SQLException {
        new CountryReport().displayAllCountries(connection);
    }
}
