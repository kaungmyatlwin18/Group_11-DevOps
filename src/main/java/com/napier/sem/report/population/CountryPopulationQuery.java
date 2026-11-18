package com.napier.sem.report.population;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Issue #28: Population totals and recorded city population for every country. */
public class CountryPopulationQuery {
    // Aggregate cities by country first: a country must contribute its total only once.
    private static final String SQL = """
            SELECT co.Name AS Country,
                   co.Population AS TotalPopulation,
                   COALESCE(cp.CityPopulation, 0) AS CityPopulation
            FROM country co
            LEFT JOIN (
                SELECT CountryCode, SUM(Population) AS CityPopulation
                FROM city
                GROUP BY CountryCode
            ) cp ON cp.CountryCode = co.Code
            ORDER BY co.Name ASC, co.Code ASC
            """;

    /** Non-city population is the remainder outside cities recorded in the dataset. */
    public record PopulationRow(String country, long totalPopulation, long cityPopulation) {
        public long nonCityPopulation() {
            return totalPopulation - cityPopulation;
        }

        public double cityPercentage() {
            return totalPopulation == 0 ? 0.0 : 100.0 * cityPopulation / totalPopulation;
        }

        public double nonCityPercentage() {
            return totalPopulation == 0 ? 0.0 : 100.0 * nonCityPopulation() / totalPopulation;
        }
    }

    public List<PopulationRow> findAllCountries(Connection connection) throws SQLException {
        Objects.requireNonNull(connection, "connection must not be null");
        List<PopulationRow> rows = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(SQL);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                rows.add(new PopulationRow(result.getString("Country"),
                        result.getLong("TotalPopulation"), result.getLong("CityPopulation")));
            }
        }
        return rows;
    }
}
