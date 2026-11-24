import com.napier.sem.AutoDisplay;
import com.napier.sem.report.capital.AllCapitalCitiesQuery.CapitalRow;
import com.napier.sem.report.capital.TopNCapitalCitiesInContinentQuery;
import com.napier.sem.report.capital.TopNCapitalCitiesInContinentReport;
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

/** Independent H2 fixtures for Issue #24; a live MySQL server is not required. */
public class TopNCapitalCitiesInContinentReportTest {

    private Connection database() throws SQLException {
        Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:capital_top_continent_" + UUID.randomUUID() + ";MODE=MySQL");

        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE country (
                        Code VARCHAR(3) PRIMARY KEY,
                        Name VARCHAR(100),
                        Continent VARCHAR(50),
                        Region VARCHAR(100),
                        Population BIGINT,
                        Capital INT
                    )
                    """);

            statement.execute("""
                    CREATE TABLE city (
                        ID INT PRIMARY KEY,
                        Name VARCHAR(100),
                        CountryCode VARCHAR(3),
                        District VARCHAR(100),
                        Population BIGINT NOT NULL
                    )
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
        } catch (SQLException error) {
            connection.close();
            throw error;
        }
    }

    private List<CapitalRow> capitals(
            Connection connection, String continent, int n) throws SQLException {
        return new TopNCapitalCitiesInContinentQuery()
                .findTopNCapitals(connection, continent, n);
    }

    private List<String> names(
            Connection connection, String continent, int n) throws SQLException {
        return capitals(connection, continent, n)
                .stream()
                .map(CapitalRow::name)
                .toList();
    }

    private String capture(
            Connection connection, String continent, int n) throws SQLException {

        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(output);
            new TopNCapitalCitiesInContinentReport().display(connection, continent, n);
        } finally {
            System.setOut(original);
        }

        return bytes.toString(StandardCharsets.UTF_8);
    }

    private String captureAllReports(Connection connection) throws SQLException {
        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(output);
            AutoDisplay.displayAllReports(connection);
        } finally {
            System.setOut(original);
        }

        return bytes.toString(StandardCharsets.UTF_8);
    }

    @Test
    void returnsRequestedTopThreeOnlyFromSelectedContinent() throws SQLException {
        try (Connection connection = database()) {
            assertEquals(
                    List.of("Golf", "Charlie", "Alpha"),
                    names(connection, "Asia", 3));
        }
    }

    @Test
    void supportsTopOne() throws SQLException {
        try (Connection connection = database()) {
            assertEquals(List.of("Golf"), names(connection, "Asia", 1));
        }
    }

    @Test
    void returnsAllAvailableMatchingCapitalsWhenNExceedsCount() throws SQLException {
        try (Connection connection = database()) {
            assertEquals(
                    List.of("Golf", "Charlie", "Alpha", "Foxtrot"),
                    names(connection, "Asia", 100));
        }
    }

    @Test
    void filtersAnotherContinentCorrectly() throws SQLException {
        try (Connection connection = database()) {
            assertEquals(List.of("Bravo"), names(connection, "Europe", 5));
        }
    }

    @Test
    void sortsByCapitalCityPopulationNotCountryPopulation() throws SQLException {
        try (Connection connection = database()) {
            assertEquals(
                    List.of(1500000L, 400000L, 100000L),
                    capitals(connection, "Asia", 3).stream()
                            .map(CapitalRow::population)
                            .toList());

            assertEquals(
                    List.of("Country G", "Country C", "Country A"),
                    capitals(connection, "Asia", 3).stream()
                            .map(CapitalRow::country)
                            .toList());
        }
    }

    @Test
    void excludesNonCapitalCitiesAndMissingOrInvalidCapitalReferences() throws SQLException {
        try (Connection connection = database()) {
            List<CapitalRow> rows = capitals(connection, "Asia", 100);

            assertFalse(rows.stream().anyMatch(row ->
                    row.name().contains("Noncapital")
                            || row.name().equals("No capital city")
                            || row.name().equals("Unlinked city")));
        }
    }

    @Test
    void trimsContinentAndMatchesIgnoringCase() throws SQLException {
        try (Connection connection = database()) {
            assertEquals(
                    names(connection, "Asia", 10),
                    names(connection, "  aSiA  ", 10));
        }
    }

    @Test
    void resolvesPopulationTiesByName() throws SQLException {
        try (Connection connection = database();
             Statement statement = connection.createStatement()) {

            statement.executeUpdate(
                    "UPDATE city SET Population = 400000 WHERE ID = 1");

            assertEquals(
                    List.of("Golf", "Alpha", "Charlie", "Foxtrot"),
                    names(connection, "Asia", 10));
        }
    }

    @Test
    void handlesUnknownContinentAndEmptyReport() throws SQLException {
        try (Connection connection = database()) {
            assertTrue(capitals(connection, "Antarctica", 5).isEmpty());
            assertTrue(capture(connection, "Antarctica", 5)
                    .contains("No results found."));
        }
    }

    @Test
    void treatsSqlLikeContinentInputAsAValue() throws SQLException {
        try (Connection connection = database()) {
            assertTrue(capitals(connection, "Asia' OR '1'='1", 10).isEmpty());
        }
    }

    @Test
    void rejectsBlankOrNullContinent() throws SQLException {
        try (Connection connection = database()) {
            assertThrows(IllegalArgumentException.class,
                    () -> capitals(connection, "  ", 5));
            assertThrows(IllegalArgumentException.class,
                    () -> capitals(connection, null, 5));
        }
    }

    @Test
    void rejectsNonPositiveN() throws SQLException {
        try (Connection connection = database()) {
            assertThrows(IllegalArgumentException.class,
                    () -> capitals(connection, "Asia", 0));
            assertThrows(IllegalArgumentException.class,
                    () -> capitals(connection, "Asia", -1));
        }
    }

    @Test
    void rejectsNullConnection() {
        assertThrows(NullPointerException.class,
                () -> capitals(null, "Asia", 5));
    }

    @Test
    void propagatesClosedConnectionError() throws SQLException {
        Connection connection = database();
        connection.close();

        assertThrows(SQLException.class,
                () -> capitals(connection, "Asia", 5));
    }

    @Test
    void displaysRequiredColumnsAndOnlyRequestedRows() throws SQLException {
        try (Connection connection = database()) {
            String output = capture(connection, "Asia", 2);

            assertTrue(output.contains(
                    "Top 2 capital cities in continent: Asia"));
            assertTrue(output.lines().anyMatch(line ->
                    line.trim().matches("Name\\s+Country\\s+Population")));
            assertTrue(output.lines().anyMatch(line ->
                    line.trim().matches("Golf\\s+Country G\\s+1500000")));
            assertTrue(output.lines().anyMatch(line ->
                    line.trim().matches("Charlie\\s+Country C\\s+400000")));
            assertTrue(output.indexOf("Golf") < output.indexOf("Charlie"));
            assertFalse(output.contains("Alpha"));
            assertFalse(output.contains("District"));
        }
    }

    @Test
    void readsAndTrimsConfiguredTopN() {
        String previous = System.getProperty("capital.topN.continent");

        try {
            System.setProperty("capital.topN.continent", " 3 ");
            assertEquals(3, TopNCapitalCitiesInContinentReport.configuredTopN());
        } finally {
            if (previous == null) {
                System.clearProperty("capital.topN.continent");
            } else {
                System.setProperty("capital.topN.continent", previous);
            }
        }
    }

    @Test
    void rejectsInvalidConfiguredTopN() {
        String previous = System.getProperty("capital.topN.continent");

        try {
            for (String invalid : List.of("0", "-2", "abc", "2.5", "2147483648")) {
                System.setProperty("capital.topN.continent", invalid);
                assertThrows(
                        IllegalArgumentException.class,
                        TopNCapitalCitiesInContinentReport::configuredTopN);
            }
        } finally {
            if (previous == null) {
                System.clearProperty("capital.topN.continent");
            } else {
                System.setProperty("capital.topN.continent", previous);
            }
        }
    }

    @Test
    void autoDisplayIncludesIssue24AndUsesConfiguredContinentAndN() throws SQLException {
        String previousContinent = System.getProperty("capital.continent");
        String previousTopN = System.getProperty("capital.topN.continent");

        try (Connection connection = database()) {
            System.setProperty("capital.continent", "Europe");
            System.setProperty("capital.topN.continent", "1");

            String output = captureAllReports(connection);

            assertTrue(output.contains("===== ISSUE #24 ====="));

            int start = output.indexOf("===== ISSUE #24 =====");
            int end = output.indexOf("\n===== ISSUE #", start + 1);
            String section = output.substring(
                    start, end < 0 ? output.length() : end);

            assertTrue(section.contains(
                    "Top 1 capital cities in continent: Europe"));
            assertTrue(section.contains("Bravo"));
            assertFalse(section.contains("Golf"));
            assertFalse(section.contains("Charlie"));
        } finally {
            if (previousContinent == null) {
                System.clearProperty("capital.continent");
            } else {
                System.setProperty("capital.continent", previousContinent);
            }

            if (previousTopN == null) {
                System.clearProperty("capital.topN.continent");
            } else {
                System.setProperty("capital.topN.continent", previousTopN);
            }
        }
    }

}
