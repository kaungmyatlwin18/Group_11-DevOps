package com.napier.sem.report.city;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Issue #11: returns every city belonging to a selected continent, largest first.
 */
public class CitiesByContinentQuery {

    public List<CityReport.CityRow> findCitiesInContinent(
            Connection connection, String continent) throws SQLException {
        Objects.requireNonNull(connection, "Connection must not be null");
        if (continent == null || continent.isBlank()) {
            throw new IllegalArgumentException("Continent must not be blank");
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
                """;

        List<CityReport.CityRow> cities = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, continent.trim());
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
