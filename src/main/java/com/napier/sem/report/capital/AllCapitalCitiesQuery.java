package com.napier.sem.report.capital;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Issue #20: Retrieves every recorded capital city, largest population first. */
public class AllCapitalCitiesQuery {

    /** Capital reports require only these three fields. */
    public record CapitalRow(String name, String country, long population) { }

    public List<CapitalRow> findAllCapitals(Connection connection) throws SQLException {
        Objects.requireNonNull(connection, "Connection must not be null");

        String sql = """
                SELECT ci.Name AS Name, co.Name AS Country, ci.Population AS Population
                FROM country co
                JOIN city ci ON co.Capital = ci.ID
                ORDER BY ci.Population DESC, ci.Name ASC, co.Code ASC
                """;

        List<CapitalRow> capitals = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                capitals.add(new CapitalRow(result.getString("Name"),
                        result.getString("Country"), result.getLong("Population")));
            }
        }
        return capitals;
    }
}
