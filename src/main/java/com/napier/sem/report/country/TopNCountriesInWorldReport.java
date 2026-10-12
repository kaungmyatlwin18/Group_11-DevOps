package com.napier.sem.report.country;

import java.sql.Connection;
import java.sql.SQLException;

/** GitHub Issue #7: View top N countries worldwide by population. */
public class TopNCountriesInWorldReport {

    public void display(Connection connection, int topN) throws SQLException {
        new CountryReport().displayTopNCountriesInWorld(connection, topN);
    }
}
