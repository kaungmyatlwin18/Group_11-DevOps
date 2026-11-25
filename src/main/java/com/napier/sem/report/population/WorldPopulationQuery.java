package com.napier.sem.report.population;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

/** Issue #29: Calculates the total population recorded for all countries. */
public class WorldPopulationQuery {

    /**
     * Returns the sum of the Population values in the country table.
     * Null population values are ignored by SUM; an empty/all-null table returns zero.
     *
     * @param connection an active connection to the World database
     * @return the recorded world population as a long value
     * @throws SQLException if the database query fails
     */
    public long findWorldPopulation(Connection connection) throws SQLException {
        Objects.requireNonNull(connection, "Connection must not be null");
        String sql = "SELECT COALESCE(SUM(Population), 0) AS WorldPopulation FROM country";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            if (!result.next()) {
                return 0L;
            }
            return result.getLong("WorldPopulation");
        }
    }
}
