import com.napier.sem.AutoDisplay;
import com.napier.sem.report.capital.AllCapitalCitiesQuery.CapitalRow;
import com.napier.sem.report.capital.CapitalCitiesByContinentQuery;
import com.napier.sem.report.capital.CapitalCitiesByContinentReport;
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

/** Independent H2 fixtures for Issue #21; a live MySQL server is not required. */
public class CapitalCitiesByContinentReportTest {
    private Connection database() throws SQLException {
        Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:capital_continent_" + UUID.randomUUID() + ";MODE=MySQL");
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

    private List<CapitalRow> capitals(Connection c, String continent) throws SQLException {
        return new CapitalCitiesByContinentQuery().findCapitals(c, continent);
    }

    private List<String> names(Connection c, String continent) throws SQLException {
        return capitals(c, continent).stream().map(CapitalRow::name).toList();
    }

    private String capture(Connection c, String continent, boolean all) throws SQLException {
        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(output);
            if (all) AutoDisplay.displayAllReports(c);
            else new CapitalCitiesByContinentReport().display(c, continent);
        } finally {
            System.setOut(original);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    @Test
    void returnsAllAsianCapitalsAndNoOtherCities() throws SQLException {
        try (Connection c = database()) {
            assertEquals(List.of("Charlie", "Alpha", "Foxtrot"), names(c, "Asia"));
        }
    }

    @Test
    void supportsAnotherContinent() throws SQLException {
        try (Connection c = database()) {
            assertEquals(List.of("Bravo"), names(c, "Europe"));
        }
    }

    @Test
    void sortsByCityPopulationNotCountryPopulation() throws SQLException {
        try (Connection c = database()) {
            assertEquals(List.of(400000L, 100000L, 0L),
                    capitals(c, "Asia").stream().map(CapitalRow::population).toList());
            assertEquals(List.of("Country C", "Country A", "Country F"),
                    capitals(c, "Asia").stream().map(CapitalRow::country).toList());
        }
    }

    @Test
    void excludesNonCapitalsAndMissingCapitalReferences() throws SQLException {
        try (Connection c = database()) {
            List<CapitalRow> rows = capitals(c, "Asia");
            assertFalse(rows.stream().anyMatch(r -> r.name().contains("Noncapital")
                    || r.country().equals("Country D") || r.country().equals("Country E")));
        }
    }

    @Test
    void trimsAndMatchesContinentIgnoringCase() throws SQLException {
        try (Connection c = database()) {
            assertEquals(names(c, "Asia"), names(c, "  aSiA  "));
        }
    }

    @Test
    void handlesUnknownContinentAndEmptyReport() throws SQLException {
        try (Connection c = database()) {
            assertTrue(capitals(c, "Antarctica").isEmpty());
            assertTrue(capture(c, "Antarctica", false).contains("No results found."));
        }
    }

    @Test
    void treatsSqlLikeInputAsAValue() throws SQLException {
        try (Connection c = database()) {
            assertTrue(capitals(c, "Asia' OR '1'='1").isEmpty());
        }
    }

    @Test
    void resolvesPopulationTiesByName() throws SQLException {
        try (Connection c = database(); Statement statement = c.createStatement()) {
            statement.executeUpdate("UPDATE city SET Population = 400000 WHERE ID = 1");
            assertEquals(List.of("Alpha", "Charlie", "Foxtrot"), names(c, "Asia"));
        }
    }

    @Test
    void displaysExactlyTheRequiredColumnsAndMatchingRows() throws SQLException {
        try (Connection c = database()) {
            String output = capture(c, "Asia", false);
            assertTrue(output.contains("Capital cities in continent: Asia"));
            assertTrue(output.lines().anyMatch(line -> line.trim().matches("Name\\s+Country\\s+Population")));
            assertTrue(output.lines().anyMatch(line -> line.trim().matches("Charlie\\s+Country C\\s+400000")));
            assertTrue(output.indexOf("Charlie") < output.indexOf("Alpha"));
            assertFalse(output.contains("District"));
            assertFalse(output.contains("Bravo"));
            assertFalse(output.contains("Noncapital A"));
        }
    }

    @Test
    void rejectsBlankAndNullContinent() throws SQLException {
        try (Connection c = database()) {
            assertThrows(IllegalArgumentException.class, () -> capitals(c, "  "));
            assertThrows(IllegalArgumentException.class, () -> capitals(c, null));
        }
    }

    @Test
    void rejectsNullConnectionAndPropagatesClosedConnectionError() throws SQLException {
        assertThrows(NullPointerException.class, () -> capitals(null, "Asia"));
        Connection c = database();
        c.close();
        assertThrows(SQLException.class, () -> capitals(c, "Asia"));
    }

    @Test
    void readsAndTrimsUserProvidedContinent() {
        String previous = System.getProperty("capital.continent");
        try {
            System.setProperty("capital.continent", " Europe ");
            assertEquals("Europe", CapitalCitiesByContinentReport.configuredContinent());
        } finally {
            if (previous == null) System.clearProperty("capital.continent");
            else System.setProperty("capital.continent", previous);
        }
    }

    @Test
    void autoDisplayPreservesEarlierReportsAndUsesSelectedContinent() throws SQLException {
        String previous = System.getProperty("capital.continent");
        try (Connection c = database()) {
            System.setProperty("capital.continent", "Europe");
            String output = capture(c, "Europe", true);
            for (int issue : new int[]{2, 4, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21}) {
                assertTrue(output.contains("===== ISSUE #" + issue + " ====="), "Missing issue " + issue);
            }
            int start = output.indexOf("===== ISSUE #21 =====");
            int end = output.indexOf("\n===== ISSUE #", start + 1);
            String section = output.substring(start, end < 0 ? output.length() : end);
            assertTrue(section.contains("Bravo"));
            assertFalse(section.contains("Charlie"));
            assertFalse(section.contains("Noncapital B"));
            assertTrue(output.contains("AVAILABLE REPORTS COMPLETED"));
        } finally {
            if (previous == null) System.clearProperty("capital.continent");
            else System.setProperty("capital.continent", previous);
        }
    }
}
