package com.napier.sem.report.country;

import com.napier.sem.ReportDisplay;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Shared SQL/report logic. Other developers can reuse executeQuery for later report issues. */
public class CountryReport {

    private static final String[] HEADERS = {
            "Code", "Name", "Continent", "Region", "Population", "Capital"
    };

    private static final String BASE_QUERY = """
            SELECT c.Code, c.Name, c.Continent, c.Region, c.Population,
                   COALESCE(capital.Name, 'N/A') AS Capital
            FROM country c
            LEFT JOIN city capital ON c.Capital = capital.ID
            """;

    /** Issue #2: All countries, biggest population first. */
    public void displayAllCountries(Connection connection) throws SQLException {
        executeQuery(connection, "ALL COUNTRIES IN THE WORLD",
                BASE_QUERY + " ORDER BY c.Population DESC, c.Code ASC");
    }

    /** Issue #4: Countries in the supplied continent, biggest population first. */
    public void displayCountriesInContinent(Connection connection, String continent)
            throws SQLException {
        if (continent == null || continent.isBlank()) {
            throw new IllegalArgumentException("Continent must not be blank");
        }
        String selectedContinent = continent.trim();
        executeQuery(connection, "ALL COUNTRIES IN " + selectedContinent,
                BASE_QUERY + " WHERE c.Continent = ? ORDER BY c.Population DESC, c.Code ASC",
                selectedContinent);
    }

    /** Reusable query execution; user-supplied values must be passed as parameters. */
    public static void executeQuery(Connection connection, String title, String sql,
                                    Object... parameters) throws SQLException {
        Objects.requireNonNull(connection, "Connection must not be null");

        List<String[]> rows = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    rows.add(new String[]{
                            result.getString("Code"),
                            result.getString("Name"),
                            result.getString("Continent"),
                            result.getString("Region"),
                            String.format(Locale.US, "%,d", result.getLong("Population")),
                            result.getString("Capital")
                    });
                }
            }
        }
        ReportDisplay.printTable(title, HEADERS, rows);
    }
}
