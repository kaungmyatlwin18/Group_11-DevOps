package com.napier.sem.report.country;

import java.sql.Connection;
import java.sql.SQLException;

/** GitHub Issue #9: View top N countries in a region by population. */
public class TopNCountriesByRegionReport {

    public void display(Connection connection, String region, int topN) throws SQLException {
        new CountryReport().displayTopNCountriesInRegion(connection, region, topN);
    }
}
