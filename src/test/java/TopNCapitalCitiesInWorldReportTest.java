import com.napier.sem.AutoDisplay;
import com.napier.sem.report.capital.AllCapitalCitiesQuery.CapitalRow;
import com.napier.sem.report.capital.TopNCapitalCitiesInWorldQuery;
import com.napier.sem.report.capital.TopNCapitalCitiesInWorldReport;
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

/** Independent H2 fixtures for Issue #23; a live MySQL server is not required. */
public class TopNCapitalCitiesInWorldReportTest {
    private Connection database() throws SQLException {
        Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:capital_top_world_" + UUID.randomUUID() + ";MODE=MySQL");
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE country (Code VARCHAR(3) PRIMARY KEY, Name VARCHAR(100),
                        Continent VARCHAR(50), Region VARCHAR(100), Population BIGINT, Capital INT)
                    """);
            statement.execute("""
                    CREATE TABLE city (ID INT PRIMARY KEY, Name VARCHAR(100), CountryCode VARCHAR(3),
                        District VARCHAR(100), Population BIGINT NOT NULL)
                    """);
            statement.executeUpdate("""
                    INSERT INTO country VALUES
                    ('AAA', 'Country A', 'Asia', 'Eastern Asia', 9000000, 1),
                    ('BBB', 'Country B', 'Europe', 'Western Europe', 7000000, 3),
                    ('CCC', 'Country C', 'Asia', 'Eastern Asia', 8000000, 5),
                    ('DDD', 'Country D', 'Asia', 'Eastern Asia', 6000000, NULL),
                    ('EEE', 'Country E', 'Asia', 'Eastern Asia', 5000000, 999),
                    ('FFF', 'Country F', 'Asia', 'Eastern Asia', 1000000, 8),
                    ('GGG', 'Country G', 'Asia', 'Southern Asia', 11000000, 9)
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
                    (8, 'Foxtrot', 'FFF', 'Central', 0),
                    (9, 'Golf', 'GGG', 'Central', 1500000)
                    """);
            return connection;
        } catch (SQLException e) {
            connection.close();
            throw e;
        }
    }

    private List<CapitalRow> capitals(Connection c, int n) throws SQLException {
        return new TopNCapitalCitiesInWorldQuery().findTopNCapitals(c, n);
    }

    private List<String> names(Connection c, int n) throws SQLException {
        return capitals(c, n).stream().map(CapitalRow::name).toList();
    }

    private String capture(Connection c, int n, boolean all) throws SQLException {
        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(output);
            if (all) AutoDisplay.displayAllReports(c);
            else new TopNCapitalCitiesInWorldReport().display(c, n);
        } finally {
            System.setOut(original);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    @Test
    void returnsRequestedTopThreeAcrossContinents() throws SQLException {
        try (Connection c = database()) {
            assertEquals(List.of("Golf", "Bravo", "Charlie"), names(c, 3));
        }
    }

    @Test
    void supportsTopOne() throws SQLException {
        try (Connection c = database()) {
            assertEquals(List.of("Golf"), names(c, 1));
        }
    }

    @Test
    void returnsAllAvailableWhenNExceedsCount() throws SQLException {
        try (Connection c = database()) {
            assertEquals(List.of("Golf", "Bravo", "Charlie", "Alpha", "Foxtrot"), names(c, 100));
        }
    }

    @Test
    void sortsByCityPopulationNotCountryPopulation() throws SQLException {
        try (Connection c = database()) {
            assertEquals(List.of(1500000L, 900000L, 400000L, 100000L, 0L),
                    capitals(c, 5).stream().map(CapitalRow::population).toList());
            assertEquals(List.of("Country G", "Country B", "Country C", "Country A", "Country F"),
                    capitals(c, 5).stream().map(CapitalRow::country).toList());
        }
    }

    @Test
    void excludesNonCapitalCitiesAndMissingCapitalReferences() throws SQLException {
        try (Connection c = database()) {
            assertFalse(capitals(c, 100).stream().anyMatch(r -> r.name().contains("Noncapital")
                    || r.country().equals("Country D") || r.country().equals("Country E")));
        }
    }

    @Test
    void breaksPopulationTiesByName() throws SQLException {
        try (Connection c = database(); Statement s = c.createStatement()) {
            s.executeUpdate("UPDATE city SET Population = 400000 WHERE ID = 1");
            assertEquals(List.of("Golf", "Bravo", "Alpha"), names(c, 3));
        }
    }

    @Test
    void handlesNoRecordedCapitals() throws SQLException {
        try (Connection c = database(); Statement s = c.createStatement()) {
            s.executeUpdate("UPDATE country SET Capital = NULL");
            assertTrue(capitals(c, 5).isEmpty());
            assertTrue(capture(c, 5, false).contains("No results found."));
        }
    }

    @Test
    void rejectsNonPositiveN() throws SQLException {
        try (Connection c = database()) {
            assertThrows(IllegalArgumentException.class, () -> capitals(c, 0));
            assertThrows(IllegalArgumentException.class, () -> capitals(c, -1));
        }
    }

    @Test
    void rejectsNullConnection() {
        assertThrows(NullPointerException.class, () -> capitals(null, 5));
    }

    @Test
    void propagatesClosedConnectionError() throws SQLException {
        Connection c = database();
        c.close();
        assertThrows(SQLException.class, () -> capitals(c, 5));
    }

    @Test
    void displaysRequiredColumnsAndOnlyRequestedRows() throws SQLException {
        try (Connection c = database()) {
            String output = capture(c, 2, false);
            assertTrue(output.contains("Top 2 capital cities in the world by population"));
            assertTrue(output.lines().anyMatch(line -> line.trim().matches("Name\\s+Country\\s+Population")));
            assertTrue(output.lines().anyMatch(line -> line.trim().matches("Golf\\s+Country G\\s+1500000")));
            assertTrue(output.lines().anyMatch(line -> line.trim().matches("Bravo\\s+Country B\\s+900000")));
            assertTrue(output.indexOf("Golf") < output.indexOf("Bravo"));
            assertFalse(output.contains("Charlie"));
            assertFalse(output.contains("District"));
        }
    }

    @Test
    void readsAndTrimsConfiguredN() {
        String previous = System.getProperty("capital.topN.world");
        try {
            System.setProperty("capital.topN.world", " 3 ");
            assertEquals(3, TopNCapitalCitiesInWorldReport.configuredTopN());
        } finally {
            if (previous == null) System.clearProperty("capital.topN.world");
            else System.setProperty("capital.topN.world", previous);
        }
    }

    @Test
    void rejectsInvalidConfiguredN() {
        String previous = System.getProperty("capital.topN.world");
        try {
            for (String invalid : List.of("0", "-2", "abc", "2.5", "2147483648")) {
                System.setProperty("capital.topN.world", invalid);
                assertThrows(IllegalArgumentException.class, TopNCapitalCitiesInWorldReport::configuredTopN);
            }
        } finally {
            if (previous == null) System.clearProperty("capital.topN.world");
            else System.setProperty("capital.topN.world", previous);
        }
    }

    @Test
    void autoDisplayPreservesPreviousReportsAndUsesConfiguredN() throws SQLException {
        String previous = System.getProperty("capital.topN.world");
        try (Connection c = database()) {
            System.setProperty("capital.topN.world", "2");
            String output = capture(c, 2, true);
            for (int issue : new int[]{2, 4, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23}) {
                assertTrue(output.contains("===== ISSUE #" + issue + " ====="), "Missing issue " + issue);
            }
            int start = output.indexOf("===== ISSUE #23 =====");
            assertTrue(start >= 0);
            int end = output.indexOf("\n===== ISSUE #", start + 1);
            String section = output.substring(start, end < 0 ? output.length() : end);
            assertTrue(section.contains("Top 2 capital cities in the world by population"));
            assertTrue(section.contains("Golf"));
            assertTrue(section.contains("Bravo"));
            assertFalse(section.contains("Charlie"));
            assertTrue(output.contains("AVAILABLE REPORTS COMPLETED"));
        } finally {
            if (previous == null) System.clearProperty("capital.topN.world");
            else System.setProperty("capital.topN.world", previous);
        }
    }
}
