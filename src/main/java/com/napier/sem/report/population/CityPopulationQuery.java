package com.napier.sem.report.population;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Issue #34: Retrieves the population of a selected city. */
public class CityPopulationQuery {

    /** Country and district distinguish cities that share the same name. */
    public record PopulationRow(String city, String country, String district, long population) { }

    /**
     * Finds exact city-name matches, ignoring case and surrounding whitespace.
     * An optional country name or three-letter country code narrows the matches.
     * Multiple matches are returned individually, never summed.
     */
    public List<PopulationRow> findPopulation(Connection connection, String city, String country)
            throws SQLException {
        Objects.requireNonNull(connection, "Connection must not be null");
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("City must not be blank");
        }

        boolean filterCountry = country != null && !country.isBlank();
        String sql = """
                SELECT ci.Name AS City, co.Name AS Country,
                       ci.District AS District, ci.Population AS Population
                FROM city ci
                JOIN country co ON ci.CountryCode = co.Code
                WHERE LOWER(ci.Name) = LOWER(?)
                """;
        if (filterCountry) {
            sql += " AND (LOWER(co.Name) = LOWER(?) OR LOWER(co.Code) = LOWER(?))\n";
        }
        sql += " ORDER BY co.Name ASC, ci.District ASC, ci.ID ASC";

        List<PopulationRow> rows = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, city.trim());
            if (filterCountry) {
                statement.setString(2, country.trim());
                statement.setString(3, country.trim());
            }
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    rows.add(new PopulationRow(result.getString("City"),
                            result.getString("Country"), result.getString("District"),
                            result.getLong("Population")));
                }
            }
        }
        return rows;
    }
}
