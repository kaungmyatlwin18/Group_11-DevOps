package com.napier.sem.report.capital;

import com.napier.sem.report.capital.AllCapitalCitiesQuery.CapitalRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Issue #24: Returns the top N populated capital cities in a selected continent. */
public class TopNCapitalCitiesInContinentQuery {

    /**
     * Returns up to N recorded capital cities in the selected continent,
     * ordered by capital-city population from highest to lowest.
     *
     * @param connection an open connection to the World database
     * @param continent the selected continent
     * @param n maximum number of capital cities to return; must be positive
     * @return matching capital cities in population order
     * @throws NullPointerException if the connection is null
     * @throws IllegalArgumentException if the continent is blank or N is not positive
     * @throws SQLException if the database operation fails
     */
    public List<CapitalRow> findTopNCapitals(
            Connection connection, String continent, int n) throws SQLException {

        Objects.requireNonNull(connection, "Connection must not be null");

        if (continent == null || continent.isBlank()) {
            throw new IllegalArgumentException("Continent must not be blank");
        }

        if (n <= 0) {
            throw new IllegalArgumentException("N must be a positive whole number");
        }

        String sql = """
                SELECT ci.Name AS Name,
                       co.Name AS Country,
                       ci.Population AS Population
                FROM country co
                JOIN city ci ON co.Capital = ci.ID
                WHERE LOWER(co.Continent) = LOWER(?)
                ORDER BY ci.Population DESC, ci.Name ASC, co.Code ASC
                LIMIT ?
                """;

        List<CapitalRow> capitals = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, continent.trim());
            statement.setInt(2, n);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    capitals.add(new CapitalRow(
                            result.getString("Name"),
                            result.getString("Country"),
                            result.getLong("Population")
                    ));
                }
            }
        }

        return capitals;
    }
}
