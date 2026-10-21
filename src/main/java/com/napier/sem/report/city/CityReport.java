
package com.napier.sem.report.city;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Provides database operations for city population reports.
 */
public class CityReport {

    /**
     * Represents a city and its population details.
     *
     * @param name       name of the city
     * @param country    country containing the city
     * @param district   district containing the city
     * @param population population of the city
     */
    public record CityRow(
            String name,
            String country,
            String district,
            long population
    ) {
    }

    /**
     * Retrieves cities in the specified district, ordered by population
     * in descending order.
     *
     * District names may exist in multiple countries.
     *
     * @param connection active database connection
     * @param district   district to search for
     * @return matching cities, or an empty list if none are found
     * @throws SQLException if a database operation fails
     * @throws IllegalArgumentException if the district is null or blank
     * @throws NullPointerException if the connection is null
     */
    public List<CityRow> findCitiesInDistrict(
            Connection connection, String district) throws SQLException {

        Objects.requireNonNull(connection, "Connection must not be null");

        if (district == null || district.isBlank()) {
            throw new IllegalArgumentException("District must not be blank");
        }

        String sql = """
                SELECT ci.Name AS Name,
                       co.Name AS Country,
                       ci.District AS District,
                       ci.Population AS Population
                FROM city ci
                JOIN country co ON ci.CountryCode = co.Code
                WHERE ci.District = ?
                ORDER BY ci.Population DESC, ci.Name ASC, ci.ID ASC
                """;

        List<CityRow> cities = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, district.trim());

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    cities.add(new CityRow(
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
