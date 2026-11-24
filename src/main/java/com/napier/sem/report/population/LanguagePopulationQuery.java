package com.napier.sem.report.population;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Issue #35: Estimates speakers of the five required languages. */
public class LanguagePopulationQuery {
    private static final List<String> LANGUAGES =
            List.of("Chinese", "English", "Hindi", "Spanish", "Arabic");

    public record LanguageRow(String language, long speakers, BigDecimal worldPercentage) { }
    private record Estimate(String language, BigDecimal speakers) { }

    public List<LanguageRow> findLanguagePopulations(Connection connection) throws SQLException {
        Objects.requireNonNull(connection, "Connection must not be null");
        BigDecimal worldPopulation;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COALESCE(SUM(Population), 0) AS WorldPopulation FROM country");
             ResultSet result = statement.executeQuery()) {
            result.next();
            worldPopulation = result.getBigDecimal("WorldPopulation");
        }

        // Multiplication uses decimal arithmetic to avoid integer overflow and float artifacts.
        // Divide by 100 exactly after aggregation, without rounding individual countries.
        String sql = """
                SELECT LOWER(cl.Language) AS LanguageKey,
                       SUM(CAST(c.Population AS DECIMAL(20,0)) *
                           CAST(cl.Percentage AS DECIMAL(7,4))) AS WeightedPopulation
                FROM countrylanguage cl
                JOIN country c ON cl.CountryCode = c.Code
                WHERE LOWER(cl.Language) IN (?, ?, ?, ?, ?)
                GROUP BY LOWER(cl.Language)
                """;
        Map<String, BigDecimal> estimates = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < LANGUAGES.size(); i++) {
                statement.setString(i + 1, LANGUAGES.get(i).toLowerCase(Locale.ROOT));
            }
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    BigDecimal weighted = result.getBigDecimal("WeightedPopulation");
                    estimates.put(result.getString("LanguageKey"),
                            weighted == null ? BigDecimal.ZERO : weighted.movePointLeft(2));
                }
            }
        }
        List<Estimate> sorted = new ArrayList<>();
        for (String language : LANGUAGES) {
            sorted.add(new Estimate(language, estimates.getOrDefault(
                    language.toLowerCase(Locale.ROOT), BigDecimal.ZERO)));
        }
        sorted.sort(Comparator.comparing(Estimate::speakers).reversed()
                .thenComparing(Estimate::language));

        List<LanguageRow> rows = new ArrayList<>();
        for (Estimate estimate : sorted) {
            BigDecimal percentage = worldPopulation.signum() == 0
                    ? BigDecimal.ZERO.setScale(2)
                    : estimate.speakers().movePointRight(2)
                            .divide(worldPopulation, 2, RoundingMode.HALF_UP);
            rows.add(new LanguageRow(estimate.language(),
                    estimate.speakers().setScale(0, RoundingMode.HALF_UP).longValueExact(),
                    percentage));
        }
        return rows;
    }
}
