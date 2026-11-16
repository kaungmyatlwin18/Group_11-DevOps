package com.napier.sem.report.city;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Issue #18: Find the N most populated cities in a selected World database country.
 * Uses bound parameters for both the country and the number of rows returned.
 */
public class TopNCitiesInCountryQuery {

    /**
     * @param connection open World database connection
     * @param country name of the selected country, such as Myanmar
     * @param n positive maximum number of cities requested
     * @return up to N cities from the country, from largest to smallest
     * @throws NullPointerException if connection is null
     * @throws IllegalArgumentException if country is blank or N is not positive
     * @throws SQLException if the database query fails
     */
    public List<CityReport.CityRow> findTopNCities(
            Connection connection, String country, int n) throws SQLException {
        Objects.requireNonNull(connection, "Connection must not be null");
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Country must not be blank");
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
                WHERE co.Name = ?
                ORDER BY ci.Population DESC, ci.Name ASC, ci.ID ASC
                LIMIT ?
                """;

        List<CityReport.CityRow> cities = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, country.trim());
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
