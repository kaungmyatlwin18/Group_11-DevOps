package com.napier.sem.report.city;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Issue #16: Returns the top N most populated cities in a selected continent.
 * The continent and the row limit are both bound SQL parameters.
 */
public class TopNCitiesInContinentQuery {

    /**
     * @param connection an open World database connection
     * @param continent the continent chosen by the user
     * @param n maximum number of matching cities to return; must be positive
     * @return up to N matching cities ordered by population descending
     * @throws NullPointerException if the connection is null
     * @throws IllegalArgumentException if the continent is blank or N is not positive
     * @throws SQLException when the database operation fails
     */
    public List<CityReport.CityRow> findTopNCities(
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
                       ci.District AS District,
                       ci.Population AS Population
                FROM city ci
                JOIN country co ON ci.CountryCode = co.Code
                WHERE co.Continent = ?
                ORDER BY ci.Population DESC, ci.Name ASC, ci.ID ASC
                LIMIT ?
                """;

        List<CityReport.CityRow> cities = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, continent.trim());
            statement.setInt(2, n);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    cities.add(new CityReport.CityRow(
                            result.getString("Name"),
                            result.getString("Country"),
                            result.getString("District"),
                            result.getLong("Population")
                    ));
                }
            }
        }
        return cities;
    }
}
