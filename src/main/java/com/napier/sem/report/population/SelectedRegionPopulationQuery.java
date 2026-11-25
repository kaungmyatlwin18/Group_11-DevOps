package com.napier.sem.report.population;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

/** Issue #31: Calculates the recorded population of one selected region. */
public class SelectedRegionPopulationQuery {

    /**
     * Returns the sum of country.Population for countries assigned to the selected region.
     * Matching is case-insensitive and ignores surrounding whitespace.
     *
     * @param connection an active connection to the World database
     * @param region the region to calculate
     * @return the recorded region population, or zero when no countries match
     * @throws SQLException if the database query fails
     * @throws NullPointerException if the connection or region is null
     * @throws IllegalArgumentException if the region is blank
     */
    public long findRegionPopulation(Connection connection, String region) throws SQLException {
        Objects.requireNonNull(connection, "Connection must not be null");
        Objects.requireNonNull(region, "Region must not be null");
        String selected = region.trim();
        if (selected.isEmpty()) {
            throw new IllegalArgumentException("Region must not be blank");
        }

        String sql = "SELECT COALESCE(SUM(Population), 0) AS RegionPopulation "
                + "FROM country WHERE LOWER(TRIM(Region)) = LOWER(?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, selected);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return 0L;
                }
                return result.getLong("RegionPopulation");
            }
        }
    }
}
