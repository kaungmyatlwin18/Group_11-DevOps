package com.napier.sem.report.capital;

import com.napier.sem.report.capital.AllCapitalCitiesQuery.CapitalRow;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Issue #22: Retrieves all recorded capitals in a selected region. */
public class CapitalCitiesByRegionQuery {

    /** Returns matching capitals ordered by city population, largest first. */
    public List<CapitalRow> findCapitals(Connection connection, String region) throws SQLException {
        Objects.requireNonNull(connection, "Connection must not be null");
        if (region == null || region.isBlank()) {
            throw new IllegalArgumentException("Region must not be blank");
        }

        String sql = """
                SELECT ci.Name AS Name, co.Name AS Country, ci.Population AS Population
                FROM country co
                JOIN city ci ON co.Capital = ci.ID
                WHERE LOWER(co.Region) = LOWER(?)
                ORDER BY ci.Population DESC, ci.Name ASC, co.Code ASC
                """;

        List<CapitalRow> capitals = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, region.trim());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    capitals.add(new CapitalRow(result.getString("Name"),
                            result.getString("Country"), result.getLong("Population")));
                }
            }
        }
        return capitals;
    }
}
