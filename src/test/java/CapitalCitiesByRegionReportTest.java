import com.napier.sem.AutoDisplay;
import com.napier.sem.report.capital.AllCapitalCitiesQuery.CapitalRow;
import com.napier.sem.report.capital.CapitalCitiesByRegionQuery;
import com.napier.sem.report.capital.CapitalCitiesByRegionReport;
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

/** Independent H2 fixtures for Issue #22; a live MySQL server is not required. */
public class CapitalCitiesByRegionReportTest {
    private Connection database() throws SQLException {
        Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:capital_region_" + UUID.randomUUID() + ";MODE=MySQL");
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

    private List<CapitalRow> capitals(Connection c, String region) throws SQLException {
        return new CapitalCitiesByRegionQuery().findCapitals(c, region);
    }

    private List<String> names(Connection c, String region) throws SQLException {
        return capitals(c, region).stream().map(CapitalRow::name).toList();
    }

    private String capture(Connection c, String region, boolean all) throws SQLException {
        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(output);
            if (all) AutoDisplay.displayAllReports(c);
            else new CapitalCitiesByRegionReport().display(c, region);
        } finally {
            System.setOut(original);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    @Test
    void returnsAllEasternAsianCapitalsAndNoOtherCities() throws SQLException {
        try (Connection c = database()) {
            assertEquals(List.of("Charlie", "Alpha", "Foxtrot"), names(c, "Eastern Asia"));
        }
    }

    @Test
    void supportsAnotherRegion() throws SQLException {
        try (Connection c = database()) {
            assertEquals(List.of("Bravo"), names(c, "Western Europe"));
        }
    }

    @Test
    void distinguishesRegionsWithinSameContinent() throws SQLException {
        try (Connection c = database()) {
            assertEquals(List.of("Golf"), names(c, "Southern Asia"));
            assertFalse(names(c, "Eastern Asia").contains("Golf"));
        }
    }

    @Test
    void sortsByCityPopulationNotCountryPopulation() throws SQLException {
        try (Connection c = database()) {
            assertEquals(List.of(400000L, 100000L, 0L),
                    capitals(c, "Eastern Asia").stream().map(CapitalRow::population).toList());
            assertEquals(List.of("Country C", "Country A", "Country F"),
                    capitals(c, "Eastern Asia").stream().map(CapitalRow::country).toList());
        }
    }

    @Test
    void excludesNonCapitalsAndMissingCapitalReferences() throws SQLException {
        try (Connection c = database()) {
            List<CapitalRow> rows = capitals(c, "Eastern Asia");
            assertFalse(rows.stream().anyMatch(r -> r.name().contains("Noncapital")
                    || r.country().equals("Country D") || r.country().equals("Country E")));
        }
    }

    @Test
    void trimsAndMatchesRegionIgnoringCase() throws SQLException {
        try (Connection c = database()) {
            assertEquals(names(c, "Eastern Asia"), names(c, "  eAsTeRn aSiA  "));
        }
    }

    @Test
    void handlesUnknownRegionAndEmptyReport() throws SQLException {
        try (Connection c = database()) {
            assertTrue(capitals(c, "Unknown region").isEmpty());
            assertTrue(capture(c, "Unknown region", false).contains("No results found."));
        }
    }

    @Test
    void treatsSqlLikeInputAsAValue() throws SQLException {
        try (Connection c = database()) {
            assertTrue(capitals(c, "Eastern Asia' OR '1'='1").isEmpty());
        }
    }

    @Test
    void resolvesPopulationTiesByName() throws SQLException {
        try (Connection c = database(); Statement statement = c.createStatement()) {
            statement.executeUpdate("UPDATE city SET Population = 400000 WHERE ID = 1");
            assertEquals(List.of("Alpha", "Charlie", "Foxtrot"), names(c, "Eastern Asia"));
        }
    }

    @Test
    void displaysExactlyTheRequiredColumnsAndMatchingRows() throws SQLException {
        try (Connection c = database()) {
            String output = capture(c, "Eastern Asia", false);
            assertTrue(output.contains("Capital cities in region: Eastern Asia"));
            assertTrue(output.lines().anyMatch(line -> line.trim().matches("Name\\s+Country\\s+Population")));
            assertTrue(output.lines().anyMatch(line -> line.trim().matches("Charlie\\s+Country C\\s+400000")));
            assertTrue(output.indexOf("Charlie") < output.indexOf("Alpha"));
            assertFalse(output.contains("District"));
            assertFalse(output.contains("Bravo"));
            assertFalse(output.contains("Golf"));
            assertFalse(output.contains("Noncapital A"));
        }
    }

    @Test
    void rejectsBlankAndNullRegion() throws SQLException {
        try (Connection c = database()) {
            assertThrows(IllegalArgumentException.class, () -> capitals(c, "  "));
            assertThrows(IllegalArgumentException.class, () -> capitals(c, null));
        }
    }

    @Test
    void rejectsNullConnectionAndPropagatesClosedConnectionError() throws SQLException {
        assertThrows(NullPointerException.class, () -> capitals(null, "Eastern Asia"));
        Connection c = database();
        c.close();
        assertThrows(SQLException.class, () -> capitals(c, "Eastern Asia"));
    }

    @Test
    void readsAndTrimsUserProvidedRegion() {
        String previous = System.getProperty("capital.region");
        try {
            System.setProperty("capital.region", " Western Europe ");
            assertEquals("Western Europe", CapitalCitiesByRegionReport.configuredRegion());
        } finally {
            if (previous == null) System.clearProperty("capital.region");
            else System.setProperty("capital.region", previous);
        }
    }

    @Test
    void autoDisplayPreservesEarlierReportsAndUsesSelectedRegion() throws SQLException {
        String previous = System.getProperty("capital.region");
        try (Connection c = database()) {
            System.setProperty("capital.region", "Western Europe");
            String output = capture(c, "Western Europe", true);
            for (int issue : new int[]{2, 4, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22}) {
                assertTrue(output.contains("===== ISSUE #" + issue + " ====="), "Missing issue " + issue);
            }
            int start = output.indexOf("===== ISSUE #22 =====");
            int end = output.indexOf("\n===== ISSUE #", start + 1);
            String section = output.substring(start, end < 0 ? output.length() : end);
            assertTrue(section.contains("Bravo"));
            assertFalse(section.contains("Charlie"));
            assertFalse(section.contains("Noncapital B"));
            assertTrue(output.contains("AVAILABLE REPORTS COMPLETED"));
        } finally {
            if (previous == null) System.clearProperty("capital.region");
            else System.setProperty("capital.region", previous);
        }
    }
}
