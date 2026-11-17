package com.napier.sem.report.capital;

import com.napier.sem.report.capital.AllCapitalCitiesQuery.CapitalRow;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Issue #23: Retrieves the top N recorded capital cities worldwide. */
public class TopNCapitalCitiesInWorldQuery {

    public List<CapitalRow> findTopNCapitals(Connection connection, int n) throws SQLException {
        Objects.requireNonNull(connection, "Connection must not be null");
        if (n <= 0) {
            throw new IllegalArgumentException("Capital Top N must be greater than zero");
        }

        String sql = """
                SELECT ci.Name AS Name, co.Name AS Country, ci.Population AS Population
                FROM country co
                JOIN city ci ON co.Capital = ci.ID
                ORDER BY ci.Population DESC, ci.Name ASC, co.Code ASC
                LIMIT ?
                """;

        List<CapitalRow> capitals = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, n);
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
