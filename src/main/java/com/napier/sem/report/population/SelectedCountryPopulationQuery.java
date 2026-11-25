package com.napier.sem.report.population;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

/** Issue #32: Retrieves the recorded population of one selected country. */
public class SelectedCountryPopulationQuery {

    private static final String SQL = "SELECT COALESCE(Population, 0) AS CountryPopulation "
            + "FROM country WHERE LOWER(TRIM(Name)) = LOWER(?) ORDER BY Code ASC";

    /**
     * Returns the population recorded for the selected country.
     * Matching is case-insensitive and ignores surrounding whitespace.
     *
     * @param connection an active connection to the World database
     * @param country the country name to retrieve
     * @return the recorded population, or zero when the country is not found
     * @throws SQLException if the database query fails
     * @throws NullPointerException if the connection or country is null
     * @throws IllegalArgumentException if the country name is blank
     */
    public long findCountryPopulation(Connection connection, String country) throws SQLException {
        Objects.requireNonNull(connection, "Connection must not be null");
        Objects.requireNonNull(country, "Country must not be null");
        String selected = country.trim();
        if (selected.isEmpty()) {
            throw new IllegalArgumentException("Country must not be blank");
        }

        try (PreparedStatement statement = connection.prepareStatement(SQL)) {
            statement.setString(1, selected);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return 0L;
                }
                return result.getLong("CountryPopulation");
            }
        }
    }
}
