package com.napier.sem.report.population;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Issue #27: Population totals and recorded city population for every region. */
public class RegionPopulationQuery {
    // Aggregate cities by country first: a country must contribute its total only once.
    private static final String SQL = """
            SELECT co.Region AS Region,
                   SUM(co.Population) AS TotalPopulation,
                   COALESCE(SUM(cp.CityPopulation), 0) AS CityPopulation
            FROM country co
            LEFT JOIN (
                SELECT CountryCode, SUM(Population) AS CityPopulation
                FROM city
                GROUP BY CountryCode
            ) cp ON cp.CountryCode = co.Code
            GROUP BY co.Region
            ORDER BY co.Region ASC
            """;

    /** Non-city population is the remainder outside cities recorded in the dataset. */
    public record PopulationRow(String region, long totalPopulation, long cityPopulation) {
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

    public List<PopulationRow> findAllRegions(Connection connection) throws SQLException {
        Objects.requireNonNull(connection, "connection must not be null");
        List<PopulationRow> rows = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(SQL);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                rows.add(new PopulationRow(result.getString("Region"),
                        result.getLong("TotalPopulation"), result.getLong("CityPopulation")));
            }
        }
        return rows;
    }
}
