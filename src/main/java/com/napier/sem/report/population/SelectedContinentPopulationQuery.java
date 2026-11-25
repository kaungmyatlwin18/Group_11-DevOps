package com.napier.sem.report.population;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

/** Issue #30: Calculates the recorded population of one selected continent. */
public class SelectedContinentPopulationQuery {

    /**
     * Returns the sum of country.Population for countries assigned to the selected continent.
     * Matching is case-insensitive and ignores surrounding whitespace.
     *
     * @param connection an active connection to the World database
     * @param continent the continent to calculate
     * @return the recorded continent population, or zero when no countries match
     * @throws SQLException if the database query fails
     * @throws NullPointerException if the connection or continent is null
     * @throws IllegalArgumentException if the continent is blank
     */
    public long findContinentPopulation(Connection connection, String continent) throws SQLException {
        Objects.requireNonNull(connection, "Connection must not be null");
        Objects.requireNonNull(continent, "Continent must not be null");
        String selected = continent.trim();
        if (selected.isEmpty()) {
            throw new IllegalArgumentException("Continent must not be blank");
        }

        String sql = "SELECT COALESCE(SUM(Population), 0) AS ContinentPopulation "
                + "FROM country WHERE LOWER(TRIM(Continent)) = LOWER(?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, selected);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return 0L;
                }
                return result.getLong("ContinentPopulation");
            }
        }
    }
}
