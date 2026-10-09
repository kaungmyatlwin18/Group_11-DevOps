package com.napier.sem.report.country;

import java.sql.Connection;
import java.sql.SQLException;

/** GitHub Issue #6: View countries in a region ordered by population. */
public class CountriesByRegionReport {

    public void display(Connection connection, String region) throws SQLException {
        new CountryReport().displayCountriesInRegion(connection, region);
    }
}