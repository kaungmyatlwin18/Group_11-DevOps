package com.napier.sem.report.capital;

import com.napier.sem.report.capital.AllCapitalCitiesQuery.CapitalRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Issue #25: Retrieves the top N recorded capital cities in a selected region. */
public class TopNCapitalCitiesInRegionQuery {

    /**
     * Returns up to N recorded capital cities in the selected region,
     * ordered by capital-city population from highest to lowest.
     */
    public List<CapitalRow> findTopNCapitals(
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
                       ci.Population AS Population
                FROM country co
                JOIN city ci ON co.Capital = ci.ID
                WHERE LOWER(co.Region) = LOWER(?)
                ORDER BY ci.Population DESC, ci.Name ASC, co.Code ASC
                LIMIT ?
                """;

        List<CapitalRow> capitals = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, region.trim());
            statement.setInt(2, n);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    capitals.add(new CapitalRow(
                            result.getString("Name"),
                            result.getString("Country"),
                            result.getLong("Population")
                    ));
                }
            }
        }

        return capitals;
    }
}
