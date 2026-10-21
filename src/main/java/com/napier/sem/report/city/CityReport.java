
package com.napier.sem.report.city;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Provides database access for city population reports.
 */
public class CityReport {

    /**
     * Represents a city record returned by a population report.
     *
     * @param name       name of the city
     * @param country    country where the city is located
     * @param district   district where the city is located
     * @param population total population of the city
     */
    public record CityRow(
            String name,
            String country,
            String district,
            long population
    ) {
    }

    /**
     * Retrieves cities within the specified district,
     * ordered by population in descending order.
     *
     * <p>District names may exist in multiple countries,
     * so the results can include cities from different countries.</p>
     *
     * @param connection an active database connection
     * @param district   the district name to search for
     * @return a list of matching cities ordered by population
     * @throws SQLException if the database query fails
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
