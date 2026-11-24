package com.napier.sem.report.population;

import com.napier.sem.ReportDisplay;
import com.napier.sem.report.population.LanguagePopulationQuery.LanguageRow;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Issue #35: Displays worldwide language speaker estimates. */
public class LanguagePopulationReport {
    private static final String[] HEADERS = {"Language", "Speakers", "World Population (%)"};

    /** Older test fixtures omit countrylanguage or its Percentage column. */
    public static boolean isAvailable(Connection connection) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        String catalog = connection.getCatalog();
        String schema = connection.getSchema();
        try (ResultSet tables = metadata.getTables(catalog, schema, "%", null)) {
            while (tables.next()) {
                String name = tables.getString("TABLE_NAME");
                if (!"countrylanguage".equalsIgnoreCase(name)) continue;
                Set<String> columns = new HashSet<>();
                try (ResultSet fields = metadata.getColumns(tables.getString("TABLE_CAT"),
                        tables.getString("TABLE_SCHEM"), name, "%")) {
                    while (fields.next()) {
                        columns.add(fields.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
                    }
                }
                if (columns.containsAll(Set.of("countrycode", "language", "percentage"))) return true;
            }
        }
        return false;
    }

    public void display(Connection connection) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        for (LanguageRow row : new LanguagePopulationQuery().findLanguagePopulations(connection)) {
            rows.add(new String[]{row.language(), Long.toString(row.speakers()),
                    row.worldPercentage().toPlainString() + "%"});
        }
        ReportDisplay.printTable("Worldwide language population statistics", HEADERS, rows);
    }
}
