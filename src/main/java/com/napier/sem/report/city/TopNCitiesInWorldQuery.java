package com.napier.sem.report.city;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Issue #15: Retrieves the top N populated cities worldwide.
 * No continent, country, region or district restriction is applied.
 */
public class TopNCitiesInWorldQuery {

    /**
     * @param connection an open World database connection
     * @param n number of cities to return (must be positive)
     * @return up to N cities, sorted by population from largest to smallest
     * @throws IllegalArgumentException when n is zero or negative
     * @throws NullPointerException when connection is null
     * @throws SQLException when the query fails
     */
    public List<CityReport.CityRow> findTopNCities(Connection connection, int n)
            throws SQLException {
        Objects.requireNonNull(connection, "Connection must not be null");
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
                ORDER BY ci.Population DESC, ci.Name ASC, ci.ID ASC
                LIMIT ?
                """;

        List<CityReport.CityRow> cities = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, n);
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
