import com.napier.sem.AutoDisplay;
import com.napier.sem.report.capital.AllCapitalCitiesQuery;
import com.napier.sem.report.capital.AllCapitalCitiesQuery.CapitalRow;
import com.napier.sem.report.capital.AllCapitalCitiesReport;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Independent H2 fixtures for Issue #20; no live MySQL server required. */
public class AllCapitalCitiesReportTest {

    private Connection database() throws SQLException {
        Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:capital_" + UUID.randomUUID() + ";MODE=MySQL");
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE country (Code VARCHAR(3) PRIMARY KEY, Name VARCHAR(100),
                        Continent VARCHAR(50), Region VARCHAR(100), Population BIGINT, Capital INT)
                    """);
            statement.execute("""
                    CREATE TABLE city (ID INT PRIMARY KEY, Name VARCHAR(100), CountryCode VARCHAR(3),
                        District VARCHAR(100), Population BIGINT NOT NULL)
                    """);
            // Synthetic populations: country ranking and largest-city ranking differ from capitals.
            statement.executeUpdate("""
                    INSERT INTO country VALUES
                    ('AAA', 'Country A', 'Asia', 'Eastern Asia', 9000000, 1),
                    ('BBB', 'Country B', 'Europe', 'Western Europe', 7000000, 3),
                    ('CCC', 'Country C', 'Africa', 'Eastern Africa', 8000000, 5),
                    ('DDD', 'Country D', 'Asia', 'Eastern Asia', 6000000, NULL),
                    ('EEE', 'Country E', 'Asia', 'Eastern Asia', 5000000, 999),
                    ('FFF', 'Country F', 'Asia', 'Eastern Asia', 1000000, 8)
                    """);
            statement.executeUpdate("""
                    INSERT INTO city VALUES
                    (1, 'Alpha', 'AAA', 'Central', 100000),
                    (2, 'Noncapital A', 'AAA', 'Central', 5000000),
                    (3, 'Bravo', 'BBB', 'Central', 900000),
                    (4, 'Noncapital B', 'BBB', 'North', 1200000),
                    (5, 'Charlie', 'CCC', 'South', 400000),
                    (6, 'No capital city', 'DDD', 'South', 800000),
                    (7, 'Unlinked city', 'EEE', 'North', 600000),
                    (8, 'Foxtrot', 'FFF', 'Central', 0)
                    """);
            return connection;
        } catch (SQLException e) {
            connection.close();
            throw e;
        }
    }

    private List<CapitalRow> capitals(Connection connection) throws SQLException {
        return new AllCapitalCitiesQuery().findAllCapitals(connection);
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private String capture(Connection connection, boolean allReports) throws SQLException {
        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(output);
            if (allReports) AutoDisplay.displayAllReports(connection);
            else new AllCapitalCitiesReport().display(connection);
        } finally {
            System.setOut(original);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    @Test
    void returnsEveryRecordedCapitalAcrossContinents() throws SQLException {
        try (Connection c = database()) {
            assertEquals(4, capitals(c).size());
            assertEquals(List.of("Country B", "Country C", "Country A", "Country F"),
                    capitals(c).stream().map(CapitalRow::country).toList());
        }
    }

    @Test
    void ordersByCityPopulationNotCountryPopulation() throws SQLException {
        try (Connection c = database()) {
            assertEquals(List.of(900000L, 400000L, 100000L, 0L),
                    capitals(c).stream().map(CapitalRow::population).toList());
        }
    }

    @Test
    void excludesLargerNonCapitalCities() throws SQLException {
        try (Connection c = database()) {
            assertEquals(List.of("Bravo", "Charlie", "Alpha", "Foxtrot"),
                    capitals(c).stream().map(CapitalRow::name).toList());
        }
    }

    @Test
    void excludesNullAndUnmatchedCapitalReferences() throws SQLException {
        try (Connection c = database()) {
            assertFalse(capitals(c).stream().anyMatch(row ->
                    row.country().equals("Country D") || row.country().equals("Country E")));
        }
    }

    @Test
    void returnsEmptyListWhenThereAreNoCapitals() throws SQLException {
        try (Connection c = database()) {
            execute(c, "UPDATE country SET Capital = NULL");
            assertTrue(capitals(c).isEmpty());
            assertTrue(capture(c, false).contains("No results found."));
        }
    }

    @Test
    void ordersEqualPopulationsByName() throws SQLException {
        try (Connection c = database()) {
            execute(c, "UPDATE city SET Population = 900000 WHERE ID = 1");
            assertEquals(List.of("Alpha", "Bravo", "Charlie", "Foxtrot"),
                    capitals(c).stream().map(CapitalRow::name).toList());
        }
    }

    @Test
    void displaysOnlyRequiredColumnsAndCapitalRows() throws SQLException {
        try (Connection c = database()) {
            String output = capture(c, false);
            assertTrue(output.contains("All capital cities in the world by population"));
            assertTrue(output.lines().anyMatch(line -> line.trim().matches("Name\\s+Country\\s+Population")));
            assertTrue(output.lines().anyMatch(line -> line.trim().matches("Bravo\\s+Country B\\s+900000")));
            assertTrue(output.contains("Foxtrot"));
            assertTrue(output.indexOf("Bravo") < output.indexOf("Charlie"));
            assertFalse(output.contains("District"));
            assertFalse(output.contains("Noncapital A"));
        }
    }

    @Test
    void rejectsNullConnection() {
        assertThrows(NullPointerException.class, () -> capitals(null));
    }

    @Test
    void propagatesSqlErrorForClosedConnection() throws SQLException {
        Connection c = database();
        c.close();
        assertThrows(SQLException.class, () -> capitals(c));
    }

    @Test
    void autoDisplayIncludesCapitalReportAndEarlierCityReports() throws SQLException {
        try (Connection c = database()) {
            String output = capture(c, true);
            for (int issue : new int[]{2, 4, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20}) {
                assertTrue(output.contains("===== ISSUE #" + issue + " ====="), "Missing issue " + issue);
            }
            String capitalOutput = output.substring(output.indexOf("===== ISSUE #20 ====="));
            assertTrue(capitalOutput.contains("Bravo"));
            assertFalse(capitalOutput.contains("Noncapital A"));
            assertTrue(capitalOutput.contains("AVAILABLE REPORTS COMPLETED"));
        }
    }
}
