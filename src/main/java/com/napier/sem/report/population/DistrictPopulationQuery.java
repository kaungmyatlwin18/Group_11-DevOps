package com.napier.sem.report.population;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Issue #33: Aggregates recorded city populations in a selected district. */
public class DistrictPopulationQuery {
    public record PopulationRow(String district, String country, long population) { }

    /** Returns one district total per matching country; optional country name or code narrows selection. */
    public List<PopulationRow> findPopulation(Connection connection, String district, String country)
            throws SQLException {
        Objects.requireNonNull(connection, "Connection must not be null");
        if (district == null || district.isBlank()) {
            throw new IllegalArgumentException("District must not be blank");
        }
        boolean filterCountry = country != null && !country.isBlank();
        String sql = """
                SELECT MIN(ci.District) AS District, co.Name AS Country,
                       SUM(ci.Population) AS Population
                FROM city ci
                JOIN country co ON ci.CountryCode = co.Code
                WHERE LOWER(ci.District) = LOWER(?)
                """;
        if (filterCountry) {
            sql += " AND (LOWER(co.Name) = LOWER(?) OR LOWER(co.Code) = LOWER(?))\n";
        }
        sql += " GROUP BY co.Code, co.Name ORDER BY co.Name ASC, co.Code ASC";
        List<PopulationRow> rows = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, district.trim());
            if (filterCountry) {
                statement.setString(2, country.trim());
                statement.setString(3, country.trim());
            }
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    rows.add(new PopulationRow(result.getString("District"),
                            result.getString("Country"), result.getLong("Population")));
                }
            }
        }
        return rows;
    }
}
