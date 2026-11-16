package com.napier.sem.report.city;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Issue #17: Find the N most populated cities in a selected World database region.
 * Uses bound parameters for both the region and the number of rows returned.
 */
public class TopNCitiesInRegionQuery {

    /**
     * @param connection open World database connection
     * @param region name of the selected region, such as Eastern Asia
     * @param n positive maximum number of cities requested
     * @return up to N cities from the region, from largest to smallest
     * @throws NullPointerException if connection is null
     * @throws IllegalArgumentException if region is blank or N is not positive
     * @throws SQLException if the database query fails
     */
    public List<CityReport.CityRow> findTopNCities(
            Connection connection, String region, int n) throws SQLException {
        Objects.requireNonNull(connection, "Connection must not be null");
        if (region == null || region.isBlank()) {
            throw new IllegalArgumentException("Region must not be blank");
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
                WHERE co.Region = ?
                ORDER BY ci.Population DESC, ci.Name ASC, ci.ID ASC
                LIMIT ?
                """;

        List<CityReport.CityRow> cities = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, region.trim());
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
