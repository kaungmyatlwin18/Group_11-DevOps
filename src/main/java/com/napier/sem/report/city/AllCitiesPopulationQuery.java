package com.napier.sem.report.city;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Retrieves every city in the World database, ordered by population.
 * Issue #10: View all cities by population.
 */
public class AllCitiesPopulationQuery {

    /**
     * Fetches all city records with their country names.
     *
     * @param connection an active World database connection
     * @return every city in descending population order
     * @throws NullPointerException if connection is null
     * @throws SQLException if a database operation fails
     */
    public List<CityReport.CityRow> findAllCities(Connection connection) throws SQLException {
        Objects.requireNonNull(connection, "Connection must not be null");

        String sql = """
                SELECT ci.Name AS Name,
                       co.Name AS Country,
                       ci.District AS District,
                       ci.Population AS Population
                FROM city ci
                JOIN country co ON ci.CountryCode = co.Code
                ORDER BY ci.Population DESC, ci.Name ASC, ci.ID ASC
                """;

        List<CityReport.CityRow> cities = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                cities.add(new CityReport.CityRow(
                        result.getString("Name"),
                        result.getString("Country"),
                        result.getString("District"),
                        result.getLong("Population")
                ));
            }
        }
        return cities;
    }
}
